package engine.segment.integrity.enums;

public enum CheckType {
	EXISTENCE, // FILE EXISTENCE
	PERMISSIONS, // JVM PERM
	SIZE, // SIZE MATCH
	CHECKSUM, // CHECKSUM MATCH
	IO
}
