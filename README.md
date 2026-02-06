# xObjectStorage

⚠️ **WORK IN PROGRESS** - This project is still under active development and not yet complete. This is an experimental storage engine with plans for S3 compatibility, distributed storage nodes, replication, and advanced data recovery mechanisms.

A high-performance, segment-based object storage engine written in Java that efficiently stores and retrieves binary objects using a segmented file approach.

## Overview

xObjectStorage is designed to handle large binary objects by storing them in segments with metadata tracking. The system uses a pool-based approach where objects are written to segments until they reach a maximum size, at which point the segment is sealed and a new segment is created.

## Features

- **Segment-based Storage**: Objects are stored in segments that are managed in a pool
- **Automatic Segmentation**: When a segment reaches its maximum size, it's sealed and a new segment is created
- **Metadata Tracking**: Each stored object is tracked with its location (segment ID, offset, size)
- **Checksum Integrity**: CRC32 checksums for data integrity verification
- **Efficient I/O**: Uses streams for reading and writing to handle large objects efficiently
- **Persistence**: Segment metadata is persisted to disk for recovery
- **Thread-safe Operations**: Designed for concurrent read/write operations

## Architecture

### Core Components

- **StorageEngine**: The main entry point that manages the storage system, handling write and read operations
- **SegmentDirectory**: Manages a pool of segments (essentially a bucket), handling allocation, writing, and reading operations
- **ObjectLocation**: Represents the location of an object within the storage system (segment ID, offset, size)

### Segment Management

- **Segment**: Represents a physical file containing multiple stored objects
- **SegmentMeta**: Metadata about a segment including its state, size, checksum, and last modified time
- **SegmentWriter/Reader**: Handle writing and reading operations to/from segments
- **SegmentState**: Enum representing the state of a segment (ACTIVE, SEALED)

### Configuration

The system is configured via `resources/application.properties`:
- `segment.max.size`: Maximum size of a segment (default: 4096 bytes)
- `storage.root`: Root directory for storage (default: "storage")
- `segment.pool.dir.name`: Name of the segment pool directory (default: "bucket-1")
- `segment.pool.metadata.filename`: Name of the metadata file (default: "bucket.meta")

## Building and Running

### Prerequisites
- Java 11 or higher
- Make

### Build Commands
```bash
# Compile the project
make compile

# Run the application
make run

# Clean compiled files
make clean

# Clean and rebuild
make re
```

### Running the Application
The main class (`main.Main`) demonstrates the storage engine by:
1. Writing three files (a PNG image and two PDFs) to the storage engine
2. Reading them back using their ObjectLocation references
3. Saving them to an "out" directory

## Usage Example

```java
StorageEngine se = new StorageEngine();

// Write an object to storage
ObjectLocation location = se.write(new FileInputStream("input-file.bin"));

// Read the object back from storage
InputStream readStream = se.read(location);

// Use the read stream as needed
byte[] data = readStream.readAllBytes();
```

## Data Integrity

The system implements multiple layers of data integrity verification:
- CRC32 checksums for each segment file
- Checksum verification during load operations
- Size validation to ensure metadata matches actual file sizes
- Metadata file checksum to verify the integrity of the metadata itself

## File Structure
```
xObjectStorage/
├── Makefile                  # Build automation
├── bin/                      # Compiled Java classes
├── resources/
│   ├── application.properties # Configuration file
│   └── static/              # Sample files for testing
├── src/
│   ├── bucket/              # Bucket-related classes
│   ├── engine/              # Core storage engine
│   │   ├── config/          # Configuration classes
│   │   ├── segment/         # Segment management classes
│   │   └── util/            # Utility classes
│   ├── logging/             # Logging utilities
│   ├── main/                # Main application class
│   └── objectabstraction/   # Object location abstraction
└── storage/                 # Default storage directory (created at runtime)
```

## Performance Characteristics

- **Append-optimized**: Designed for high-throughput write operations
- **Memory efficient**: Minimal memory footprint for metadata management
- **Scalable**: Can handle large numbers of objects through segment pooling
- **Fast reads**: Direct file access based on offset and size information

## Future Enhancements

- Write-Ahead Logging (WAL) for improved durability
- S3 compatibility layer
- Multi-bucket support
- Distributed storage nodes
- Replication mechanisms
- Garbage collection for deleted objects
- Compression support
- Advanced data recovery mechanisms
- Distributed storage capabilities
- Advanced indexing with SQL database
- Backup and snapshot mechanisms
