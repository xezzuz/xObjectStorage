package engine.segment;

import java.util.Optional;

import engine.util.ChecksumUtils;

import java.nio.file.Path;

public class SegmentMeta {
	private final int id;
	private final Path path;
	private long size;
	private SegmentState state;
	private String checksum;
	private long lastModified;

	public SegmentMeta(int id, Path path, Optional<Long> size, Optional<SegmentState> state, Optional<String> checksum, Optional<Long> lastModified) {
		this.id = id;
		this.path = path;
		this.size = size.orElse(0L);
		this.state = state.orElse(SegmentState.ACTIVE);
		this.checksum = checksum.orElse(ChecksumUtils.defaultChecksum);
		this.lastModified = lastModified.orElse(System.currentTimeMillis());
	}

	public static SegmentMeta createWithDefaults(int id, Path path) {
		return new SegmentMeta(
			id,
			path,
			Optional.ofNullable(null),
			Optional.ofNullable(null),
			Optional.ofNullable(null),
			Optional.ofNullable(null)
		);
	}

	public static Path resolveSegmentPathFromId(int id, Path baseDir) {
		return baseDir.resolve(String.format("segment-%04d.dat", id));
	}

	public void increaseSize(long size) {
		this.size += size;
	}

	public void decreaseSize(long size) {
		this.size -= size;
	}

	public int getId() { return id; }
	public Path getPath() { return path; }
	public long getSize() { return size; }
	public void setSize(long size) { this.size = size; }
	public SegmentState getState() { return state; }
	public void setState(SegmentState state) { this.state = state; }
	public String getChecksum() { return this.checksum; };
	public void setChecksum(String checksum) { this.checksum = checksum; };
	public long getLastModified() { return this.lastModified; };
	public void setLastModified(long lastModified) { this.lastModified = lastModified; };

	@Override
	public String toString() {
		return String.format(
			"SegmentMeta={id=%d, path='%s', size=%d, state='%s', checksum='%s', lastModified=%d}",
			id, path, size, state, checksum, lastModified
		);
	}
}
