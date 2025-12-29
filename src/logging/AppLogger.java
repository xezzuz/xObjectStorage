package logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class AppLogger {
    public static final Logger log = Logger.getLogger("GLOBAL");

    static {
        // Remove default handlers
        Logger rootLogger = Logger.getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Add console handler with custom Spring Boot-style formatter
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SpringBootStyleFormatter());

        log.addHandler(handler);
        log.setLevel(Level.ALL);
    }

    private static class SpringBootStyleFormatter extends Formatter {
        private static final String RESET = "\u001B[0m";
        private static final String RED = "\u001B[31m";
        private static final String YELLOW = "\u001B[33m";
        private static final String GREEN = "\u001B[32m";
        private static final String CYAN = "\u001B[36m";
        private static final String GRAY = "\u001B[90m";
        private static final String WHITE = "\u001B[97m";

        private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            // Timestamp
            String timestamp = LocalDateTime.now().format(dtf);

            // Color for level
            String color;
            Level level = record.getLevel();
            if (level == Level.SEVERE) color = RED;
            else if (level == Level.WARNING) color = YELLOW;
            else if (level == Level.INFO) color = GREEN;
            else color = CYAN; // FINE/FINER/FINST

            // Thread name in gray
            String threadName = Thread.currentThread().getName();

            // Short class name
            String loggerName = record.getLoggerName();
            if (loggerName.contains(".")) {
                loggerName = loggerName.substring(loggerName.lastIndexOf('.') + 1);
            }

            return String.format(
                "%s%s%s %s [%s%s%s] - %s%n",
                GRAY, timestamp, RESET,            // timestamp in white
                GRAY + threadName + RESET, // gray thread + class name
                color, level.getName(), RESET,      // colored level
                // GRAY + threadName + RESET + " " + loggerName, // gray thread + class name
                formatMessage(record)               // log message
            );
        }
    }
}
