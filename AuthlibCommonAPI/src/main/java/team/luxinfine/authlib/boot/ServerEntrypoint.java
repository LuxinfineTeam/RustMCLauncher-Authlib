package team.luxinfine.authlib.boot;

import team.luxinfine.authlib.CommonAPI;
import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

@Environment(TargetSide.SERVER)
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
        try {
            URL agentLocation = ServerEntrypoint.class.getProtectionDomain().getCodeSource().getLocation();
            JarFile agentJar = new JarFile(new File(agentLocation.toURI()));
            instrumentation.addTransformer(new AuthlibTransformer(agentJar));
            LOGGER.info("Authlib Java agent success loaded from " + agentLocation);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to open the Authlib agent jar", e);
            System.exit(1);
        }

        CommonAPI.init();
    }

    private static final class AuthlibTransformer implements ClassFileTransformer {
        private final JarFile agentJar;

        private AuthlibTransformer(JarFile agentJar) {
            this.agentJar = agentJar;
        }

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
            try {
                JarEntry entry = agentJar.getJarEntry(resourceName);
                if (entry == null)
                    return rejectUnbundledClass(className, "bundled class is missing");
                try (InputStream stream = agentJar.getInputStream(entry)) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = stream.read(buffer)) != -1)
                        bytes.write(buffer, 0, count);
                    return bytes.toByteArray();
                }
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
