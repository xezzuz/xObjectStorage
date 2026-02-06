package node.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

<<<<<<< HEAD:src/node/service/NodeStorageService.java
import engine.StorageEngine;
import node.ObjectIndex;
import objectabstraction.ObjectLocation;
=======
import core.StorageEngine;
import node.ObjectIndex;
import core.objectabstraction.ObjectLocation;
>>>>>>> d1c80038c4b6aef8a2a0e8f1771d3bf237b6a9b0:src/node/NodeStorageService.java

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
