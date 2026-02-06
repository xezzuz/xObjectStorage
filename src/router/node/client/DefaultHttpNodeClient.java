package router.node.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import router.node.NodeClient;
// import router.node.NodeEndpoint;
import cluster.NodeEndpoint;

public final class DefaultHttpNodeClient implements NodeClient {
	private final HttpClient client;

	public DefaultHttpNodeClient() {
		this.client = HttpClient.newBuilder().build();
	}

	@Override
	public HttpResponse<InputStream> put(NodeEndpoint node, String objectId, InputStream data) throws IOException, Exception {
		String objectUri = node.getURI() + "/objects/" + objectId;

		// build the http request
		HttpRequest req = HttpRequest.newBuilder()
							.PUT(HttpRequest.BodyPublishers.ofInputStream(() -> data))
							.uri(new URI(objectUri))
							.build();

		HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
		return res;
	}

	@Override
	public HttpResponse<InputStream> get(NodeEndpoint node, String objectId) throws IOException, Exception {
		String objectUri = node.getURI() + "/objects/" + objectId;


		// build the http request
		HttpRequest req = HttpRequest.newBuilder()
							.GET()
							.uri(new URI(objectUri))
							.build();

		HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());

		return res;
	}

}
