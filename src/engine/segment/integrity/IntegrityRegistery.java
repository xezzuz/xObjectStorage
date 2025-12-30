package engine.segment.integrity;

import java.util.Map;
import java.util.HashMap;
import engine.segment.integrity.IntegrityReport;

/**
 * IntegrityRegistry stores the latest known integrity reports for each segment in a SegmentDirectory.
 *
 * Responsibilities:
 * - Act as a single source of truth for segment health
 * - Provide fast, read-only access to integrity reports
 *
 * This class should not perform checks or enforce policies.
 */
public class IntegrityRegistry {
	// not thread safe
	private final Map<Integer, IntegrityReport> reports = new HashMap<>();

	public void add(IntegrityReport report) {
		reports.put(report.getSegmentId(), report);
	}

	public IntegrityReport get(int segmentId) {
		return reports.get(segmentId);
	}

	// boolean has(int segmentId)?
}
