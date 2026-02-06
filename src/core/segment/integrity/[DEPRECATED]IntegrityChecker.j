package core.segment.integrity;

import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;

import core.segment.SegmentMeta;
import core.util.ChecksumUtils;

import core.segment.integrity.enums.*;
import core.segment.integrity.checks.*;

import static logging.AppLogger.log;

/**
 * SegmentIntegrityChecker performs physical integrity checks on segment resources.
 *
 * Responsibilities:
 * - Validate on disk segment existence, size, and checksum
 * - Produce immutable integrity reports describing observed state
 *
 * This class observes facts, it should not make decisions.
 */
public final class IntegrityChecker {
	public static IntegrityReport check(SegmentMeta meta, CheckSource source) {
		log.info("Starting integrity check for segment " + meta.getId() + " from source " + source);

		IntegrityReport.Builder reportBuilder =
			new IntegrityReport.Builder(meta.getId(), meta.getPath())
			.segmentState(meta.getState())
			.checkSource(source)
			.expectedSize(meta.getSize())
			.expectedChecksum(meta.getChecksum());

		Map<CheckType, IntegrityCheckResult> result = new HashMap<>();

		/**
		 * this should evolve into a list of checks
		 * this would result in centrilized error handling (exceptions)
		 * also generating the final report would be straight forward
		 */

		/* ---------- EXISTENCE + PERMISSIONS ---------- */
		log.fine("Checking existence and permissions for segment " + meta.getId());
		try {
			boolean exists = Files.exists(meta.getPath());
			boolean regular = exists && Files.isRegularFile(meta.getPath());
			boolean readable = exists && Files.isReadable(meta.getPath());

			reportBuilder.fileExists(exists)
						 .isRegularFile(regular)
						 .isReadable(readable);

			result.put(
				CheckType.EXISTENCE,
				exists
					? new IntegrityCheckResult(CheckType.EXISTENCE, CheckStatus.PASS, null, null)
					: new IntegrityCheckResult(CheckType.EXISTENCE, CheckStatus.FAIL, "Segment file does not exist", null)
			);

			result.put(
				CheckType.PERMISSIONS,
				readable
					? new IntegrityCheckResult(CheckType.PERMISSIONS, CheckStatus.PASS, null, null)
					: new IntegrityCheckResult(CheckType.PERMISSIONS, CheckStatus.FAIL, "Segment file is not readable", null)
			);

			if (!exists || !regular || !readable) {
				log.warning("Segment " + meta.getId() + " failed existence/perms check, skipping dependent checks");
				// SKIP DEPENDENT CHECKS
				result.put(CheckType.SIZE, skipIntegrityCheckResult(CheckType.SIZE, "Prerequisite failed"));
				result.put(CheckType.CHECKSUM, skipIntegrityCheckResult(CheckType.CHECKSUM, "Prerequisite failed"));
				// TODO: RETURN THE FINAL REPORT
				return finalizeReport(reportBuilder, result);
			}
		} catch (Exception e) {
			log.severe("Exception during existence/permissions check for segment " + meta.getId() + ": " + e.getMessage());
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorIntegrityCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
			return finalizeReport(reportBuilder, result);
		}

		/* -------------------- SIZE -------------------- */
		log.fine("Checking size for segment " + meta.getId());
		try {
			long actualSize = Files.size(meta.getPath());
			reportBuilder.actualSize(actualSize);

			if (actualSize == meta.getSize()) {
				result.put(CheckType.SIZE, passIntegrityCheckResult(CheckType.SIZE));
			} else {
				log.warning("Segment " + meta.getId() + " size mismatch: expected " + meta.getSize() + ", actual " + actualSize);
				result.put(CheckType.SIZE, failIntegrityCheckResult(CheckType.SIZE, String.format("Expected: '%d', Actual: '%d'", meta.getSize(), actualSize)));
				// SKIP DEPENDENT CHECKS
				result.put(CheckType.CHECKSUM, skipIntegrityCheckResult(CheckType.CHECKSUM, "Prerequisite failed"));
				// TODO: RETURN THE FINAL REPORT
				return finalizeReport(reportBuilder, result);
			}
		} catch (Exception e) {
			log.severe("Exception during size check for segment " + meta.getId() + ": " + e.getMessage());
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorIntegrityCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
			return finalizeReport(reportBuilder, result);
		}

		/* ------------------ CHECKSUM ------------------ */
		log.fine("Checking checksum for segment " + meta.getId());
		try {
			String actualChecksum = ChecksumUtils.calculateFileChecksum(meta.getPath());
			if (actualChecksum.equals(meta.getChecksum())) {
				result.put(CheckType.CHECKSUM, passIntegrityCheckResult(CheckType.CHECKSUM));
			} else {
				log.warning("Segment " + meta.getId() + " checksum mismatch: expected " + meta.getChecksum() + ", actual " + actualChecksum);
				result.put(CheckType.CHECKSUM, failIntegrityCheckResult(CheckType.CHECKSUM, String.format("Expected: '%s', Actual: '%s'", meta.getChecksum(), actualChecksum)));
				// SKIP DEPENDENT CHECKS IF ANY IN THE FUTURE
				// result.put(CheckType.CHECKSUM, "Prerequisite failed"); // EXAMPLE
				// TODO: RETURN THE FINAL REPORT
				return finalizeReport(reportBuilder, result);
			}
		} catch (Exception e) {
			log.severe("Exception during checksum check for segment " + meta.getId() + ": " + e.getMessage());
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorIntegrityCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
		}
		IntegrityReport report = finalizeReport(reportBuilder, result);
		log.info("Completed integrity check for segment " + meta.getId() + ", health: " + report.getHealthStatus());
		return report;
	}

	private static IntegrityReport finalizeReport(IntegrityReport.Builder builder, Map<CheckType, IntegrityCheckResult> results) {
		FailureCategory failureCategory = mapFailureCategory(results);
		HealthStatus healthStatus = mapHealthStatus(failureCategory);
		SeverityLevel severityLevel = mapSeverityLevel(failureCategory);

		builder.failureCategory(failureCategory)
			   .healthStatus(healthStatus)
			   .severity(severityLevel);
		// TODO: ADD IS RECOVERABLE

		for (IntegrityCheckResult res : results.values()) {
			builder.addIntegrityCheckResult(res);
		}

		return builder.build();
	}

	private static FailureCategory mapFailureCategory(Map<CheckType, IntegrityCheckResult> results) {
		// TODO: FIX THIS PLIZ HH
		// if (results.containsValue(CheckStatus.ERROR))
		// 	return FailureCategory.IO_FAILURE;


		if (results.containsKey(CheckType.EXISTENCE) && results.get(CheckType.EXISTENCE).getStatus() == CheckStatus.FAIL)
			return FailureCategory.MISSING_RESOURCE;
		if (results.containsKey(CheckType.PERMISSIONS) && results.get(CheckType.PERMISSIONS).getStatus() == CheckStatus.FAIL)
			return FailureCategory.PERMISSION_ISSUE;
		if (results.containsKey(CheckType.CHECKSUM) && results.get(CheckType.CHECKSUM).getStatus() == CheckStatus.FAIL)
			return FailureCategory.DATA_CORRUPTION;
		if (results.containsKey(CheckType.SIZE) && results.get(CheckType.SIZE).getStatus() == CheckStatus.FAIL)
			return FailureCategory.METADATA_MISMTACH;

		return FailureCategory.NONE;
	}

	private static HealthStatus mapHealthStatus(FailureCategory category) {
		switch (category) {
			case NONE:
				return HealthStatus.HEALTHY;
			case METADATA_MISMTACH:
				return HealthStatus.DEGRADED;
			case DATA_CORRUPTION, PERMISSION_ISSUE, IO_FAILURE:
				return HealthStatus.UNHEALTHY;
			case MISSING_RESOURCE:
				return HealthStatus.UNUSABLE;
			default:
				return HealthStatus.UNKNOWN;
		}
	}

	private static SeverityLevel mapSeverityLevel(FailureCategory category) {
		switch (category) {
			case NONE:
				return SeverityLevel.INFO;
			case METADATA_MISMTACH:
				return SeverityLevel.WARNING;
			case DATA_CORRUPTION, PERMISSION_ISSUE, IO_FAILURE:
				return SeverityLevel.ERROR;
			case MISSING_RESOURCE:
				return SeverityLevel.CRITICAL;
			default:
				return SeverityLevel.ERROR;
		}
	}

	private static IntegrityCheckResult passIntegrityCheckResult(CheckType type) {
		return new IntegrityCheckResult(type, CheckStatus.PASS, null, null);
	}

	private static IntegrityCheckResult failIntegrityCheckResult(CheckType type, String reason) {
		return new IntegrityCheckResult(type, CheckStatus.FAIL, reason, null);
	}

	private static IntegrityCheckResult skipIntegrityCheckResult(CheckType type, String reason) {
		return new IntegrityCheckResult(type, CheckStatus.SKIPPED, reason, null);
	}

	private static IntegrityCheckResult errorIntegrityCheckResult(CheckType type, String reason, Exception e) {
		return new IntegrityCheckResult(type, CheckStatus.ERROR, reason, e);
	}
}
