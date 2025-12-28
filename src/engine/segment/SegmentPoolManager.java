package engine.segment;

import engine.config.StorageEngineConfig;
import engine.segment.*;
import engine.util.ChecksumUtils;
import objectabstraction.*;

import static logging.AppLogger.log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/* this class manages a segment group aka buckets */

public class SegmentPoolManager {

	private final Path storageDir;

	private int nextSegmentId;

	private final long maxSegmentSize;

	private final SegmentPoolPersistence persistence;

	private List<SegmentMeta> allSegments = new ArrayList<>();
	private Queue<SegmentMeta> writableSegmentsPool = new LinkedList<>();

	public SegmentPoolManager(Path poolDirPath) {
		if (!Files.exists(poolDirPath)) {
			log.info("Segments Pool directory does not exist - Creating new one");

			try {
				Files.createDirectories(poolDirPath);
				log.info("Created segments pool directory at " + poolDirPath);
				Files.createFile(poolDirPath.resolve(StorageEngineConfig.SEGMENT_POOL_METADATA_FILENAME));
				log.info("Created segment pool metadata file at " + poolDirPath.resolve(StorageEngineConfig.SEGMENT_POOL_METADATA_FILENAME));
			} catch (IOException e) {
				throw new RuntimeException("Failed to create a new segment pool");
			}
		}

		this.storageDir = poolDirPath;
		this.maxSegmentSize = StorageEngineConfig.SEGMENT_MAX_SIZE;
		this.persistence = new SegmentPoolPersistence(storageDir);

		loadSegmentsIntoMemory();
		// loadWritableSegmentsPool();
	}

	public ObjectLocation append(InputStream src) throws IOException {
		SegmentWriter writer = getSegmentWriter();

		log.fine("APPENDING OPERATION TO SEGMENT " + writer.getSegmentMeta());
		// System.out.println("[TRACE] SegmentPoolManager APPENDING OPERATION TO SEGMENT " + writer.getSegmentMeta());

		ObjectLocation objectLocation =  writer.write(src);
		log.info("OBJECT LOCATION DEFINED " + objectLocation);
		// System.out.println("[TRACE] SegmentPoolManager OBJECT LOCATION DEFINED " + objectLocation);

		releaseWritableSegment(writer.getSegmentMeta());

		// need to check total read bytes against written bytes for anomalies

		// saveChanges();
		// this.persistence.save(new ArrayList<>(List.of(writer.getSegmentMeta())));
		this.persistence.save();

		return objectLocation;
	}

	public InputStream read(ObjectLocation location) throws IOException {
		SegmentReader reader = getSegmentReader(location.getSegmentId());

		log.fine("READING OPERATION TO SEGMENT " + reader);
		log.fine("OBJECT LOCATION " + location);
		// System.out.println("[TRACE] SegmentPoolManager READING OPERATION TO SEGMENT " + reader);
		// System.out.println("[TRACE] SegmentPoolManager OBJECT LOCATION " + location);

		return reader.read(location);
	}

	private SegmentMeta acquireWritableSegment() {
		SegmentMeta segment = writableSegmentsPool.poll();
		if (segment != null) {
			log.fine("GETTING A WRITABLE SEGMENT FROM POOL");
			// System.out.println("[TRACE] GETTING A WRITABLE SEGMENT FROM POOL");
			return segment;
		}

		// try {
			log.fine("GETTING A NEW SEGMENT - POOL IS EMPTY");
			// System.out.println("[TRACE] GETTING A NEW SEGMENT - POOL IS EMPTY");
			segment = getNextSegmentMeta();
			allSegments.add(segment);
		// } catch (IOException e) {
		// 	throw new RuntimeException("Failed to create segment", e);
		// }

		return segment;
	}

	private void releaseWritableSegment(SegmentMeta toRelease) throws IOException {
		if (isInNeedToBeSealed(toRelease)) {
			sealSegment(toRelease);
		} else {
			writableSegmentsPool.add(toRelease);
		}
	}

	// private void saveChanges() {
	// 	persistence.save(new ArrayList<>(writableSegmentsPool));
	// }

	private SegmentWriter getSegmentWriter() throws IOException {
		SegmentMeta writableSegment = acquireWritableSegment();

		return new SegmentWriter(writableSegment);
	}

	private SegmentReader getSegmentReader(int id) throws IOException {
		SegmentMeta readableSegment = null;

		/* need to be optimized */
		for (SegmentMeta segment : allSegments) {
			if (segment.getId() == id) {
				readableSegment = segment;
				break;
			}
		}

		if (readableSegment == null)
			throw new IllegalStateException("Failed to get a readable segment");

		return new SegmentReader(readableSegment);
	}

	private void sealSegment(SegmentMeta toBeSealed) {
		toBeSealed.setState(SegmentState.SEALED);
		log.fine("SEGMENT " + toBeSealed + " IS SEALED");
		// System.out.println("[TRACE] SEGMENT " + toBeSealed + " IS SEALED");
	}

	private boolean isInNeedToBeSealed(SegmentMeta segment) throws IOException {
		if (segment.getSize() >= maxSegmentSize) {
			log.fine("SEGMENT " + segment + " REACHED ITS MAXIMUM SIZE THRESHOLD");
			// System.out.println("[TRACE] SEGMENT " + segment + " REACHED ITS MAXIMUM SIZE THRESHOLD");
			return true;
		}
		return false;
	}

	// private SegmentMeta allocateNewSegment() throws IOException {
	// 	return new Segment(getNextSegmentMeta());
	// }

	private SegmentMeta getNextSegmentMeta() {
		Path segmentFilePath = generateSegmentFilePath(nextSegmentId);

		return SegmentMeta.createWithDefaults(nextSegmentId++, segmentFilePath);
	}

	// need to be optimized
	// private void loadWritableSegmentsPool() {
	// 	List<SegmentMeta> writableSegmentsList = persistence.getSegmentsByState(SegmentState.ACTIVE);

	// 	for (SegmentMeta segment : writableSegmentsList) {
	// 		writableSegmentsPool.add(segment);
	// 	}
	// }

	private void loadSegmentsIntoMemory() {
		allSegments = persistence.getAllSegments();

		int lastSegmentId = -1;
		for (SegmentMeta segment : allSegments) {
			if (segment.getState() == SegmentState.ACTIVE)
				this.writableSegmentsPool.add(segment);

			lastSegmentId = segment.getId();
		}

		nextSegmentId = lastSegmentId + 1;
	}

	private Path generateSegmentFilePath(int segmentId) {
		return storageDir.resolve(String.format("segment-%04d.dat", segmentId));
	}
}
