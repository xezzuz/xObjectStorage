package engine.segment.integrity;

import java.util.Map;
import java.util.List;

import engine.segment.SegmentMeta;

import engine.segment.integrity.enums.*;
import engine.segment.integrity.checks.*;

import static logging.AppLogger.log;

/**
 * SegmentIntegrityChecker performs physical integrity checks on segment resources.
 *
 * Responsibilities:
 * - Runs an IntegrityCheck on a segment
 * - Produce immutable integrity reports describing observed state
 *
 * This class observes facts, it should not make decisions.
 */
public final class IntegrityChecker {

	/**
	 *
	 * @param meta
	 * @param checks
	 * @param source
	 * @return
	 */
	public static IntegrityReport check(SegmentMeta meta, List<IntegrityCheck> checks, CheckSource source) {
		log.fine("Starting " + checks.size() + " integrity checks for segment " + meta.getId() + " from source " + source);

		IntegrityReport.Builder segmentReportBuilder = new IntegrityReport.Builder(meta.getId(), meta.getPath());

		boolean skipAllChecks = false;
		for (IntegrityCheck singleCheck : checks) {
			if (skipAllChecks) {
				segmentReportBuilder.addIntegrityCheckResult(IntegrityCheckResult.newSkipped(singleCheck.getCheckType(), "Prerequisite failed"));
			} else {
				IntegrityCheckResult checkResult = singleCheck.runCheck(meta, segmentReportBuilder);
				if (checkResult.getStatus() != CheckStatus.PASS)
					skipAllChecks = true;
			}
		}

		IntegrityReport finalReport = finalizeReport(segmentReportBuilder);

		log.fine("Completed " + checks.size() + " integrity checks for segment " + meta.getId() + " from source " + source);

		return finalReport;
	}

	// private void skipCheck() {

	// }

	private static IntegrityReport finalizeReport(IntegrityReport.Builder builder) {
		FailureCategory failureCategory = mapFailureCategory(builder.getIntegrityCheckResults());
		HealthStatus healthStatus = mapHealthStatus(failureCategory);
		SeverityLevel severityLevel = mapSeverityLevel(failureCategory);

		builder.failureCategory(failureCategory)
			   .healthStatus(healthStatus)
			   .severity(severityLevel);
		// TODO: ADD IS RECOVERABLE

		// for (IntegrityCheckResult res : results.values()) {
		// 	builder.addIntegrityCheckResult(res);
		// }

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

	// private static IntegrityCheckResult passIntegrityCheckResult(CheckType type) {
	// 	return new IntegrityCheckResult(type, CheckStatus.PASS, null, null);
	// }

	// private static IntegrityCheckResult failIntegrityCheckResult(CheckType type, String reason) {
	// 	return new IntegrityCheckResult(type, CheckStatus.FAIL, reason, null);
	// }

	// private static IntegrityCheckResult skipIntegrityCheckResult(CheckType type, String reason) {
	// 	return new IntegrityCheckResult(type, CheckStatus.SKIPPED, reason, null);
	// }

	// private static IntegrityCheckResult errorIntegrityCheckResult(CheckType type, String reason, Exception e) {
	// 	return new IntegrityCheckResult(type, CheckStatus.ERROR, reason, e);
	// }
}
