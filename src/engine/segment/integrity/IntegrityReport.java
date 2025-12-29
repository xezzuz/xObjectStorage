import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import engine.segment.SegmentState;

public class IntegrityReport {
	/* ------------------ IDENTITY ------------------ */

	private final int segmentId;
	private final Path segmentPath;
	private final SegmentState segmentState;
	private final CheckSource checkSource; // startup, periodic, manual
	private final Instant checkTimestamp;



	/* ---------------- OBSERVATIONS ---------------- */

	private final boolean fileExists;
	private final boolean isRegularFile;
	private final boolean isReadableFile;

	private final Long expectedSize;
	private final Long actualSize;

	private final String expectedChecksum;
	private final String actualChecksum;

	private final Exception ioException;


	/* ---------------- CHECK RESULTS ---------------- */

	private final Map<CheckType, CheckResult> checkResults;


	/* ------------------- SUMMARY ------------------- */

	private final HealthStatus healthStatus;
	private final FailureCategory failureCategory;
	private final Severity severity;

	// private final boolean recoverable;
	// private final boolean requiresImmediateAction;
	// private final boolean safeToRetryChecks;

	private IntegrityReport(Builder builder) {
		this.segmentId = builder.segmentId;
		this.segmentPath = builder.segmentPath;
		this.segmentState = builder.segmentState;
		this.checkSource = builder.checkSource;
		this.checkTimestamp = builder.checkTimestamp;

		this.fileExists = builder.fileExists;
		this.isRegularFile = builder.isRegularFile;
		this.isReadableFile = builder.isReadableFile;
		this.expectedSize = builder.expectedSize;
		this.actualSize = builder.actualSize;
		this.expectedChecksum = builder.expectedChecksum;
		this.actualChecksum = builder.actualChecksum;
		this.ioException = builder.ioException;

		this.checkResults = Collections.unmodifiableMap(new EnumMap<>(builder.checkResults));

		this.healthStatus = builder.healthStatus;
		this.failureCategory = builder.failureCategory;
		this.severity = builder.severity;

		this.recoverable = builder.recoverable;
		this.requiresImmediateAction = builder.requiresImmediateAction;
		this.safeToRetryChecks = builder.safeToRetryChecks;
	}


	/* ------------------- GETTERS ------------------- */

	public int getSegmentId() {
		return segmentId;
	}

	public Path getSegmentPath() {
		return segmentPath;
	}

	public SegmentState getSegmentState() {
		return segmentState;
	}

	public CheckSource getCheckSource() {
		return checkSource;
	}

	public Instant getCheckTimestamp() {
		return checkTimestamp;
	}

	public Map<CheckType, CheckResult> getCheckResults() {
		return checkResults;
	}

	public HealthStatus getHealthStatus() {
		return healthStatus;
	}

	public FailureCategory getFailureCategory() {
		return failureCategory;
	}

	public Severity getSeverity() {
		return severity;
	}

	public Optional<Exception> getIOException() {
		return Optional.ofNullable(ioException);
	}

	public boolean isHealthy() {
		return healthStatus == HealthStatus.HEALTHY;
	}

	public static class Builder {
		private final int segmentId;
		private final Path segmentPath;
		private final SegmentState segmentState;
		private final CheckSource checkSource; // startup, periodic, manual
		private final Instant checkTimestamp = Instant.now();
		private final boolean fileExists;
		private final boolean isRegularFile;
		private final boolean isReadableFile;
		private final Long expectedSize;
		private final Long actualSize;
		private final String expectedChecksum;
		private final String actualChecksum;
		private final Exception ioException;
		private final Map<CheckType, CheckResult> checkResults = new EnumMap<>(CheckType.class);
		private final HealthStatus healthStatus = HealthStatus.UNKNOWN;
		private final FailureCategory failureCategory = FailureCategory.NONE;
		private final Severity severity = Severity.INFO;
	}
}
