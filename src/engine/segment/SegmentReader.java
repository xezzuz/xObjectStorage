package engine.segment;

import engine.segment.*;
import objectabstraction.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.File;

import static logging.AppLogger.log;

public class SegmentReader {
	private final int BUFFER_SIZE = 4096;

	private final SegmentMeta meta;
	private final Segment unlockedSegment; // unlocked segment - no need to acquire for reading

	private final FileChannel readChannel;

	public SegmentReader(SegmentMeta segment) throws IOException {
		this.meta = segment;
		this.unlockedSegment = new Segment(segment);
		this.readChannel = FileChannel.open(
			unlockedSegment.getPath().toAbsolutePath(),
			StandardOpenOption.READ
		);
	}

	public InputStream read(ObjectLocation location) throws IOException {
		Path pathToSegment = unlockedSegment.getPath().toAbsolutePath();
		long offset = location.getOffset();
		long size = location.getSize();

		log.info("READ: Path = " + pathToSegment);
		log.info("READ: Offset = " + offset + ", Size = " + size);

		File file = pathToSegment.toFile();
		if (!file.exists()) {
			log.warning("File does not exist!");
			throw new IOException("File not found: " + pathToSegment);
		}

		long fileLength = file.length();
		log.info("READ: File length = " + fileLength);

		if (offset > fileLength) {
			log.warning("Offset is beyond file length!");
		}

		if (offset + size > fileLength) {
			log.warning("Requested read exceeds file length! EOF likely.");
		}

		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(offset);

		// For testing, attempt a read but catch EOFException to see if it triggers
		byte[] buffer;
		try {
			buffer = new byte[(int) size];
			log.fine("Attempting raf.readFully with size = " + size);
			raf.readFully(buffer, 0, (int) size);
		} catch (Exception e) {
			log.warning("Caught EOFException! Requested: " + size + ", available: " + (fileLength - offset));
			raf.close();
			throw e; // rethrow to preserve original behavior
		}

		raf.close();
		log.info("READ: Successfully read " + buffer.length + " bytes");

		return new ByteArrayInputStream(buffer);
	}


	// public Segment getSegment() {
	// 	return unlockedSegment;
	// }

	public SegmentMeta getSegmentMeta() {
		return meta;
	}
}
