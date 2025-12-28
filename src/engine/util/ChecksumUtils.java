package engine.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ChecksumUtils {
	public static final String defaultChecksum = "00000000"; // default checksum for non-existent/empty files

	/**
	 * Calculates CRC32 checksum of a file
	 * @param filePath Path to the file
	 * @return CRC32 checksum as 8-character hex string
	 */
	public static String calculateFileChecksum(Path filePath) {
		try {
			if (!Files.exists(filePath)) {
				return defaultChecksum;
			}

			CRC32 crc32 = new CRC32();
			try (InputStream is = Files.newInputStream(filePath)) {
				byte[] buffer = new byte[8192]; // 8KB buffer
				int bytesRead;

				while ((bytesRead = is.read(buffer)) != -1) {
					crc32.update(buffer, 0, bytesRead);
				}
			}

			return String.format("%08x", crc32.getValue());
		} catch (IOException e) {
			System.err.println("Failed to calculate checksum for file " + filePath + ": " + e.getMessage());
			return defaultChecksum;
		}
	}

	/**
	 * Calculates CRC32 checksum of a list of strings
	 * This is mainly used to calculate checksum of a string-based file (ex: metadata file)
	 * @Note this by default adds new line character
	 * @param lines List of strings to calculate checksum for
	 * @return CRC32 checksum as 8-character hex string
	 */
	public static String calculateListOfStringsChecksum(List<String> lines) {
		CRC32 crc32 = new CRC32();
		for (String line : lines) {
			crc32.update(line.getBytes());
			crc32.update('\n');
		}
		return String.format("%08x", crc32.getValue());
	}

	/**
	 * Calculates CRC32 checksum of a single string
	 * @param content String to calculate checksum for
	 * @return CRC32 checksum as 8-character hex string
	 */
	public static String calculateStringChecksum(String content) {
		CRC32 crc32 = new CRC32();
		crc32.update(content.getBytes());
		return String.format("%08x", crc32.getValue());
	}

	/**
	 * Verifies if the calculated checksum matches the expected checksum
	 * Its just a proxy - no fancy algo hh
	 * @param expected Expected checksum
	 * @param actual Actual checksum to verify
	 * @return true if checksums match, false otherwise
	 */
	public static boolean verifyChecksum(String expected, String actual) {
		return expected.equals(actual);
	}

	/**
	 * Verifies a file against a stored checksum
	 * @param filePath Path to the file to verify
	 * @param expectedChecksum Expected checksum to verify against
	 * @return true if the file's calculated checksum matches the expected checksum, false otherwise
	 */
	public static boolean verifyFileChecksum(Path filePath, String expectedChecksum) {
		String actualChecksum = calculateFileChecksum(filePath);
		return verifyChecksum(expectedChecksum, actualChecksum);
	}
}
