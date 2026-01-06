package core.segment.integrity.checks;

import java.nio.file.Files;
import java.nio.file.Path;

import core.segment.SegmentMeta;
import core.segment.integrity.IntegrityReport;
import core.segment.integrity.enums.CheckStatus;
import core.segment.integrity.enums.CheckType;

public class PermissionsCheck implements IntegrityCheck {
	private final CheckType CHECK_TYPE = CheckType.PERMISSIONS;

	@Override
	public IntegrityCheckResult runCheck(SegmentMeta meta) {
		IntegrityCheckResult.Builder builder = new IntegrityCheckResult.Builder(CHECK_TYPE);

		try {
			Path segmentPath = meta.getPath();

			boolean isReadable = Files.isReadable(segmentPath);
			boolean isWritable = Files.isWritable(segmentPath);

			return builder.status((isReadable && isWritable) ? CheckStatus.PASS : CheckStatus.FAIL)
							.details(
								!isReadable ? "Segment file is not readable" :
								!isWritable ? "Segment file is not writable" :
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

			boolean isReadable = Files.isReadable(segmentPath);
			boolean isWritable = Files.isWritable(segmentPath);

			report.isReadable(isReadable);
			// TODO: ADD IS WRITABLE

			IntegrityCheckResult checkResult =
				builder.status((isReadable && isWritable) ? CheckStatus.PASS : CheckStatus.FAIL)
				.details(
					!isReadable ? "Segment file is not readable" :
					!isWritable ? "Segment file is not writable" :
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

	@Override
	public CheckType getCheckType() {
		return CHECK_TYPE;
	}
}
