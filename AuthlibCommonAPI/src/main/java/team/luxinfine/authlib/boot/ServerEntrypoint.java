package team.luxinfine.authlib.boot;

import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.jar.JarOutputStream;

@Environment(TargetSide.SERVER)
public final class ServerEntrypoint {
    private static final Logger LOGGER = Logger.getLogger("AuthLib");

    //Пакеты authlib, во всех класслоудерах будут загружаться строго из нашего authlib
    private static final String[] AUTHLIB_PACKAGES = {
            "com/mojang/authlib/",
            "team/luxinfine/rslauncher/server/",
            "team/luxinfine/authlib/"
    };

    //Точка входа для -javaagent
    //Пример запуска сервера: java -javaagent:authlib-xxx-server.jar -jar servercore.jar
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        try {
            File agentFile = new File(ServerEntrypoint.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            JarFile agentJar = new JarFile(agentFile);
            injectAuthlibImplToBootClasspath(instrumentation, agentJar);
            instrumentation.addTransformer(new AuthlibTransformer(agentJar));

            //Чтобы API загрузилось через boot загрузчик, используем рефлексию
            Class<?> commonApi = Class.forName("team.luxinfine.authlib.CommonAPI", true, null);
            commonApi.getMethod("init").invoke(null);

            LOGGER.info("Authlib Java agent success loaded from " + agentFile);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("Authlib bootstrap initialization failed", cause);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize the Authlib Java agent", e);
        }
    }

    //Создаем мини-jar, в котором будут ТОЛЬКО НАШИ классы для authlib
    //Этот jar будет подключен в boot загрузчик, который доступен и приоритетен для ВСЕХ класслоудеров независимо от реализации.
    //Полностью подключить туда authlib нельзя, com.mojang.* классы должны оказаться в правильном загрузчике ядра для избежания
    //проблем с подгрузкой классов-зависимостей самой authlib (gson / апач и т.п.)
    private static void injectAuthlibImplToBootClasspath(Instrumentation instrumentation, JarFile agentJar) throws Exception {
        Path tempJar = Files.createTempFile("authlib-bootstrap-", ".jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(tempJar.toFile().toPath()))) {
            Enumeration<JarEntry> entries = agentJar.entries();
            byte[] buffer = new byte[8192];
            while (entries.hasMoreElements()) {
                JarEntry source = entries.nextElement();
                String name = source.getName();

                //Грузим только классы, кроме патченных mojang
                if (!name.endsWith(".class"))
                    continue;
                if (!name.startsWith("team/luxinfine/authlib/") && !name.startsWith("team/luxinfine/rslauncher/"))
                    continue;

                //НИ В КОЕМ СЛУЧАЕ не дублируем сам энтрипоинт, иначе будет краш с зацикливаем агентом
                if (name.startsWith("team/luxinfine/authlib/boot/"))
                    continue;

                output.putNextEntry(new JarEntry(name));
                try (InputStream input = agentJar.getInputStream(source)) {
                    int count;
                    while ((count = input.read(buffer)) != -1)
                        output.write(buffer, 0, count);
                }
                output.closeEntry();
            }
        }

        try (JarFile bootstrapJar = new JarFile(tempJar.toFile())) {
            instrumentation.appendToBootstrapClassLoaderSearch(bootstrapJar);
        }
        tempJar.toFile().deleteOnExit();
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
