package cluster;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ClusterConfig {
	private final String clusterId;
	private final int clusterRouterPort;
	private final List<NodeEndpoint> nodes;
	private final long healthCheckIntervalMs;
	private final long healthCheckTimeoutMs;

	private ClusterConfig(String clusterId, int clusterRouterPort, List<NodeEndpoint> nodes,
						  long healthCheckIntervalMs, long healthCheckTimeoutMs) {
		this.clusterId = clusterId;
		this.clusterRouterPort = clusterRouterPort;
		this.nodes = List.copyOf(nodes);
		this.healthCheckIntervalMs = healthCheckIntervalMs;
		this.healthCheckTimeoutMs = healthCheckTimeoutMs;
	}

	public static ClusterConfig loadFromPropertiesConfigFile(String filePath) throws IOException {
		Properties props = new Properties();

		try (FileInputStream fs = new FileInputStream(filePath)) {
			props.load(fs);
		}

		String clusterId = props.getProperty("cluster.id", "default-cluster-id");
		int clusterRouterPort = Integer.parseInt(
			props.getProperty("cluster.router.port", "3000")
		);

		List<NodeEndpoint> nodes = parseNodesEndpointsFromProperites(props);

		long healthCheckIntervalMs = Long.parseLong(
			props.getProperty("cluster.health.check.interval.ms", "10000") // default 10s
		);

		long healthCheckTimeoutMs = Long.parseLong(
			props.getProperty("cluster.health.check.timeout.ms", "5000") // default 5s
		);

		return new ClusterConfig(
			clusterId,
			clusterRouterPort,
			nodes,
			healthCheckIntervalMs,
			healthCheckTimeoutMs
		);
	}

	private static List<NodeEndpoint> parseNodesEndpointsFromProperites(Properties props) {
		List<NodeEndpoint> parsedNodes = new ArrayList<>();

		// expected format: node.1.id=node1, node.1.uri=http://localhost:9000
        //                  node.2.id=node2, node.2.uri=http://localhost:9001

		int nodeIndex = 0;
		while (true) {
			String nodeIdPropKey = "node." + nodeIndex + ".id";
			String nodeURIPropKey = "node." + nodeIndex + ".uri";

			String nodeId = props.getProperty(nodeIdPropKey);
			String nodeURIStr = props.getProperty(nodeURIPropKey);

			if (nodeId == null || nodeURIStr == null)
				break; // no more nodes

			try {
				URI nodeURI = URI.create(nodeURIStr);

				parsedNodes.add(new NodeEndpoint(nodeId, nodeURI));
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Invalid URI for node " + nodeId + ": " + nodeURIStr, e);
			}

			nodeIndex++;
		}

		if (parsedNodes.isEmpty()) {
			throw new RuntimeException("No noes defined in cluster configuration");
		}

		return parsedNodes;
	}

	public String getClusterId() {
		return clusterId;
	}

	public int getClusterRouterPort() {
		return clusterRouterPort;
	}

	public List<NodeEndpoint> getNodes() {
		return nodes;
	}

	public long getHealthCheckIntervalMs() {
		return healthCheckIntervalMs;
	}

	public long getHealthCheckTimeoutMs() {
		return healthCheckTimeoutMs;
	}
}
