package engine.segment.integrity;

import java.util.ArrayList;
import java.util.List;

import static logging.AppLogger.log;

import engine.segment.SegmentMeta;
import engine.segment.integrity.checks.ChecksumCheck;
import engine.segment.integrity.checks.ExistenceCheck;
import engine.segment.integrity.checks.IntegrityCheck;
import engine.segment.integrity.checks.PermissionsCheck;
import engine.segment.integrity.checks.SizeCheck;
import engine.segment.integrity.enums.CheckSource;

/**
 * IntegrityMonitor manages startup, periodic and manual integrity checks on segments.
 * This class watch changes in the subsystem (observer) -> reflects those changes on the IntegrityRegistry
 * This class should not decide or perform any actions such as recovery
 * Its only purpose is managing and running integrity checks and update the registry. nothing more.
 */
public class IntegrityMonitor {
	private final IntegrityRegistry registry;

	private final List<IntegrityCheck> checks;

	public IntegrityMonitor(IntegrityRegistry registry) {
		this.registry = registry;
		this.checks = getDefaultIntegrityChecks();
	}

	public IntegrityMonitor(IntegrityRegistry registry, List<IntegrityCheck> checks) {
		this.registry = registry;
		this.checks = checks;
	}

	public void performStartupChecks(List<SegmentMeta> segments) {
		log.info("Starting (not yet multi-threaded) startup integrity checks for " + segments.size() + " segments");

		for (SegmentMeta segment : segments) {
			// run checks and generate report
			IntegrityReport segmentReport = runIntegrityChecks(segment, this.checks, CheckSource.STARTUP);

			// add report to registry
			registry.add(segmentReport);
		}

		log.info("All startup integrity checks completed successfully");
		logStartupSummary(segments);
	}

	/**
	 * Logs a summary of startup integrity check results.
	 */
	private void logStartupSummary(List<SegmentMeta> segments) {
		int healthyCount = 0;
		int unhealthyCount = 0;
		int degradedCount = 0;

		for (SegmentMeta segment : segments) {
			IntegrityReport report = registry.get(segment.getId());
			if (report != null) {
				switch (report.getHealthStatus()) {
					case HEALTHY:
						healthyCount++;
						log.info("Segment " + segment.getId() + " has " + report.getHealthStatus() + " status");
						break;
					case UNHEALTHY:
					case UNUSABLE:
						unhealthyCount++;
						log.severe("Segment " + segment.getId() + " failed integrity check: " +
								report.getFailureCategory());
						break;
					case DEGRADED:
						degradedCount++;
						log.warning("Segment " + segment.getId() + " has degraded status: " +
							report.getFailureCategory());
						break;
					default:
						log.info("Segment " + segment.getId() + " has " + report.getHealthStatus() + " status");
				}
			}
		}

		log.info("Startup integrity checks summary: " + healthyCount + " healthy, " +
				degradedCount + " degraded, " + unhealthyCount + " unhealthy");
	}

	private IntegrityReport runIntegrityChecks(SegmentMeta segment, List<IntegrityCheck> checks, CheckSource source) {
		return IntegrityChecker.check(segment, checks, source);
	}

	private List<IntegrityCheck> getDefaultIntegrityChecks() {
		List<IntegrityCheck> defaultChecks = new ArrayList<>();

		defaultChecks.add(new ExistenceCheck());
		defaultChecks.add(new PermissionsCheck());
		defaultChecks.add(new SizeCheck());
		defaultChecks.add(new ChecksumCheck());

		return defaultChecks;
	}
}
