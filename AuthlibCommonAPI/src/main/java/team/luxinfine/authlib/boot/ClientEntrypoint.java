package team.luxinfine.authlib.boot;

import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Универсальная точка входа клиента. Лаунчер загружает её системным загрузчиком
 * до vanilla-клиента или загрузчика модификаций. Реальный главный класс игры
 * передаётся через {@code -DLFLauncher.Boot.MainClass=<class.name>}.
 * <BR><BR>
 * Нативный API лаунчера ({@code team.luxinfine.rslauncher.client.NativeAPI}) живёт в
 * bootstrap-загрузчике: лаунчер сам определяет его (JNI {@code DefineClass}) и
 * регистрирует нативы ДО вызова этого класса. Пакет {@code team.luxinfine.rslauncher}
 * не объявлен ни одним модулем игрового слоя, поэтому игровые загрузчики всегда
 * делегируют его родителю — никакой подмены classpath не требуется.
 */
@Environment(TargetSide.CLIENT)
public final class ClientEntrypoint {
    private static final Logger LOGGER = Logger.getLogger("AuthLib");

    private static final String PROP_MAIN_CLASS = "LFLauncher.Boot.MainClass";
    private static final String PROP_LOG_LEVEL = "LFLauncher.LogLevel";

    public static void main(String[] args) throws Throwable {
        configureLogging();
        initURLHooksIfNeeded();

        String mainClass = System.getProperty(PROP_MAIN_CLASS);
        if (!isNotEmpty(mainClass)) {
            LOGGER.log(Level.SEVERE, "Invalid client start: Main class not specified. Please add jvm arg -D" + PROP_MAIN_CLASS + "=class.name");
            System.exit(1);
        }
        System.clearProperty(PROP_MAIN_CLASS);
        launchMainClass(ClientEntrypoint.class.getClassLoader(), mainClass, args);
    }

    //Настройка дефолтного логгера JUL и уровня логгирования в в log4j
    //Может быть изменена в рантайме конфигурациями log4j.xml
    private static void configureLogging() {
        String configuredLevel = System.getProperty(PROP_LOG_LEVEL, "INFO").trim().toUpperCase();

        Logger root = Logger.getLogger("");
        for (Handler existing : root.getHandlers()) {
            root.removeHandler(existing);
            existing.close();
        }

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new java.util.logging.Formatter() {
            private final ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal.withInitial(
                    () -> new SimpleDateFormat("dd.MM.yyyy|HH:mm:ss"));

            @Override
            public String format(LogRecord record) {
                StringBuilder output = new StringBuilder()
                        .append('[').append(dateFormat.get().format(new Date(record.getMillis()))).append("] [")
                        .append(record.getLevel().getName()).append("] ").append(formatMessage(record)).append(System.lineSeparator());
                if (record.getThrown() != null) {
                    StringWriter trace = new StringWriter();
                    record.getThrown().printStackTrace(new PrintWriter(trace));
                    output.append(trace);
                }
                return output.toString();
            }
        });
        root.addHandler(handler);

        switch (configuredLevel) {
            case "TRACE":
                root.setLevel(Level.FINEST);
                System.setProperty("org.apache.logging.log4j.level", "TRACE");
                System.setProperty("log4j2.level", "TRACE");
                break;
            case "DEBUG":
                root.setLevel(Level.FINE);
                System.setProperty("org.apache.logging.log4j.level", "DEBUG");
                System.setProperty("log4j2.level", "DEBUG");
                break;
            case "INFO":
                root.setLevel(Level.INFO);
                System.setProperty("org.apache.logging.log4j.level", "INFO");
                System.setProperty("log4j2.level", "INFO");
                break;
            case "WARN":
                root.setLevel(Level.WARNING);
                System.setProperty("org.apache.logging.log4j.level", "WARN");
                System.setProperty("log4j2.level", "WARN");
                break;
            case "ERROR":
                root.setLevel(Level.SEVERE);
                System.setProperty("org.apache.logging.log4j.level", "ERROR");
                System.setProperty("log4j2.level", "ERROR");
                break;
        }
    }

    //В некоторых старых версиях игры нужно установить глобальный URL хук
    private static void initURLHooksIfNeeded() throws Throwable {
        Class<?> clazz;
        try {
            clazz = Class.forName("team.luxinfine.authlib.redirect.JavaURLHooks");
        } catch (ClassNotFoundException noClass) {
            //Класса нет - значит хук не нужен, всё ок
            return;
        }

        try {
            clazz.getMethod("init").invoke(null);
        } catch (InvocationTargetException callError) {
            throw callError.getCause() != null ? callError.getCause() : callError;
        }

        LOGGER.info("Mojang API URL redirect success installed");
    }

    private static void launchMainClass(@Nonnull ClassLoader loader, @Nonnull String className, @Nonnull String[] args) throws Throwable {
        Class<?> clazz;
        try {
            clazz = Class.forName(className, true, loader);
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Invalid server start command: Main class '" + className + "' not available: " + e);
            System.exit(1);
            return;
        }

        Method entrypoint;
        try {
            entrypoint = clazz.getMethod("main", String[].class);
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Invalid server start command: Main class '" + className + "' does not contain main(String[]) method: " + e);
            System.exit(1);
            return;
        }

        try {
            entrypoint.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw cause != null ? cause : e;
        }
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
