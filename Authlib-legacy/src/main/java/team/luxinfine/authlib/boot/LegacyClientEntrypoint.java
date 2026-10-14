package team.luxinfine.authlib.boot;

import team.luxinfine.authlib.redirect.MojangAPIRedirect;
import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Прелоудер игры с установкой URL хука для перенаправления mojang api на api лаунчера<BR>
 * Именно этот класс будет вызывать лаунчер для запуска игры
 */
@Environment(TargetSide.CLIENT)
public final class LegacyClientEntrypoint {
    private static final Logger LOGGER = Logger.getLogger("AuthLib");

    public static void main(String[] args) throws Throwable {
        MojangAPIRedirect.initURLHook();

        String bootClass = System.getProperty("LFLauncher.Boot.MainClass");
        if (bootClass == null) {
            LOGGER.log(Level.SEVERE, "Invalid launcher profile: Main class not specified. Please add jvm arg -DLFLauncher.Boot.MainClass=class.name");
            System.exit(1);
            return;
        }
        System.clearProperty("LFLauncher.Boot.MainClass");

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

        entrypoint.invoke(null, (Object) args);
    }
}
