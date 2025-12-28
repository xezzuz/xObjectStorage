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

	// public SegmentWriter(Segment segment) {
	// 	this.lockedSegment = segment;
	// }

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
			// log.fine(String.format("SegmentWriter READ OPERATION (%d bytes)", readBytes));
			if (readBytes <= 0)
				break ;

			writtenBytes = lockedSegment.write(ByteBuffer.wrap(buffer, 0, readBytes));
			// log.fine(String.format("SegmentWriter WRITE OPERATION (%d bytes)", readBytes));

			totalWrittenBytes += writtenBytes;
		}

		log.fine("SegmentWriter FLUSHING OPERATION ON LOCKED SEGMENT " + lockedSegment.toString());
		lockedSegment.flush();
		log.info("SegmentWriter TOTAL WRITTEN BYTES : " + totalWrittenBytes);

		/* need to unlock the segment for writing */

		log.warning("SEGMENT BEFORE SIZE INCREASE" + meta);
		meta.increaseSize(totalWrittenBytes);
		log.warning("SEGMENT AFTER SIZE INCREASE" + meta);

		return new ObjectLocation(
			lockedSegment.getId(),
			offset,
			totalWrittenBytes
		);
	}

	// public Segment getSegment() {
	// 	return lockedSegment;
	// }

	public SegmentMeta getSegmentMeta() {
		return meta;
	}
}
