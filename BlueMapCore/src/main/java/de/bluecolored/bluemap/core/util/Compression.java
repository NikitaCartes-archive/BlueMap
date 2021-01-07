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
