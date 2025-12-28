package main;

import engine.*;
import objectabstraction.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static logging.AppLogger.log;

public class Main {
	public static void main(String[] args) {
		log.info("---------------- xObjectStorage ----------------");

		try {
			StorageEngine se = new StorageEngine();

			ObjectLocation obj1 = se.write(new FileInputStream("resources/static/atadamano-ma3ak.png"));
			ObjectLocation obj2 = se.write(new FileInputStream("resources/static/file.txt"));
			ObjectLocation obj3 = se.write(new FileInputStream("resources/static/cih.pdf"));

			new FileOutputStream(Path.of("out", "obj1.png").toFile()).write(se.read(obj1).readAllBytes());
			new FileOutputStream(Path.of("out", "obj2.pdf").toFile()).write(se.read(obj2).readAllBytes());
			new FileOutputStream(Path.of("out", "obj3.pdf").toFile()).write(se.read(obj3).readAllBytes());
		} catch (Exception e) {
			log.severe("Exception occurred: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
