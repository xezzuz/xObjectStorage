package cluster;

import java.time.Instant;

public final class ClusterNode {
	private final NodeEndpoint endpoint;
	private NodeStatus status;
	private Instant lastHealthCheck;
	private Instant lastStatusChange;

	public ClusterNode(NodeEndpoint endpoint) {
		this.endpoint = endpoint;
		this.status = NodeStatus.UNKNOWN;
		this.lastHealthCheck = Instant.EPOCH;
		this.lastStatusChange = Instant.now();
	}

	public NodeEndpoint getEndpoint() {
		return endpoint;
	}

	public NodeStatus getStatus() {
		return status;
	}

	public Instant getLastHealthCheck() {
		return lastHealthCheck;
	}

	public Instant getLastStatusChange() {
		return lastStatusChange;
	}

	public void updateStatus(NodeStatus newStatus) {
		this.lastHealthCheck = Instant.now();
		this.lastStatusChange = newStatus != status ? Instant.now() : lastStatusChange;
		this.status = newStatus;
	}

	@Override
	public int hashCode() {
		return endpoint.hashCode();
	}

	@Override
	public String toString() {
		return "ClusterNode{" +
				"endpoint=" + endpoint +
				", status=" + status +
				", lastHealthCheck=" + lastHealthCheck +
				", lastStatusChange=" + lastStatusChange +
				'}';
	}
}
