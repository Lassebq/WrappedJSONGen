package lbq.jsongen;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {
	public static byte[] readAllBytes(InputStream inputStream) throws IOException {
		final int bufLen = 4 * 0x400; // 4KB
		byte[] buf = new byte[bufLen];
		int readLen;
		IOException exception = null;

		try {
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
					outputStream.write(buf, 0, readLen);

				return outputStream.toByteArray();
			}
		} catch (IOException e) {
			exception = e;
			throw e;
		} finally {
			if (exception == null) inputStream.close();
			else try {
				inputStream.close();
			} catch (IOException e) {
				exception.addSuppressed(e);
			}
		}
	}

	public static String getSHA1(InputStream is) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			BufferedInputStream bs = new BufferedInputStream(is);
			byte[] buffer = new byte[1024];
			int bytesRead;

			while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
				md.update(buffer, 0, bytesRead);
			}
			byte[] digest = md.digest();

			StringBuilder sb = new StringBuilder();
			for (byte bite : digest) {
				sb.append(Integer.toString((bite & 255) + 256, 16).substring(1).toLowerCase());
			}
			bs.close();
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}
}
