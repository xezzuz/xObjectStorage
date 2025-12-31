package engine.segment.integrity.checks;

import engine.segment.SegmentMeta;
import engine.segment.integrity.IntegrityReport;
import engine.segment.integrity.enums.CheckType;

public interface IntegrityCheck {
	public IntegrityCheckResult runCheck(SegmentMeta meta);
	public IntegrityCheckResult runCheck(SegmentMeta meta, IntegrityReport.Builder report);
	public CheckType getCheckType();
}
