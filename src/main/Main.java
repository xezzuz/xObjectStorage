package main;

import engine.*;
import node.NodeServer;
import node.ObjectIndex;
import node.service.NodeStorageService;

import static logging.AppLogger.log;

import cluster.ClusterManager;

public class Main {
	public static void main(String[] args) {
		runNodeServer(3000);
	}

	public static void runClusterServer() {
		log.info("---------------- xClusterServer ----------------");

		try {
			ClusterManager server = new ClusterManager();

			server.start();
		} catch (Exception e) {
			log.severe("Exception occurred: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void runNodeServer(int port) {
		log.info("---------------- xNodeServer ----------------");

		try {
			StorageEngine se = new StorageEngine();
			ObjectIndex idx = new ObjectIndex();
			NodeStorageService storageService = new NodeStorageService(se, idx);
			NodeServer server = new NodeServer(port, storageService);

			server.start();
		} catch (Exception e) {
			log.severe("Exception occurred: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
