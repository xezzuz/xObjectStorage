package engine;

import engine.segment.*;
import engine.config.*;
import objectabstraction.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class StorageEngine {
	private final Path rootStorageDir;
	private SegmentPoolManager segmentManager; // this represents a single bucket

	public StorageEngine() throws IOException {
		rootStorageDir = Path.of(StorageEngineConfig.STORAGE_ROOT);

		// create root storage if does not exist
		Files.createDirectories(rootStorageDir);

		this.segmentManager = new SegmentPoolManager(this.rootStorageDir.resolve(StorageEngineConfig.SEGMENT_POOL_DIR_NAME));
	}

	public ObjectLocation write(InputStream src) throws IOException {
		return segmentManager.append(src);
	}

	public InputStream read(ObjectLocation location) throws IOException {
		return segmentManager.read(location);
	}
}
