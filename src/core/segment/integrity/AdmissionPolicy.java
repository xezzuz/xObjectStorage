package core.segment.integrity;

import static logging.AppLogger.log;

// import core.segment.integrity.IntegrityRegistry;
import core.segment.integrity.enums.*;


/**
 * AdmissionPolicy defines rules for allowing segment participation
 * in read and write operations based on integrity reports.
 *
 * Responsibilities:
 * - Decide whether a segment is readable or writable (not perms, by consuming integrity reports)
 * - Enforce safety boundaries using last known integrity facts
 *
 * This class should not perform integrity checks or recovery actions.
 */
public class AdmissionPolicy {
	private final IntegrityRegistry registry;

	public AdmissionPolicy(IntegrityRegistry registry) {
		this.registry = registry;
	}

	public boolean isWritable(int segmentId) {
		IntegrityReport report = registry.get(segmentId);
		if (report == null) {
			log.warning("No integrity report found for segment " + segmentId + ", allowing write access assuming its healthy");
			return true; // i think is safer to deny here - but we will decide later
		}

		boolean writable = report.getHealthStatus() == HealthStatus.HEALTHY;
		if (!writable) {
			log.warning("Segment " + segmentId + " denied write access due to health status: " + report.getHealthStatus());
		} else {
			log.fine("Segment " + segmentId + " granted write access, health: " + report.getHealthStatus());
		}
		return writable;
	}

	public boolean isReadable(int segmentId) {
		IntegrityReport report = registry.get(segmentId);
		if (report == null) {
			log.warning("No integrity report found for segment " + segmentId + ", allowing read access assuming its healthy");
			return true; // i think is safer to deny here - but we will decide later
		}

		boolean readable = report.getHealthStatus() == HealthStatus.HEALTHY ||
						  report.getHealthStatus() == HealthStatus.DEGRADED;
		if (!readable) {
			log.warning("Segment " + segmentId + " denied read access due to health status: " + report.getHealthStatus());
		} else {
			log.fine("Segment " + segmentId + " granted read access, health: " + report.getHealthStatus());
		}
		return readable;
	}
}
