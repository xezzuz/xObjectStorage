package cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClusterState {
	private final Map<String, ClusterNode> nodes;

	public ClusterState(List<ClusterNode> initialNodes) {
		this.nodes = new HashMap<>();

		for (ClusterNode node : initialNodes) {
			nodes.put(node.getEndpoint().getId(), node);
		}
	}

	public String getClusterStateSummary() {
		int total, healthy = 0, unhealthy = 0;
		double percentage;

		total = nodes.size();
		for (ClusterNode node : nodes.values()) {
			if (node.getStatus() == NodeStatus.HEALTHY)
				healthy++;
			else
				unhealthy++;
		}
		percentage = (healthy / total) * 100.0;

		return String.format(
			"Cluster state: %d/%d nodes healthy (%.2f%%) - %d unhealthy",
			healthy, total, percentage, unhealthy
		);
	}

	public void updateNodeStatus(String nodeId, NodeStatus newStatus) {
		ClusterNode toUpdate = nodes.get(nodeId);

		toUpdate.updateStatus(newStatus);
	}
}
