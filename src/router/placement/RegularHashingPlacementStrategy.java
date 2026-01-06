package router.placement;

import java.util.List;

// import router.node.NodeEndpoint;
import cluster.NodeEndpoint;

public class RegularHashingPlacementStrategy implements PlacementStrategy {
	private final List<NodeEndpoint> nodes;

	public RegularHashingPlacementStrategy(List<NodeEndpoint> nodes) {
		this.nodes = List.copyOf(nodes);
	}

	@Override
	public NodeEndpoint place(String objectId) {
		int index = Math.floorMod(objectId.hashCode(), nodes.size());
		return nodes.get(index);
	}
}
