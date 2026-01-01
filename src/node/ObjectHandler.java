package node;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static logging.AppLogger.log;

public class ObjectHandler implements HttpHandler {
	private final NodeStorageService storageService;

	public ObjectHandler(NodeStorageService storageService) {
		this.storageService = storageService;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		String path = exchange.getRequestURI().getPath();
		String objectId = path.substring("/object/".length());

		log.info("Incoming request " + method + " " + path);

		switch (exchange.getRequestMethod()) {
			case "GET" -> handleGet(exchange, objectId);
			case "PUT" -> handlePut(exchange, objectId);

			default -> exchange.sendResponseHeaders(405, -1);
		}
	}

	private void handleGet(HttpExchange ex, String id) throws IOException {
		log.info("GET object " + id);
		try (InputStream in = storageService.get(id)) {
			ex.sendResponseHeaders(200, 0);
			in.transferTo(ex.getResponseBody());
			log.info("GET object " + id + " completed");
		} catch (FileNotFoundException e) {
			log.warning("GET object " + id + " not found");
			ex.sendResponseHeaders(404, -1);
		} catch (Exception e) {
			log.severe("GET object " + id + " failed " + e.getMessage());
			ex.sendResponseHeaders(500, -1);
		} finally {
			ex.close();
		}

	}

	private void handlePut(HttpExchange ex, String id) throws IOException {
		log.info("PUT object " + id);
		try (InputStream in = ex.getRequestBody()) {
			storageService.put(id, in);
			ex.sendResponseHeaders(201, -1);
			log.info("PUT object " + id + " completed");
		} catch (Exception e) {
			log.severe("PUT object " + id + " failed " + e.getMessage());
			ex.sendResponseHeaders(500, -1);
		} finally {
			ex.close();
		}
	}
}
