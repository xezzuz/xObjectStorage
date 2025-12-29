package engine.segment;

import static logging.AppLogger.log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

public class Segment {
	private final SegmentMeta	meta;

	private final FileChannel	segChannel;

	private final Checksum checksum = new CRC32();

	public Segment(SegmentMeta meta) throws IOException {
		this.meta = meta;
		this.segChannel = FileChannel.open(
			meta.getPath(),
			StandardOpenOption.CREATE,
			StandardOpenOption.READ,
			StandardOpenOption.WRITE
		);
	}

	public long write(ByteBuffer bufferToWrite) throws IOException {
		ByteBuffer checksumByteBuffer = bufferToWrite.asReadOnlyBuffer();

		// Ensure we're writing at the end of the file
		segChannel.position(segChannel.size()); // this needs to re-thinked of
		long bytesWritten = 0;

		while (bufferToWrite.hasRemaining()) {
			bytesWritten += segChannel.write(bufferToWrite);
		}


		checksum.update(checksumByteBuffer);
		meta.setChecksum(String.format("%08x", checksum.getValue()));

		return bytesWritten;
	}

	public void flush() throws IOException {
		this.segChannel.force(true);
	}

	public long getCurrentOffset() throws IOException {
		return this.segChannel.size();
	}

	public long getSize() throws IOException {
		return this.segChannel.size();
	}

	public Path getPath() {
		return this.meta.getPath();
	}

	public int getId() {
		return this.meta.getId();
	}

	public SegmentMeta getMeta() {
		return this.meta;
	}

	public String toString() {
		return String.format("Segment{" + meta + "}");
	}
}
