package team.luxinfine.authlib.boot;

import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Прелоудер клиента<BR>
 * Именно этот класс будет вызывать лаунчер для запуска клиента
 */
@Environment(TargetSide.CLIENT)
public final class LegacyClientEntrypoint {
    private static final Logger LOGGER = Logger.getLogger("AuthLib");
    private static final String MAIN_CLASS_PROP = "LFLauncher.Boot.MainClass";

    public static void main(String[] args) throws Throwable {
        JavaURLHooks.init();

        String bootClass = System.getProperty(MAIN_CLASS_PROP);
        if (bootClass == null) {
            LOGGER.log(Level.SEVERE, "Invalid launcher profile: Main class not specified. Please add jvm arg -D"+MAIN_CLASS_PROP+"=class.name");
            System.exit(1);
            return;
        }
        System.clearProperty(MAIN_CLASS_PROP);

        Class<?> clazz;
        try {
            clazz = Class.forName(bootClass);
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Invalid launcher profile: Main class '" + bootClass + "' not available: " + e);
            System.exit(1);
            return;
        }

        Method entrypoint;
        try {
            entrypoint = clazz.getMethod("main", String[].class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Invalid launcher profile: Main class '" + bootClass + "' not contains main(String[]) method: " + e);
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
}
