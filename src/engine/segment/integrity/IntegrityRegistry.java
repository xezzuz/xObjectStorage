package engine.segment.integrity;

import java.util.Map;
import java.util.HashMap;
// import engine.segment.integrity.IntegrityReport;

import static logging.AppLogger.log;

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
		log.fine("Adding integrity report for segment " + report.getSegmentId() +
				", health: " + report.getHealthStatus());
		reports.put(report.getSegmentId(), report);
	}

	public IntegrityReport get(int segmentId) {
		IntegrityReport report = reports.get(segmentId);
		if (report == null) {
			log.fine("No integrity report found for segment " + segmentId);
		} else {
			log.fine("Retrieved integrity report for segment " + segmentId +
					", health: " + report.getHealthStatus());
		}
		return report;
	}

	// boolean has(int segmentId)?
}
