import engine.segment.SegmentMeta;

public final class IntegrityChecker {
	public static IntegrityReport check(SegmentMeta meta, CheckSource source) {
		IntegrityReport.Builder reportBuilder =
			new IntegrityReport.Builder(meta.getId(), source);
	}
}
