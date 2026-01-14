package auto.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtil {
    private LogUtil() {
    }

    private static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static Logger getLogger() {
        Class<?> callerClass = WALKER.walk(stream ->
                        stream
                                .filter(f -> !f.getClassName().equals(LogUtil.class.getName()))
                                .findFirst()
                                .map(StackWalker.StackFrame::getDeclaringClass)
                                .get()
                //.orElse(LogUtil.class)
        );
        return LogManager.getLogger(callerClass);
    }

    public static void info(String message) {
        getLogger().info(message);
    }

    public static void warn(String message) {
        getLogger().warn(message);
    }

    public static void error(String message) {
        getLogger().error(message);
    }

    public static void error(String message, Throwable t) {
        getLogger().error(message, t);
    }

    public static void debug(String message) {
        getLogger().debug(message);
    }

    public static void fatal(String message) {
        getLogger().fatal(message);
    }

}
