package core.segment.integrity.checks;

import core.segment.SegmentMeta;
import core.segment.integrity.IntegrityReport;
import core.segment.integrity.enums.CheckType;

public interface IntegrityCheck {
	public IntegrityCheckResult runCheck(SegmentMeta meta);
	public IntegrityCheckResult runCheck(SegmentMeta meta, IntegrityReport.Builder report);
	public CheckType getCheckType();
}
