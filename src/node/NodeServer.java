package node;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static logging.AppLogger.log;

public class NodeServer {
	private final HttpServer server;
	private final NodeStorageService storageService; // needless for now

	public NodeServer(int port, NodeStorageService storageService) throws Exception {
		this.storageService = storageService;
		this.server = HttpServer.create(new InetSocketAddress(port), 0);

		server.createContext("/objects", new ObjectHandler(storageService));
		server.createContext("/health", exchange -> {
			OutputStream body = exchange.getResponseBody();
			exchange.sendResponseHeaders(200, "OK".length());
			body.write("OK".getBytes());
			exchange.close();
		});

		server.setExecutor(null); // default executor
	}

	public void start() {
		log.info("Starting NodeServer...");
		server.start();
		log.info("NodeServer is listening on port " + server.getAddress().getPort() + "...");
	}

	public void stop() {
		log.info("Shutting down NodeServer...");
		server.stop(0);
		log.info("NodeServer stopped");
	}
}
