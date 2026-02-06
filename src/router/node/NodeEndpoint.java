package router.node;

import java.net.URI;

public class NodeEndpoint {
	private final String id;
	private final URI baseURI;

	public NodeEndpoint(String id, URI baseURI) {
		this.id = id;
		this.baseURI = baseURI;
	}

	public String getId() {
		return this.id;
	}

	public URI getURI() {
		return this.baseURI;
	}
}
