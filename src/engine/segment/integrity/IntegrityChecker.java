import java.nio.file.Files;
import java.util.Map;

import engine.segment.SegmentMeta;
import engine.util.ChecksumUtils;

public final class IntegrityChecker {
	public static IntegrityReport check(SegmentMeta meta, CheckSource source) {
		IntegrityReport.Builder reportBuilder =
			new IntegrityReport.Builder(meta.getId(), source)
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
			}
		} catch (Exception e) {
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
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
				result.put(CheckType.CHECKSUM, "Prerequisite failed");
				// TODO: RETURN THE FINAL REPORT
			}
		} catch (Exception e) {
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
		}

		/* ------------------ CHECKSUM ------------------ */
		try {
			String actualChecksum = ChecksumUtils.calculateFileChecksum(meta.getPath());
			if (!actualChecksum.equals(meta.getChecksum())) {
				result.put(CheckType.CHECKSUM, passCheckResult(CheckType.CHECKSUM));
			} else {
				result.put(CheckType.CHECKSUM, failCheckResult(CheckType.CHECKSUM, String.format("Expected: '%s', Actual: '%s'", meta.getChecksum(), actualChecksum)));
				// SKIP DEPENDENT CHECKS IF ANY IN THE FUTURE
				// result.put(CheckType.CHECKSUM, "Prerequisite failed"); // EXAMPLE
				// TODO: RETURN THE FINAL REPORT
			}
		} catch (Exception e) {
			reportBuilder.exception(e);
			result.put(CheckType.IO, errorCheckResult(CheckType.IO, e.getMessage(), e));
			// TODO: RETURN THE FINAL REPORT
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
