package cluster;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static logging.AppLogger.log;

public class HealthMonitor {
	private final ClusterConfig config;
	private final ClusterState state;

	public HealthMonitor(ClusterConfig config, ClusterState state) {
		this.config = config;
		this.state = state;
	}

	public void start() {}
	public void stop() {}

	public void performClusterHealthCheck() {
		log.info("Starting health checks for " + config.getNodes().size() + " nodes of " + config.getClusterId());

		for (NodeEndpoint nodeEndpoint : config.getNodes()) {
			try {
				log.info("Checking node " + nodeEndpoint.getId() + " health status...");
				boolean isHealthy = isServerHealthy(nodeEndpoint);

				state.updateNodeStatus(nodeEndpoint.getId(), isHealthy
					? NodeStatus.HEALTHY
					: NodeStatus.UNHEALTHY);

				log.info("Health status of node " + nodeEndpoint.getId() + ": " + (isHealthy
				? NodeStatus.HEALTHY.name()
				: NodeStatus.UNHEALTHY.name()));
			} catch (Exception e) {
				log.warning("Failed to check health status of node " + nodeEndpoint.getId() + ": " + e.getMessage());

				state.updateNodeStatus(nodeEndpoint.getId(), NodeStatus.UNHEALTHY);
			}
		}

		log.info("Completed health checks for " + config.getNodes().size() + " nodes of " + config.getClusterId());
		log.info(state.getClusterStateSummary());
	}

	public boolean isServerHealthy(NodeEndpoint nodeEndpoint) {
		URI healthURI = nodeEndpoint.getURI().resolve("/health");

		try {
			HttpClient client = HttpClient.newBuilder()
								.connectTimeout(Duration.ofMillis(config.getHealthCheckTimeoutMs()))
								.build();

			HttpRequest healthCheckRequest = HttpRequest.newBuilder()
												.GET()
												.uri(healthURI)
												.timeout(Duration.ofMillis(config.getHealthCheckTimeoutMs()))
												.build();

			HttpResponse<Void> response = client.send(healthCheckRequest, HttpResponse.BodyHandlers.discarding());

			return response.statusCode() == 200;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
