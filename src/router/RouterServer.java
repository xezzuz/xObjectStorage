package router;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import static logging.AppLogger.log;

import router.node.NodeClient;
import cluster.NodeEndpoint;
import router.placement.PlacementStrategy;

public class RouterServer {
	private final PlacementStrategy placement;
	private final NodeClient nodeClient;
	private final HttpServer server;

	public RouterServer(int port, NodeClient nodeClient, PlacementStrategy placement) throws IOException {
		this.nodeClient = nodeClient;
		this.placement = placement;

		this.server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/objects", new ObjectProxyHandler());
		server.createContext("/health", new HttpHandler() {
			public void handle(HttpExchange ex) throws IOException {
				ex.sendResponseHeaders(200, "OK".length());
				ex.getResponseBody().write("OK".getBytes());
				ex.close();
			}
		});
	}

	public void start() {
		log.info("Starting RouterServer...");
		server.start();
		log.info("RouterServer is listening on port " + server.getAddress().getPort() + "...");
	}

	public void stop() {
		log.info("Shutting down RouterServer...");
		server.stop(0);
		log.info("RouterServer stopped");
	}

	private class ObjectProxyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			String method = ex.getRequestMethod();
			String path = ex.getRequestURI().getPath();
			String objectId = path.substring("/objects/".length());

			NodeEndpoint reponsibleNode = placement.place(objectId);

			try {
				switch (method) {
					case "GET":
						forwardGet(ex, reponsibleNode, objectId);
						break;
					case "PUT":
						forwardPut(ex, reponsibleNode, objectId);
						break;
					default:
						ex.sendResponseHeaders(405, -1);
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e);
			}
		}

		public void forwardGet(HttpExchange ex, NodeEndpoint node, String objectId) throws IOException, Exception {
			var res = nodeClient.get(node, objectId);

			ex.sendResponseHeaders(res.statusCode(), 0);

			res.body().transferTo(ex.getResponseBody());
		}

		public void forwardPut(HttpExchange ex, NodeEndpoint node, String objectId) throws IOException, Exception {
			var res = nodeClient.put(node, objectId, ex.getRequestBody());

			ex.sendResponseHeaders(res.statusCode(), -1);
		}
	}
}
