package core.segment;

import static logging.AppLogger.log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

public class Segment {
	private final SegmentMeta	meta;

	private final FileChannel	segChannel;

	private final Checksum checksum;

	public Segment(SegmentMeta meta) throws IOException {
		this.meta = meta;
		this.segChannel = FileChannel.open(
			meta.getPath(),
			StandardOpenOption.CREATE,
			StandardOpenOption.READ,
			StandardOpenOption.WRITE
		);

		this.checksum = new CRC32();

		long fileSize = Files.size(meta.getPath());
		if (fileSize > 0) {
			try (InputStream is = Files.newInputStream(meta.getPath())) {
				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					this.checksum.update(buffer, 0, bytesRead);
				}
			}
		}
		log.severe("SegmentObject was created, current checksum: " + Long.toHexString(this.checksum.getValue()));
	}

	public long write(ByteBuffer bufferToWrite) throws IOException {
		segChannel.position(segChannel.size()); // this needs to re-thinked of
		long bytesWritten = 0;

		// copy of the buffer data to update the checksum
		byte[] dataToWrite = new byte[bufferToWrite.remaining()];
		bufferToWrite.get(dataToWrite);
		bufferToWrite.rewind(); // Reset for the actual write

		while (bufferToWrite.hasRemaining()) {
			bytesWritten += segChannel.write(bufferToWrite);
		}

		// update the checksum with the data that was just written
		checksum.update(dataToWrite, 0, (int)bytesWritten);

		// update the metadata with the current checksum
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
