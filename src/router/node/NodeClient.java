package router.node;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;

import cluster.NodeEndpoint;

public interface NodeClient {
	public HttpResponse<InputStream> put(NodeEndpoint node, String objectId, InputStream data) throws IOException, Exception;
	public HttpResponse<InputStream> get(NodeEndpoint node, String objectId) throws IOException, Exception;
}
