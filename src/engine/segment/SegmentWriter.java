package engine.segment;

import objectabstraction.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static logging.AppLogger.log;

public class SegmentWriter {
	private final int BUFFER_SIZE = 4096;

	private final SegmentMeta meta;
	private final Segment lockedSegment; // locked segment

	public SegmentWriter(SegmentMeta meta) throws IOException {
		this.meta = meta;
		this.lockedSegment = new Segment(meta);
	}

	public ObjectLocation write(InputStream src) throws IOException {
		/* need to lock the segment for writing */

		long	offset = lockedSegment.getCurrentOffset();
		long	totalWrittenBytes = 0;

		byte[] buffer = new byte[BUFFER_SIZE];

		while (true) {
			int 	readBytes = 0;
			long 	writtenBytes = 0;

			readBytes = src.read(buffer);
			if (readBytes <= 0)
				break ;

			writtenBytes = lockedSegment.write(ByteBuffer.wrap(buffer, 0, readBytes));

			totalWrittenBytes += writtenBytes;
		}

		lockedSegment.flush();

		/* need to unlock the segment for writing */

		meta.increaseSize(totalWrittenBytes);

		return new ObjectLocation(
			lockedSegment.getId(),
			offset,
			totalWrittenBytes
		);
	}

	public SegmentMeta getSegmentMeta() {
		return meta;
	}
}
