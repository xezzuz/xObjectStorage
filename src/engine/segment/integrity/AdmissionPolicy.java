package engine.segment.integrity;

import engine.segment.integrity.IntegrityRegistry;
import engine.segment.integrity.enums.HealthStatus;


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
		if (report == null)
			return false; // i think it is safer to deny

		// this will evolve later (health vs failure vs severity)
		switch (report.getHealthStatus()) {
			case HEALTHY:
				return true;
			case UNHEALTHY:
				return false;
			default:
				return false;
		}
	}

	public boolean isReadable(int segmentId) {
		IntegrityReport report = registry.get(segmentId);
		if (report == null)
			return false; // i think it is safer to deny

		// this will evolve later (health vs failure vs severity)
		switch (report.getHealthStatus()) {
			case HEALTHY:
				return true;
			case DEGRADED:
				return true;
			default:
				return false;
		}
	}
}
