package node.service.object.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import node.service.NodeStorageService;

import static logging.AppLogger.log;

public class GetObjectHandler implements HttpHandler {
	private final NodeStorageService storageService;

	public GetObjectHandler(NodeStorageService srv) {
		this.storageService = srv;
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String objectId = ex.getRequestURI().getPath().substring("/objects/".length());

		log.info("GET object " + objectId);
		try (InputStream in = storageService.get(objectId)) {
			ex.sendResponseHeaders(200, 0);
			in.transferTo(ex.getResponseBody());
			log.info("GET object " + objectId + " completed");
		} catch (FileNotFoundException e) {
			log.warning("GET object " + objectId + " not found");
			ex.sendResponseHeaders(404, -1);
		} catch (Exception e) {
			log.severe("GET object " + objectId + " failed " + e.getMessage());
			ex.sendResponseHeaders(500, -1);
		} finally {
			ex.close();
		}
	}
}
