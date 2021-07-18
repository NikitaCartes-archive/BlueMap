/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.ETag;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFileHandler implements HttpHandler {
    private static final String SERVER = "BlueMap / " + BlueMap.VERSION;

    private final ResourceHandler resourceHandler;

    public StaticFileHandler(Path webRoot, HttpHandler next) {
        ResourceManager resourceManager = PathResourceManager
                .builder()
                .setBase(webRoot)
                .setFollowLinks(false)
                .setETagFunction(this::calculateEtag)
                .build();

        PreCompressedResourceSupplier resourceSupplier = new PreCompressedResourceSupplier(resourceManager);
        resourceSupplier.addEncoding("gzip", ".gz");
        resourceSupplier.addEncoding("br", ".br");

        this.resourceHandler = new ResourceHandler(resourceSupplier, next);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.SERVER, SERVER);

        if (exchange.getRequestMethod().equals(Methods.GET) || exchange.getRequestMethod().equals(Methods.HEAD)) {
            try {
                this.resourceHandler.handleRequest(exchange);
            } catch (Exception ex) {
                exchange.setStatusCode(500);
                exchange.getResponseSender().send(
                        "500 - Internal Server Error\n" +
                        SERVER
                );
                exchange.endExchange();
            }

            return;
        }

        exchange.setStatusCode(400);
        exchange.getResponseSender().send(
                "400 - Bad Request\n" +
                SERVER
        );
        exchange.endExchange();
    }

    public ETag calculateEtag(Path path) {
        try {
            long size = Files.size(path);
            long lastModified = Files.getLastModifiedTime(path).toMillis();

            String eTag = Long.toHexString(size) + Integer.toHexString(path.hashCode()) + Long.toHexString(lastModified);
            return new ETag(false, eTag);
        } catch (IOException e) {
            Logger.global.noFloodError("etag-error", "Failed to calculate E-Tag for file: " + path, e);
            return null;
        }
    }

}
