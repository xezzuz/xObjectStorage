package engine;

import engine.segment.*;
import engine.config.*;
import objectabstraction.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The StorageEngine is the composition root and lifecycle owner of the storage system.
 *
 * Responsibilities:
 * - Owns the storage root and initializes all subsystems
 * - Orchestrates startup integrity checks
 * - Composes integrity checking, reporting, and admission policies
 * - Exposes the public read/write API of the storage system
 *
 * This class makes system-level decisions but does not perform low level I/O
 * or segment specific logic.
 */
public class StorageEngine {
	private final Path rootStorageDir;
	private SegmentDirectory segmentManager; // this represents a single bucket

	public StorageEngine() throws IOException {
		rootStorageDir = Path.of(StorageEngineConfig.STORAGE_ROOT);

		// create root storage if does not exist
		Files.createDirectories(rootStorageDir);

		this.segmentManager = new SegmentDirectory(this.rootStorageDir.resolve(StorageEngineConfig.SEGMENT_POOL_DIR_NAME));
	}

	public ObjectLocation write(InputStream src) throws IOException {
		return segmentManager.append(src);
	}

	public InputStream read(ObjectLocation location) throws IOException {
		return segmentManager.read(location);
	}
}
