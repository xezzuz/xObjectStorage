package engine.segment;

import engine.config.StorageEngineConfig;
import engine.segment.*;
import engine.segment.integrity.AdmissionPolicy;
import engine.util.ChecksumUtils;
import objectabstraction.*;

import static logging.AppLogger.log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/* this class manages a segment group aka buckets */
/**
 * SegmentDirectory manages a directory of segments and their runtime lifecycle.
 * its old name was SegmentPoolManager, ill implement that later
 * the SegmentDirectory should evolve later to contain a pool of readers and writers
 *
 * Responsibilities:
 * - Maintain in-memory metadata for all segments in the directory
 * - Allocate, seal, and persist segments
 * - Route read and write operations to appropriate segment readers/writers
 * - Enforce admission decisions provided by external policies
 *
 * This class does NOT:
 * - Perform integrity checks
 * - Decide system-wide behavior
 * - Implement recovery or health policies
 */
public class SegmentDirectory {

	private final Path storageDir;
	private final AdmissionPolicy admissionPolicy;

	private int nextSegmentId;

	private final long maxSegmentSize;

	private final SegmentDirectoryPersistence persistence;

	private List<SegmentMeta> allSegments = new ArrayList<>();
	private Queue<SegmentMeta> writableSegmentsPool = new LinkedList<>();

	public SegmentDirectory(Path poolDirPath, AdmissionPolicy admissionPolicy) {
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
		this.admissionPolicy = admissionPolicy;
		this.maxSegmentSize = StorageEngineConfig.SEGMENT_MAX_SIZE;
		this.persistence = new SegmentDirectoryPersistence(storageDir);

		loadSegmentsIntoMemory();
		// loadWritableSegmentsPool();
	}

	public ObjectLocation append(InputStream src) throws IOException {
		SegmentWriter writer = getSegmentWriter();

		log.fine("Appending operation to segment " + writer.getSegmentMeta().getId() + " started");
		ObjectLocation objectLocation =  writer.write(src);
		log.info("Appending operation to segment " + objectLocation.getSegmentId() + " succeeded (@" + objectLocation.getOffset() + ", " + objectLocation.getSize() + " bytes)");

		releaseWritableSegment(writer.getSegmentMeta());

		// need to check total read bytes against written bytes for anomalies

		// saveChanges();
		// this.persistence.save(new ArrayList<>(List.of(writer.getSegmentMeta())));
		this.persistence.save();

		return objectLocation;
	}

	public InputStream read(ObjectLocation location) throws IOException {
		SegmentReader reader = getSegmentReader(location.getSegmentId());

		log.fine("Reading operation from segment " + reader.getSegmentMeta().getId() + " started");
		InputStream in = reader.read(location);
		log.info("Reading operation from segment " + location.getSegmentId() + " succeeded (@" + location.getOffset() + ", " + location.getSize() + " bytes)");

		return in;
	}

	private SegmentMeta acquireWritableSegment() {
		SegmentMeta segment = writableSegmentsPool.poll();
		if (segment != null && admissionPolicy.isWritable(segment.getId())) {
			log.fine("Acquired writable segment " + segment.getId() + " from pool");
			return segment;
		}

		if (segment != null) {
			log.fine("Segment " + segment.getId() + " was in pool but denied by admission policy, getting new segment");
		} else {
			log.fine("Writable segment pool is empty, creating new segment");
		}

		segment = getNextSegmentMeta();
		if (admissionPolicy.isWritable(segment.getId())) {
			allSegments.add(segment);
			log.fine("Created new writable segment " + segment.getId());
			return segment;
		}

		log.severe("Failed to acquire a writable segment due to integrity + admission policy restrictions");
		throw new IllegalStateException("Failed to acquire a writable segment due to integrity + admission policy restrictions");
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

		if (readableSegment == null) {
			log.severe("Failed to get a readable segment with id " + id);
			throw new IllegalStateException("Failed to get a readable segment with id " + id);
		}

		if (!admissionPolicy.isReadable(id)) {
			log.warning("Segment " + id + " is not allowed for reading due to integrity + admission policy restrictions");
			throw new IllegalStateException("Segment " + id + " is not allowed for reading due to integrity + admission policy restrictions");
		}

		log.fine("Successfully retrieved readable segment " + id);
		return new SegmentReader(readableSegment);
	}

	private void sealSegment(SegmentMeta toBeSealed) {
		toBeSealed.setState(SegmentState.SEALED);
		log.fine("Sealing segment " + toBeSealed);
	}

	private boolean isInNeedToBeSealed(SegmentMeta segment) throws IOException {
		if (segment.getSize() >= maxSegmentSize) {
			log.fine("Segment " + segment.getId() + " reached the maximum size threshold");
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
		log.info("Loading segments into memory from persistence");
		allSegments = persistence.getAllSegments();
		log.info("Loaded " + allSegments.size() + " segments from persistence");

		int lastSegmentId = -1;
		int activeCount = 0;
		for (SegmentMeta segment : allSegments) {
			if (segment.getState() == SegmentState.ACTIVE) {
				this.writableSegmentsPool.add(segment);
				activeCount++;
			}

			lastSegmentId = segment.getId();
		}

		log.info("Added " + activeCount + " active segments to writable pool");
		nextSegmentId = lastSegmentId + 1;
		log.info("Next segment ID will be " + nextSegmentId);
	}

	private Path generateSegmentFilePath(int segmentId) {
		return storageDir.resolve(String.format("segment-%04d.dat", segmentId));
	}

	public List<SegmentMeta> getAllSegments() {
		return this.allSegments;
	}
}
