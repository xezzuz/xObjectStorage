package cluster;

import java.util.ArrayList;
import java.util.List;

import router.RouterServer;
import router.node.client.DefaultHttpNodeClient;
import router.placement.RegularHashingPlacementStrategy;

// import static logging.AppLogger.log;

public class ClusterManager {
	private final ClusterConfig config;
	private final ClusterState state;
	private final HealthMonitor healthMonitor;
	private final RouterServer routerServer;

	public ClusterManager() throws Exception {
		this(ClusterConfig.loadFromPropertiesConfigFile("/resources/cluster.properties"));
	}

	public ClusterManager(ClusterConfig config) throws Exception {
		this.config = config;
		this.state = new ClusterState(createInitialClusterNodes());
		this.healthMonitor = new HealthMonitor(config, state);
		this.healthMonitor.performClusterHealthCheck();
		this.routerServer = new RouterServer(
			config.getClusterRouterPort(),
			new DefaultHttpNodeClient(),
			new RegularHashingPlacementStrategy(config.getNodes())
		);
	}

	public void start() {
		routerServer.start();
	}

	private List<ClusterNode> createInitialClusterNodes() {
		List<ClusterNode> initialNodes = new ArrayList<>();
		for (NodeEndpoint nodeEndpoint : config.getNodes()) {
			ClusterNode clusterNode = new ClusterNode(nodeEndpoint);
			initialNodes.add(clusterNode);
		}
		return initialNodes;
	}
}
