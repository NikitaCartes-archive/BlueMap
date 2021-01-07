package de.bluecolored.bluemap.core.config;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.enc.Encoder;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;

public enum CompressionConfig {
    PLAIN("PLAIN", "json"),
    GZIP("GZIP", "json.gz"),
    BROTLI("BROTLI", "json.br");

    private final String compressionType;
    private final String fileExtension;
    private final int compressionLevel;

    CompressionConfig(String compressionType, int compressionLevel) {
        this.compressionLevel = compressionLevel;
        if (compressionType.equals("true") || compressionType.equals("GZIP")) {
            this.compressionType = "GZIP";
            this.fileExtension = "json.gz";
        }
        else if (compressionType.equals("false") || compressionType.equals("PLAIN")) {
            this.compressionType = "PLAIN";
            this.fileExtension = "json";
        }
        else if (compressionType.equals("BROTLI")) {
            try {
                BrotliLoader.isBrotliAvailable();
                this.compressionType = compressionType;
                this.fileExtension = "json.br";
                if (compressionLevel < 1 || compressionLevel > 11) {
                    throw new IOException("Invalid configuration: value of compressionLevel isn't in [1,11]");
                }
            } catch (Throwable UnsatisfiedLinkError) {
                this.compressionType = "GZIP";
                this.fileExtension = "json.gz";
                // ToDo: Print to log about fallback to gzip
            }
        }
        else throw new IOException("Invalid configuration: value of useCompression is not understandable");
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public OutputStream getOutputStream(OutputStream os) {
        switch(compressionType) {
            case PLAIN:
                return os;
            case GZIP:
                return new GZIPOutputStream(os);
            case BROTLI:
                Encoder.Parameters params = new Encoder.Parameters().setQuality(compressionLevel);
                return new BrotliOutputStream(os, params);
        }
    }

    public InputStream getInputStream(InputStream fis) {
        switch (compressionType) {
            case PLAIN:
                return fis;
            case GZIP:
                return new GZIPInputStream(fis);
            case BROTLI:
                return new BrotliInputStream(fis);
        }
    }
}
