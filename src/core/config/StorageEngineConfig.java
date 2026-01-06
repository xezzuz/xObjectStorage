package core.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public final class StorageEngineConfig {

	/* PUBLIC RUNTIME CONSTANTS */

	// SEGMENT FILE SETTINGS
	public static final long SEGMENT_MAX_SIZE;

	// STORAGE PATHS
	public static final String STORAGE_ROOT;

	// METADATA
	public static final String SEGMENT_POOL_DIR_NAME; // this should be dynamically created
	public static final String SEGMENT_POOL_METADATA_FILENAME;

	/* INTERNAL */
	private static final Properties PROPS = new Properties();

	static {
		loadProperties();

		SEGMENT_MAX_SIZE 					= getLong("segment.max.size");
		STORAGE_ROOT 						= getString("storage.root");
		SEGMENT_POOL_DIR_NAME 				= getString("segment.pool.dir.name");
		SEGMENT_POOL_METADATA_FILENAME 		= getString("segment.pool.metadata.filename");

		validateProperties();
	}

	private static void loadProperties() {
		// System.out.println("RESOURCE: " + StorageEngineConfig.class.getClassLoader().getResource("./resources/application.properties"));
		// System.out.println("RESOURCE: " + StorageEngineConfig.class.getClassLoader());
		try (
			FileInputStream in = new FileInputStream("resources/application.properties");
		) {

			// if (in == null) {
			// 	throw new IllegalStateException("application.properties not found on classpath");
			// }

			PROPS.load(in); // this parses the props inside the app.props file
		} catch (Exception e) {
			throw new RuntimeException("Failed to load application.properties file");
		}
	}

	private static String getString(String key) {
		String value = PROPS.getProperty(key);
		if (value == null || value.isBlank())
			throw new IllegalStateException(String.format("Missing required config key: '%s'", key));
		return value;
	}

	private static long getLong(String key) {
		try {
			String value = getString(key);
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new IllegalStateException(String.format("Invalid long value for config key: '%s'", key));
		}
	}

	private static void validateProperties() {
		if (SEGMENT_MAX_SIZE <= 0) {
			throw new IllegalStateException("segment.max.size must be > 0");
		}
	}

	private StorageEngineConfig() {}
}
