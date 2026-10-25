package team.luxinfine.authlib.boot;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerEntrypoint {
    private static final Logger LOGGER = Logger.getLogger("AuthLib");

    //Пакеты authlib, во всех класслоудерах будут загружаться строго из нашего authlib
    private static final String[] AUTHLIB_PACKAGES = {
            "com/mojang/authlib/",
            "team/luxinfine/rslauncher/",
            "team/luxinfine/authlib/"
    };

    //Точка входа для -javaagent
    //Пример запуска сервера: java -javaagent:authlib-xxx-server.jar -jar servercore.jar
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new AuthlibTransformer());
        LOGGER.info("Authlib Java agent success loaded");
    }

    private static final class AuthlibTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(
                ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] bytecode
        ) {
            if (className == null) return null;
            boolean isAuthlibClass = false;
            for (String packagePrefix : AUTHLIB_PACKAGES) {
                if (className.startsWith(packagePrefix)) {
                    isAuthlibClass = true;
                    break;
                }
            }
            if (!isAuthlibClass) return null;

            String resourceName = className + ".class";
            ClassLoader agentLoader = ServerEntrypoint.class.getClassLoader();
            try (InputStream stream = agentLoader.getResourceAsStream(resourceName)) {
                if (stream == null)
                    return rejectUnbundledClass(className, "bundled class is missing");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) != -1)
                    bytes.write(buffer, 0, count);
                return bytes.toByteArray();
            } catch (Exception e) {
                return rejectUnbundledClass(className, "unable to read bundled class: " + e);
            }
        }

        //Выброс исключения в transform игнорируется JVM и трактуется как "загрузи оригинальный байткод"
        //Потому остается только вывести ошибку в лог и вернуть сломанный байткод (0 байт)
        private static byte[] rejectUnbundledClass(String className, String reason) {
            LOGGER.log(Level.SEVERE, "Refusing to load non-bundled Authlib class " + className + " (" + reason + ")", new RuntimeException("Stack trace"));
            return new byte[0];
        }
    }
}
