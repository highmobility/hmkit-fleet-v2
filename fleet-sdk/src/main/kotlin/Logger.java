import org.slf4j.LoggerFactory;

public class Logger {
    private static org.slf4j.Logger logger = null;

    public static org.slf4j.Logger getLogger() {
        if (logger == null) logger = LoggerFactory.getLogger(Logger.class);
        return logger;
    }
}
