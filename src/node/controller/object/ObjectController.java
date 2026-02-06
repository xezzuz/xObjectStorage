package node.controller.object;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static logging.AppLogger.log;

import node.controller.object.handlers.GetObjectHandler;
import node.controller.object.handlers.PutObjectHandler;
import node.service.NodeStorageService;

public class ObjectController implements HttpHandler {
	private final NodeStorageService storageService;
	private final GetObjectHandler getHandler;
	private final PutObjectHandler putHandler;

	public ObjectController(NodeStorageService storageService) {
		this.storageService = storageService;
		this.getHandler = new GetObjectHandler(storageService);
		this.putHandler = new PutObjectHandler(storageService);
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String method = ex.getRequestMethod();
		String path = ex.getRequestURI().getPath();

		log.info("Incoming request " + method + " " + path);

		switch (method) {
			case "GET":
				this.getHandler.handle(ex);
				break;
			case "PUT":
				this.putHandler.handle(ex);
				break;
			default:
				ex.sendResponseHeaders(405, -1);
				break;
		}
	}
}
