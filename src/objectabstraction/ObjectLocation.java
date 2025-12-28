package objectabstraction;

public class ObjectLocation {
	private final int segmentId; // parent segment
	private final long offset; // offset in segment
	private final long size; // size in segment

	public ObjectLocation(int segmentId, long offset, long size) {
		if (segmentId < 0 || offset < 0 || size <= 0)
			throw new IllegalArgumentException("Invalid object location arguments");

		this.segmentId = segmentId;
		this.offset = offset;
		this.size = size;
	}

	public int getSegmentId() {
		return this.segmentId;
	}

	public long getOffset() {
		return this.offset;
	}

	public long getSize() {
		return this.size;
	}

	@Override
	public String toString() {
		return String.format("ObjectLocation{segmentId=%04d, offset=%d, size=%d}", segmentId, offset, size);
	}

}
