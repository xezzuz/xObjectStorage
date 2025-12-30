package engine.segment.integrity;

import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;

import engine.segment.SegmentMeta;
import engine.util.ChecksumUtils;

import engine.segment.integrity.enums.*;
import engine.segment.integrity.*;

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
		IntegrityReport.Builder reportBuilder =
			new IntegrityReport.Builder(meta.getId(), meta.getPath())
			.segmentState(meta.getState())
			.checkSource(source)
			.expectedSize(meta.getSize())
			.expectedChecksum(meta.getChecksum());

		Map<CheckType, CheckResult> result = new HashMap<>();

		/**
		 * this should evolve into a list of checks
		 * this would result in centrilized error handling (exceptions)
		 * also generating the final report would be straight forward
		 */

		/* ---------- EXISTENCE + PERMISSIONS ---------- */
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
					? new CheckResult(CheckType.EXISTENCE, CheckStatus.PASS, null, null)
					: new CheckResult(CheckType.EXISTENCE, CheckStatus.FAIL, "Segment file does not exist", null)
			);

			result.put(
				CheckType.PERMISSIONS,
				readable
					? new CheckResult(CheckType.PERMISSIONS, CheckStatus.PASS, null, null)
					: new CheckResult(CheckType.PERMISSIONS, CheckStatus.FAIL, "Segment file is not readable", null)
			);

			if (!exists || !regular || !readable) {
				// SKIP DEPENDENT CHECKS
				result.put(CheckType.SIZE, skipCheckResult(CheckType.SIZE, "Prerequisite failed"));
				result.put(CheckType.CHECKSUM, skipCheckResult(CheckType.CHECKSUM, "Prerequisite failed"));
				// TODO: RETURN THE FINAL REPORT
				return finalizeReport(reportBuilder, result);
			}
		} catch (Exception e) {
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
			return finalizeReport(reportBuilder, result);
		}

		/* -------------------- SIZE -------------------- */
		try {
			long actualSize = Files.size(meta.getPath());
			reportBuilder.actualSize(actualSize);

			if (actualSize == meta.getSize()) {
				result.put(CheckType.SIZE, passCheckResult(CheckType.SIZE));
			} else {
				result.put(CheckType.SIZE, failCheckResult(CheckType.SIZE, String.format("Expected: '%d', Actual: '%d'", meta.getSize(), actualSize)));
				// SKIP DEPENDENT CHECKS
				result.put(CheckType.CHECKSUM, skipCheckResult(CheckType.CHECKSUM, "Prerequisite failed"));
				// TODO: RETURN THE FINAL REPORT
				return finalizeReport(reportBuilder, result);
			}
		} catch (Exception e) {
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
			return finalizeReport(reportBuilder, result);
		}

		/* ------------------ CHECKSUM ------------------ */
		try {
			String actualChecksum = ChecksumUtils.calculateFileChecksum(meta.getPath());
			if (actualChecksum.equals(meta.getChecksum())) {
				result.put(CheckType.CHECKSUM, passCheckResult(CheckType.CHECKSUM));
			} else {
				result.put(CheckType.CHECKSUM, failCheckResult(CheckType.CHECKSUM, String.format("Expected: '%s', Actual: '%s'", meta.getChecksum(), actualChecksum)));
				// SKIP DEPENDENT CHECKS IF ANY IN THE FUTURE
				// result.put(CheckType.CHECKSUM, "Prerequisite failed"); // EXAMPLE
				// TODO: RETURN THE FINAL REPORT
				return finalizeReport(reportBuilder, result);
			}
		} catch (Exception e) {
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
		}
		return finalizeReport(reportBuilder, result);
	}

	private static IntegrityReport finalizeReport(IntegrityReport.Builder builder, Map<CheckType, CheckResult> results) {
		FailureCategory failureCategory = mapFailureCategory(results);
		HealthStatus healthStatus = mapHealthStatus(failureCategory);
		SeverityLevel severityLevel = mapSeverityLevel(failureCategory);

		builder.failureCategory(failureCategory)
			   .healthStatus(healthStatus)
			   .severity(severityLevel);
		// TODO: ADD IS RECOVERABLE

		for (CheckResult res : results.values()) {
			builder.addCheckResult(res);
		}

		return builder.build();
	}

	private static FailureCategory mapFailureCategory(Map<CheckType, CheckResult> results) {
		if (results.containsValue(CheckStatus.ERROR))
			return FailureCategory.IO_FAILURE;

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

	private static CheckResult passCheckResult(CheckType type) {
		return new CheckResult(type, CheckStatus.PASS, null, null);
	}

	private static CheckResult failCheckResult(CheckType type, String reason) {
		return new CheckResult(type, CheckStatus.FAIL, reason, null);
	}

	private static CheckResult skipCheckResult(CheckType type, String reason) {
		return new CheckResult(type, CheckStatus.SKIPPED, reason, null);
	}

	private static CheckResult errorCheckResult(CheckType type, String reason, Exception e) {
		return new CheckResult(type, CheckStatus.ERROR, reason, e);
	}
}
