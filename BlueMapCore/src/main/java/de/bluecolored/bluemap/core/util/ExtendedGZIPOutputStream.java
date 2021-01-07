package de.bluecolored.bluemap.core.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A normal {@link GZIPOutputStream} but you can change the deflaters compression-level
 */
public class ExtendedGZIPOutputStream extends GZIPOutputStream {

	/**
	 * Creates a new output stream with a default buffer size.
	 *
	 * <p>The new output stream instance is created as if by invoking
	 * the 2-argument constructor GZIPOutputStream(out, false).
	 *
	 * @param out the output stream
	 * @param compressionLevel the GZIP compression level (0-9)
	 * @exception IOException If an I/O error has occurred.
	 */
	public ExtendedGZIPOutputStream(OutputStream out, int compressionLevel) throws IOException {
		super(out);
		this.def.setLevel(compressionLevel);
	}

}
