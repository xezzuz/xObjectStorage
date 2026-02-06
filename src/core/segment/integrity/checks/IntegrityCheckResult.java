package core.segment.integrity.checks;

import java.util.Optional;

import core.segment.integrity.enums.*;

public class IntegrityCheckResult {
	private final CheckType type;
	private final CheckStatus status;

	private final String details; // EXPLAINS WHAT EXACTLY HAPPENED
	private final Exception exception; // EXCEPTION DURING CHECK

	public IntegrityCheckResult(CheckType type, CheckStatus status, String details, Exception exception) {
		this.type = type;
		this.status = status;
		this.details = details;
		this.exception = exception;
	}

	public CheckType getType() {
		return type;
	}

	public CheckStatus getStatus() {
		return status;
	}

	public Optional<String> getDetails() {
		return Optional.ofNullable(details);
	}

	public Optional<Exception> getException() {
		return Optional.ofNullable(exception);
	}

	public static IntegrityCheckResult newPass(CheckType type) {
		return new IntegrityCheckResult(type, CheckStatus.PASS, null, null);
	}

	public static IntegrityCheckResult newFail(CheckType type, String details) {
		return new IntegrityCheckResult(type, CheckStatus.FAIL, details, null);
	}

	public static IntegrityCheckResult newSkipped(CheckType type, String details) {
		return new IntegrityCheckResult(type, CheckStatus.SKIPPED, details, null);
	}

	public static IntegrityCheckResult newError(CheckType type, String details, Exception e) {
		return new IntegrityCheckResult(type, CheckStatus.ERROR, details, e);
	}

	public static class Builder {
		private CheckType type;
		private CheckStatus status;
		private String details;
		private Exception exception;

		public Builder(CheckType type) {
			this.type = type;
		}

		public Builder status(CheckStatus status) {
			this.status = status;
			return this;
		}

		public Builder details(String details) {
			this.details = details;
			return this;
		}

		public Builder exception(Exception exception) {
			this.exception = exception;
			return this;
		}

		public IntegrityCheckResult build() {
			return new IntegrityCheckResult(type, status, details, exception);
		}
	}
}
