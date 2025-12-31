package engine.util;

import engine.segment.Segment;
import engine.segment.SegmentMeta;
import engine.segment.SegmentState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.Checksum;

import static logging.AppLogger.log;

public class SegmentIntegrityChecker {

	// /**
	//  * Filters unhealthy segments from a list of segments
	//  * @param segmentsMeta List of segments to check
	//  * @return List<SegmentMeta> object containing healthy segments
	//  */
	// public static List<SegmentMeta> filterUnhealthySegments(List<SegmentMeta> segmentsMeta) {
	// 	List<SegmentMeta> healthySegments = new ArrayList<>();

	// 	for (SegmentMeta segment : segmentsMeta) {
	// 		if (performHealthCheck(segment).isHealthy())
	// 			healthySegments.add(segment);
	// 	}

	// 	return healthySegments;
	// }

	/**
	 * Performs a comprehensive health check on a segment
	 * @param segmentMeta The segment to check
	 * @return SegmentHealthStatus object with detailed results
	 */
	public static SegmentHealthStatus performHealthCheck(SegmentMeta segmentMeta) {
		SegmentHealthStatus status = new SegmentHealthStatus(segmentMeta.getId());

		try {
			if (!(status.fileExists = isSegmentFileExists(segmentMeta))) {
				status.errors.add(SegmentHealthErrors.NOT_FOUND);
				return status; // returning here is crucial - performing other checks will result in IOException since the file already does not exist
			}

			if (!(status.sizeMatches = isSegmentSizeValid(segmentMeta)))
				status.errors.add(SegmentHealthErrors.SIZE_MISMATCH);

			if (!(status.checksumValid = isSegmentChecksumValid(segmentMeta)))
				status.errors.add(SegmentHealthErrors.CHECKSUM_MISMATCH);
		} catch (IOException e) {
			status.errors.add(SegmentHealthErrors.IO_ERROR);
		}

		return status;
	}

	/**
	 * Performs a comprehensive health check on a list of segments
	 * @param segmentsMeta List of segments to check
	 * @return List<SegmentHealthStatus> object with detailed results
	 */
	public static List<SegmentHealthStatus> performHealthCheck(List<SegmentMeta> segmentsMeta) {
		List<SegmentHealthStatus> healthIntegrityCheckResults = new ArrayList<>();

		for (SegmentMeta segment : segmentsMeta) {
			healthIntegrityCheckResults.add(performHealthCheck(segment));
		}

		return healthIntegrityCheckResults;
	}

	private static boolean isSegmentFileExists(SegmentMeta meta) {
		return Files.exists(meta.getPath()) && Files.isRegularFile(meta.getPath());
	}

	private static boolean isSegmentChecksumValid(SegmentMeta meta) {
		return ChecksumUtils.calculateFileChecksum(meta.getPath()).equals(meta.getChecksum());
	}

	private static boolean isSegmentSizeValid(SegmentMeta meta) throws IOException {
		return Files.size(meta.getPath()) == meta.getSize();
	}

	/**
	 * Status class for comprehensive integrity checks
	 */
	public static class SegmentHealthStatus {
		private final int segmentId;
		// private boolean valid = false;
		public boolean fileExists = false;
		public boolean sizeMatches = false;
		public boolean checksumValid = false;
		public List<SegmentHealthErrors> errors = new ArrayList<>();

		public SegmentHealthStatus(int segmentId) {
			this.segmentId = segmentId;
		}

		public List<SegmentHealthErrors> getHealthErrors() {
			return errors;
		}

		public boolean isHealthy() {
			return fileExists && sizeMatches && checksumValid;
		}

		@Override
		public String toString() {
			return String.format("SegmentHealthStatus{id=%d, fileExists=%b, sizeMatches=%b, checksumValid=%b, errors=%d}",
				segmentId, fileExists, sizeMatches, checksumValid, errors.size());
		}
	}

	public enum SegmentHealthErrors {
		NOT_FOUND,
		SIZE_MISMATCH,
		CHECKSUM_MISMATCH,
		IO_ERROR
	}
}
