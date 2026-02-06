package node.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import engine.StorageEngine;
import node.ObjectIndex;
import objectabstraction.ObjectLocation;

public class NodeStorageService {
	private final StorageEngine engine;
	private final ObjectIndex index;

	public NodeStorageService(StorageEngine engine, ObjectIndex index) {
		this.engine = engine;
		this.index = index;
	}

	public void put(String id, InputStream in) throws IOException {
		ObjectLocation location = engine.write(in);
		index.put(id, location);
	}

	public InputStream get(String id) throws IOException {
		ObjectLocation location = index.get(id);
		if (location == null) {
			throw new FileNotFoundException(id);
		}

		InputStream in = engine.read(location);
		return in;
	}
}
