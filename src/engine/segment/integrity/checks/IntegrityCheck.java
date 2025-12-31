package engine.segment.integrity.checks;

import engine.segment.SegmentMeta;
import engine.segment.integrity.IntegrityReport;

public interface IntegrityCheck {
	public IntegrityCheckResult runCheck(SegmentMeta meta);
	public IntegrityCheckResult runCheck(SegmentMeta meta, IntegrityReport.Builder report);

	// TODO: ADD STATUC FUNCTIONS TO GET INSTANCES OF INTEGRITYCHECKRESULT
	// TODO: USE THEM INSIDE THE FUNCTIONS runCheck
}
