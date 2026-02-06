package node.service.object.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import node.service.NodeStorageService;

import static logging.AppLogger.log;

import java.io.IOException;
import java.io.InputStream;

public class PutObjectHandler implements HttpHandler {
	private final NodeStorageService storageService;

	public PutObjectHandler(NodeStorageService srv) {
		this.storageService = srv;
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String objectId = ex.getRequestURI().getPath().substring("/objects/".length());

		log.info("PUT object " + objectId);
		try (InputStream in = ex.getRequestBody()) {
			storageService.put(objectId, in);
			ex.sendResponseHeaders(201, -1);
			log.info("PUT object " + objectId + " completed");
		} catch (Exception e) {
			log.severe("PUT object " + objectId + " failed " + e.getMessage());
			ex.sendResponseHeaders(500, -1);
		} finally {
			ex.close();
		}
	}
}
