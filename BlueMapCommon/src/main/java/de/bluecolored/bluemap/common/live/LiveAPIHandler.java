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
package de.bluecolored.bluemap.common.live;

import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.common.plugin.PluginConfig;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.BlueMap;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import java.io.StringWriter;

public class LiveAPIHandler extends PathHandler {

	private final ServerInterface server;
	private final PluginConfig config;
	
	public LiveAPIHandler(ServerInterface server, PluginConfig config, HttpHandler next) {
		super(next);

		this.server = server;
		this.config = config;

		this.addPrefixPath("/live", this::handleLivePingRequest);
		this.addExactPath("/live/players", this::handlePlayersRequest);
	}

	public void handleLivePingRequest(HttpServerExchange exchange) {
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseHeaders().add(Headers.CACHE_CONTROL, "no-cache");

		exchange.getResponseSender().send("{\"status\":\"OK\"}");
		exchange.endExchange();
	}

	public void handlePlayersRequest(HttpServerExchange exchange) throws Exception {
		if (!exchange.getRequestMethod().equals(Methods.GET)) {
			String server = exchange.getResponseHeaders().getFirst(Headers.SERVER);

			exchange.setStatusCode(400);
			exchange.getResponseSender().send(
					"400 - Bad Request\n" +
					server
			);
			exchange.endExchange();
			return;
		}

		try (
				StringWriter data = new StringWriter();
				JsonWriter json = new JsonWriter(data);
		) {
			json.beginObject();
			json.name("players").beginArray();
			for (Player player : server.getOnlinePlayers()) {
				if (!player.isOnline()) continue;

				if (config.isHideInvisible() && player.isInvisible()) continue;
				if (config.isHideSneaking() && player.isSneaking()) continue;
				if (config.getHiddenGameModes().contains(player.getGamemode().getId())) continue;

				json.beginObject();
				json.name("uuid").value(player.getUuid().toString());
				json.name("name").value(player.getName().toPlainString());
				json.name("world").value(player.getWorld().toString());
				json.name("position").beginObject();
				json.name("x").value(player.getPosition().getX());
				json.name("y").value(player.getPosition().getY());
				json.name("z").value(player.getPosition().getZ());
				json.endObject();
				json.endObject();
			}

			json.endArray();
			json.endObject();

			exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
			exchange.getResponseHeaders().add(Headers.CACHE_CONTROL, "no-cache");

			exchange.getResponseSender().send(data.toString());
		}
	}
	
}
