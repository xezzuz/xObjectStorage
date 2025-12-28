package engine.util;

import engine.segment.SegmentMeta;
import engine.segment.SegmentState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static logging.AppLogger.log;

public class SegmentIntegrityChecker {

	/**
	 * Verifies the integrity of a single segment by checking its existence, checksum and size
	 * @param segmentMeta The segment metadata to verify
	 * @return true if the segment is valid, false if corrupted
	 */
	public static boolean verifySegmentIntegrity(SegmentMeta segmentMeta) {
		try {
			Path segmentPath = segmentMeta.getPath();

			// check if the segment file exists
			if (!Files.exists(segmentPath)) {
				log.warning("Segment file does not exist: " + segmentPath);
				return false;
			}

			// check if segment has default checksum
			if (segmentMeta.getChecksum() == null || segmentMeta.getChecksum().equals(ChecksumUtils.defaultChecksum)) {
				log.warning("Segment " + segmentMeta.getId() + " has default value checksum, " +
				"by design this should never happen, unless if it was caused by a crash");
			}

			// check the stored checksum against the actual file
			String expectedChecksum = segmentMeta.getChecksum();
			String calculatedChecksum = ChecksumUtils.calculateFileChecksum(segmentPath);
			if (ChecksumUtils.verifyChecksum(expectedChecksum, calculatedChecksum)) {
				log.warning("CHECKSUM MISMATCH for segment " + segmentMeta.getId() +
					". Expected: " + expectedChecksum +
					", Calculated: " + calculatedChecksum);
				return false;
			}

			// verify that the stored size matches the actual file size
			long expectedSize = segmentMeta.getSize();
			long actualFileSize = Files.size(segmentPath);
			if (actualFileSize != expectedSize) {
				log.warning("SIZE MISMATCH for segment " + segmentMeta.getId() +
					". Expected: " + expectedSize + "B" +
					", Actual: " + actualFileSize + "B" +
					", Difference: " + Math.abs(actualFileSize) + "B");
				return false;
			}

			return true;
		} catch (IOException e) {
			log.severe("Error checking integrity for segment " + segmentMeta.getId() + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * Verifies the integrity of a list of segments
	 * @param segments List of segments to verify
	 * @return List of segments that are valid
	 */
	public static List<SegmentMeta> verifyListOfSegmentsIntegrity(List<SegmentMeta> segments) {
		List<SegmentMeta> onlyValidSegments = new ArrayList<>();

		for (SegmentMeta segment : segments) {
			if (verifySegmentIntegrity(segment))
				onlyValidSegments.add(segment);
		}

		return onlyValidSegments;
	}

	/**
	 * Performs a comprehensive integrity check on a segment
	 * @param segmentMeta The segment to check
	 * @return SegmentIntegrityStatus object with detailed results
	 */
	public static SegmentIntegrityStatus performComprehensiveCheck(SegmentMeta segmentMeta) {
		SegmentIntegrityStatus status = new SegmentIntegrityStatus(segmentMeta.getId());

		try {
			Path segmentPath = segmentMeta.getPath();

			// check file existence
			status.fileExists = Files.exists(segmentPath);
			if (!status.fileExists) {
				status.valid = false;
				status.errorMessages.add("Segment file does not exist: " + segmentPath);
				return status;
			}

			// check file size
			long actualSize = Files.size(segmentPath);
			status.sizeMatches = (actualSize == segmentMeta.getSize());
			if (!status.sizeMatches) {
				status.valid = false;
				status.errorMessages.add("Size mismatch: expected " + segmentMeta.getSize() +
					", actual " + actualSize + ", diff " + Math.abs(actualSize - segmentMeta.getSize()));
			}

			// check checksum if available
			if (segmentMeta.getChecksum() != null && !segmentMeta.getChecksum().equals(ChecksumUtils.defaultChecksum)) {
				status.checksumValid = ChecksumUtils.verifyFileChecksum(segmentPath, segmentMeta.getChecksum());
				if (!status.checksumValid) {
					status.valid = false;
					status.errorMessages.add("Checksum mismatch for segment " + segmentMeta.getId());
				}
			} else {
				// if no checksum stored, calculate and store one - this should be removed later
				status.checksumValid = true;
			}

		} catch (IOException e) {
			status.valid = false;
			status.errorMessages.add("IO error during integrity check: " + e.getMessage());
		}

		return status;
	}

	/**
	 * Status class for comprehensive integrity checks
	 */
	public static class SegmentIntegrityStatus {
		public final int segmentId;
		public boolean valid = true;
		public boolean fileExists = false;
		public boolean sizeMatches = false;
		public boolean checksumValid = false;
		public List<String> errorMessages = new ArrayList<>();

		public SegmentIntegrityStatus(int segmentId) {
			this.segmentId = segmentId;
		}

		public boolean isHealthy() {
			return valid && fileExists && sizeMatches && checksumValid;
		}

		@Override
		public String toString() {
			return String.format("SegmentIntegrityStatus{id=%d, valid=%b, fileExists=%b, sizeMatches=%b, checksumValid=%b, errors=%d}",
				segmentId, valid, fileExists, sizeMatches, checksumValid, errorMessages.size());
		}
	}
}
