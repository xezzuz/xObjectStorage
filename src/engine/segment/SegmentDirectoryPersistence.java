package engine.segment;

import engine.config.*;
import engine.util.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static logging.AppLogger.log;

public class SegmentDirectoryPersistence {
	private final Path poolDirPath;
	private final Path metadataFile;

	private final Map<Integer, SegmentMeta> inMemorySegments;

	private final List<SegmentMeta> allSegments;

	public SegmentDirectoryPersistence(Path poolDirPath) {
		this.poolDirPath = poolDirPath;
		this.metadataFile = poolDirPath.resolve(StorageEngineConfig.SEGMENT_POOL_METADATA_FILENAME);
		this.inMemorySegments = new HashMap<>();

		this.allSegments = new ArrayList<>();

		populateAllSegments();
		populateInMemorySegments();
	}

	public List<SegmentMeta> getAllSegments() {
		return allSegments;
	}

	public List<SegmentMeta> getSegmentsByState(SegmentState state) {
		List<SegmentMeta> result = new ArrayList<>();

		for (SegmentMeta segment : allSegments) {
			if (segment.getState() == state) {
				result.add(segment);
			}
		}

		return result;
	}

	private void populateInMemorySegments() {
		for (SegmentMeta segment : allSegments) {
			inMemorySegments.put(segment.getId(), segment);
		}
	}

	private void populateAllSegments() {
		/* note: these are all the segments present in the metadata file */
		allSegments.addAll(this.loadFromDisk().getAllSegments());
	}

	/*
		this function api abstracts the process of loading the persistent data
		into memory for further use

		in the future this should implement multiple loading features such as
		WAL - write ahead logs
		replay logs on load
		identify inconsistencies from temporary files
		rollback mechanism through backup files
	*/
	private SegmentDirectoryPersistenceLoadResult loadFromDisk() {
		if (!Files.exists(metadataFile)) {
			log.warning("METADATA FILE DOES NOT EXIST");
				return new SegmentDirectoryPersistenceLoadResult();
		}

		List<SegmentMeta> allSegments = new ArrayList<>();
		try (Scanner sc = new Scanner(metadataFile)) {
			int linesCount = 0;

			while (sc.hasNextLine()) {
				String segmentMetaLine = sc.nextLine();
				linesCount++;

				try {
					SegmentMeta parsedMeta = parseMetaLine(segmentMetaLine);
					allSegments.add(parsedMeta);
				} catch (Exception e) {
					log.warning(String.format("INVALID LINE AT LINE '%d' - SKIPPING", linesCount));
					continue;
				}
			}
		} catch (Exception e) {
			log.severe("Exception Caught: " + e.getMessage());
			e.printStackTrace();
		}

		log.info("LOADING METADATA FROM DISK RESULT " + allSegments);

		return new SegmentDirectoryPersistenceLoadResult(allSegments);
	}

	/*
		this function takes segments meta updates and performs the saving algo
		this is just a high level api used by external classes such as SegmentManager
		this in the future should implement different persistence features such as:
		batching updates
		optimizing the saving to disk
		perform backups
		identify unconsistencies

		i think that updates to memory should be reflected immediately - to disk should be batched
	*/
	public void save() {
		log.fine("SAVING ALL SEGMENTS INTO TEMP FILE " + allSegments);

		Path tempMetaFile = poolDirPath.resolve("meta.data.tmp");

		// should we sort before saving to meta file?

		try (
			FileChannel channel = FileChannel.open(
				tempMetaFile,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE
			);
		) {
			for (SegmentMeta segment : allSegments) {
				log.fine("CURRENT SEGMENT TO SAVE " + segment);

				byte[] lineBytes = (formatMetaLine(segment) + "\n").getBytes();

				ByteBuffer buffer = ByteBuffer.wrap(lineBytes);

				while (buffer.hasRemaining()) {
					channel.write(buffer);
				}
			}

			channel.force(true);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write inMemoryData to temp metadata file", e);
		}

		try {
			Files.move(
				tempMetaFile,
				metadataFile,
				StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.ATOMIC_MOVE
			);
		} catch (IOException e) {
			throw new RuntimeException("Failed to atomically move tempmetafile into metafile", e);
		}
	}

	private SegmentMeta parseMetaLine(String metaLine) {
		String[] splitted = metaLine.split(":");
		if (splitted.length != 6)
			throw new RuntimeException("Invalid line: " + metaLine);

		try {
			int segmentId = Integer.parseInt(splitted[0]);
			Path segmentFilePath = poolDirPath.resolve(splitted[1]);
			SegmentState segmentState = SegmentState.valueOf(splitted[2]);
			long segmentSize = Long.parseLong(splitted[3]);
			String segmentChecksum = splitted[4];
			long segmentLastModified = Long.parseLong(splitted[5]);

			return new SegmentMeta(
				segmentId,
				segmentFilePath,
				Optional.of(segmentSize),
				Optional.of(segmentState),
				Optional.of(segmentChecksum),
				Optional.of(segmentLastModified)
			);
		} catch (Exception e) {
			throw new RuntimeException("Invalid line: " + metaLine + e.getMessage(), e);
		}
	}

	private String formatMetaLine(SegmentMeta meta) {
		String segmentFilename = meta.getPath().getFileName().toString();

		return meta.getId() + ":"
				+ segmentFilename + ":"
				+ meta.getState().name() + ":"
				+ meta.getSize() + ":"
				+ meta.getChecksum() + ":"
				+ System.currentTimeMillis();
	}

	public static class SegmentDirectoryPersistenceLoadResult {
		private final List<SegmentMeta> segments;

		public SegmentDirectoryPersistenceLoadResult() {
			this(Collections.emptyList());
		}

		public SegmentDirectoryPersistenceLoadResult(List<SegmentMeta> segments) {
			this.segments = segments;
		}

		public List<SegmentMeta> getAllSegments() {
			return segments;
		}

		@Override
		public String toString() {
			return String.format("SegmentDirectoryPersistenceLoadResult{segments='%s'}", segments);
		}
	}
}
