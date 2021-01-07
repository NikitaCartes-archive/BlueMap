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
package de.bluecolored.bluemap.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Compression {

    private final CompressionType compressionType;
    private final int compressionLevel;

    public Compression(CompressionType compressionType) {
        this(compressionType, compressionType.getDefaultCompressionLevel());
    }

    public Compression(CompressionType compressionType, int compressionLevel) {
        this.compressionType = compressionType;
        this.compressionLevel = compressionLevel;
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public OutputStream createOutputStream(OutputStream out) throws IOException {
        return compressionType.createOutputStream(out, compressionLevel);
    }

    public InputStream createInputStream(InputStream in) throws IOException {
        return compressionType.createInputStream(in);
    }

}
