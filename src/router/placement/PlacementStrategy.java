package router.placement;

// import router.node.NodeEndpoint;
import cluster.NodeEndpoint;

public interface PlacementStrategy {
	public NodeEndpoint place(String objectId);
}
