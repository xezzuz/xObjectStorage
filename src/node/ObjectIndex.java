package node;

import java.util.HashMap;
import java.util.Map;

import core.objectabstraction.ObjectLocation;

public class ObjectIndex {
	private final Map<String, ObjectLocation> inMemoryMap = new HashMap<>();

	public ObjectLocation get(String id) {
		return inMemoryMap.get(id);
	}

	public void put(String id, ObjectLocation location) {
		inMemoryMap.put(id, location);
	}
}
