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

import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import com.nixxcode.jvmbrotli.enc.Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;

public enum CompressionType {

	PLAIN("plain", "", 0){
		@Override
		public InputStream createInputStream(InputStream in) {
			return in;
		}

		@Override
		public OutputStream createOutputStream(OutputStream out, int compressionLevel) {
			return out;
		}
	},
	GZIP("gzip", ".gz", Deflater.DEFAULT_COMPRESSION){
		@Override
		public InputStream createInputStream(InputStream in) throws IOException {
			return new GZIPInputStream(in);
		}

		@Override
		public OutputStream createOutputStream(OutputStream out, int compressionLevel) throws IOException {
			if (compressionLevel != Deflater.DEFAULT_COMPRESSION) {
				if (compressionLevel < 0) compressionLevel = 0;
				if (compressionLevel > 9) compressionLevel = 9;
			}
			return new ExtendedGZIPOutputStream(out, compressionLevel);
		}
	},
	BROTLI("brotli", ".br", -1){
		@Override
		public InputStream createInputStream(InputStream in) throws IOException {
			return new BrotliInputStream(in);
		}

		@Override
		public OutputStream createOutputStream(OutputStream out, int compressionLevel) throws IOException {
			if (compressionLevel != -1) {
				if (compressionLevel < 0) compressionLevel = 0;
				if (compressionLevel > 11) compressionLevel = 11;
			}
			Encoder.Parameters params = new Encoder.Parameters().setQuality(compressionLevel);
			return new BrotliOutputStream(out, params);
		}
	};

	private final String id;
	private final String fileExtension;
	private final int defaultCompressionLevel;

	CompressionType(String id, String fileExtension, int defaultCompressionLevel){
		this.id = id;
		this.fileExtension = fileExtension;
		this.defaultCompressionLevel = defaultCompressionLevel;
	}

	public String getId(){
		return id;
	}

	/**
	 * Returns the preferred file-extension for this compression-type, with the leading '<code>.</code>'.<br>
	 * Might return an empty string if no extension is needed.
	 *
	 * @return The preferred file-extension
	 */
	public String getFileExtension() {
		return fileExtension;
	}

	public int getDefaultCompressionLevel() {
		return defaultCompressionLevel;
	}

	/**
	 * Creates an InputStream that decompresses this compression type.
	 * @param in the {@link InputStream} to be decompressed.
	 * @return the new decompressed {@link InputStream}.
	 * @throws IOException If an I/O error has occurred.
	 */
	public abstract InputStream createInputStream(InputStream in) throws IOException;

	/**
	 * Creates an OutputStream that compresses in this compression type.
	 * @param out the {@link OutputStream} to be compressed.
	 * @param compressionLevel the level of compression, if applicable. If the compression-level is not in the legal bounds of this compression-type the next closest one will be chosen.
	 * @return the new compressed {@link OutputStream}.
	 * @throws IOException If an I/O error has occurred.
	 */
	public abstract OutputStream createOutputStream(OutputStream out, int compressionLevel) throws IOException;

	/**
	 * Creates an OutputStream that compresses in this compression type with the default compressionLevel.
	 * @param out the {@link OutputStream} to be compressed.
	 * @return the new compressed {@link OutputStream}.
	 * @throws IOException If an I/O error has occurred.
	 */
	public OutputStream createOutputStream(OutputStream out) throws IOException {
		return createOutputStream(out, getDefaultCompressionLevel());
	}

	public static CompressionType forId(String id){
		for (CompressionType compressionType : values()){
			if (compressionType.id.equals(id)) return compressionType;
		}

		throw new NoSuchElementException("There is CompressionType with the id '" + id + "'!");
	}

}
