package cluster;

import java.net.URI;
import java.util.Objects;

public class NodeEndpoint {
	private final String id;
	private final URI uri;

	public NodeEndpoint(String id, URI uri) {
		if (id == null || uri == null)
			throw new IllegalArgumentException("ID and URI cannot be null");

		this.id = id;
		this.uri = uri;
	}

	public String getId() {
		return id;
	}

	public URI getURI() {
		return uri;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || this.getClass() != other.getClass()) return false;

		NodeEndpoint thatEndpoint = (NodeEndpoint) other;
		return Objects.equals(this.id, thatEndpoint.id) && Objects.equals(this.uri, thatEndpoint.uri);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, uri);
	}

	@Override
	public String toString() {
		return "NodeEndpoint{" +
				"id='" + id + '\'' +
				", uri=" + uri +
				'}';
	}
}
