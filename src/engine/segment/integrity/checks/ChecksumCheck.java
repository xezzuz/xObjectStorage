package engine.segment.integrity.checks;

import java.nio.file.Path;

import engine.segment.SegmentMeta;
import engine.segment.integrity.IntegrityReport;
import engine.segment.integrity.enums.CheckStatus;
import engine.segment.integrity.enums.CheckType;
import engine.util.ChecksumUtils;

public class ChecksumCheck implements IntegrityCheck {
	private final CheckType CHECK_TYPE = CheckType.CHECKSUM;

	@Override
	public IntegrityCheckResult runCheck(SegmentMeta meta) {
		IntegrityCheckResult.Builder builder = new IntegrityCheckResult.Builder(CHECK_TYPE);

		try {
			Path segmentPath = meta.getPath();

			String expectedChecksum = meta.getChecksum();
			String actualChecksum = ChecksumUtils.calculateFileChecksum(segmentPath);

			return builder.status((actualChecksum == expectedChecksum) ? CheckStatus.PASS : CheckStatus.FAIL)
							.details(
								(actualChecksum == expectedChecksum) ? "Segment file checksum mismatches" :
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

			String expectedChecksum = meta.getChecksum();
			String actualChecksum = ChecksumUtils.calculateFileChecksum(segmentPath);

			report.actualChecksum(actualChecksum);

			IntegrityCheckResult checkResult =
				builder.status((actualChecksum == expectedChecksum) ? CheckStatus.PASS : CheckStatus.FAIL)
				.details(
					(actualChecksum == expectedChecksum) ? "Segment file checksum mismatches" :
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
