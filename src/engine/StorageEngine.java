package engine;

import engine.segment.*;
import engine.segment.integrity.AdmissionPolicy;
import engine.segment.integrity.IntegrityChecker;
import engine.segment.integrity.IntegrityMonitor;
import engine.segment.integrity.IntegrityRegistry;
import engine.segment.integrity.IntegrityReport;
import engine.segment.integrity.enums.CheckSource;
import engine.config.*;
import objectabstraction.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static logging.AppLogger.log;

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
	private final Path rootStorageDir; // a disk?
	private final SegmentDirectory segmentDirectory; // a single bucket - later will be able to support multiple ones
	private final IntegrityRegistry registry;
	private final AdmissionPolicy admissionPolicy;
	private final IntegrityMonitor monitor;

	public StorageEngine() throws IOException {
		this.rootStorageDir = Path.of(StorageEngineConfig.STORAGE_ROOT);
		Files.createDirectories(rootStorageDir);

		this.registry = new IntegrityRegistry();
		this.admissionPolicy = new AdmissionPolicy(registry);
		this.monitor = new IntegrityMonitor(registry);

		this.segmentDirectory = new SegmentDirectory(
			this.rootStorageDir.resolve(StorageEngineConfig.SEGMENT_POOL_DIR_NAME),
			admissionPolicy
		);

		bootStorageEngine();
	}

	private void bootStorageEngine() {
		// perform startup integrity checks on all segmentDirectories (for now just one)
		monitor.performStartupChecks(this.segmentDirectory.getAllSegments());
	}

	public ObjectLocation write(InputStream src) throws IOException {
		return segmentDirectory.append(src);
	}

	public InputStream read(ObjectLocation location) throws IOException {
		return segmentDirectory.read(location);
	}
}
