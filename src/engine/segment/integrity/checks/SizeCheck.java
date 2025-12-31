package engine.segment.integrity.checks;

import java.nio.file.Files;
import java.nio.file.Path;

import engine.segment.SegmentMeta;
import engine.segment.integrity.IntegrityReport;
import engine.segment.integrity.enums.CheckStatus;
import engine.segment.integrity.enums.CheckType;

public class SizeCheck implements IntegrityCheck {
	private final CheckType CHECK_TYPE = CheckType.SIZE;

	@Override
	public IntegrityCheckResult runCheck(SegmentMeta meta) {
		IntegrityCheckResult.Builder builder = new IntegrityCheckResult.Builder(CHECK_TYPE);

		try {
			Path segmentPath = meta.getPath();

			long expectedSize = meta.getSize();
			long actualSize = Files.size(segmentPath);

			return builder.status((actualSize == expectedSize) ? CheckStatus.PASS : CheckStatus.FAIL)
							.details(
								(actualSize == expectedSize) ? "Segment file size mismatches" :
								null)
							.build();
		} catch (Exception e) {
			return builder.status(CheckStatus.FAIL)
							.details("Exception caught during check " + e.getMessage())
							.exception(e)
							.build();
		}
	}

	@Override
	public IntegrityCheckResult runCheck(SegmentMeta meta, IntegrityReport.Builder report) {
		IntegrityCheckResult.Builder builder = new IntegrityCheckResult.Builder(CHECK_TYPE);

		try {
			Path segmentPath = meta.getPath();

			long expectedSize = meta.getSize();
			long actualSize = Files.size(segmentPath);

			report.actualSize(actualSize);

			IntegrityCheckResult checkResult =
				builder.status((actualSize == expectedSize) ? CheckStatus.PASS : CheckStatus.FAIL)
				.details(
					(actualSize == expectedSize) ? "Segment file size mismatches" :
					null)
				.build();

			report.addIntegrityCheckResult(checkResult);

			return checkResult;
		} catch (Exception e) {
			IntegrityCheckResult checkResult =
				builder.status(CheckStatus.FAIL)
				.details("Exception caught during check " + e.getMessage())
				.exception(e)
				.build();

			report.addIntegrityCheckResult(checkResult);

			return checkResult;
		}
	}
}
