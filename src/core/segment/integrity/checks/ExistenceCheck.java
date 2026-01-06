package core.segment.integrity.checks;

import java.nio.file.Files;
import java.nio.file.Path;

import core.segment.SegmentMeta;
import core.segment.integrity.IntegrityReport;
import core.segment.integrity.enums.CheckStatus;
import core.segment.integrity.enums.CheckType;

public class ExistenceCheck implements IntegrityCheck {
	private final CheckType CHECK_TYPE = CheckType.EXISTENCE;

	@Override
	public IntegrityCheckResult runCheck(SegmentMeta meta) {
		IntegrityCheckResult.Builder builder = new IntegrityCheckResult.Builder(CHECK_TYPE);

		try {
			Path segmentPath = meta.getPath();

			boolean exists = Files.exists(segmentPath);
			boolean isFile = Files.isRegularFile(segmentPath);

			return builder.status((exists && isFile) ? CheckStatus.PASS : CheckStatus.FAIL)
							.details(!exists ? "Segment file does not exist" : !isFile ? "Segment file is not a regular file" : null)
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

			boolean exists = Files.exists(segmentPath);
			boolean isFile = Files.isRegularFile(segmentPath);

			report.fileExists(exists)
					.isRegularFile(isFile);

			IntegrityCheckResult checkResult =
				builder.status((exists && isFile) ? CheckStatus.PASS : CheckStatus.FAIL)
				.details(!exists ? "Segment file does not exist" : !isFile ? "Segment file is not a regular file" : null)
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

	@Override
	public CheckType getCheckType() {
		return CHECK_TYPE;
	}
}
