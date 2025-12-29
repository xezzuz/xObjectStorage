public class CheckResult {
	private final CheckType type;
	private final CheckStatus status;

	private final String details; // EXPLAINS WHAT EXACTLY HAPPENED
	private final Exception exception; // EXCEPTION DURING CHECK

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
}
