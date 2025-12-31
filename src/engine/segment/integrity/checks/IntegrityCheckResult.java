package engine.segment.integrity.checks;

import java.util.Optional;

import engine.segment.integrity.enums.*;

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
