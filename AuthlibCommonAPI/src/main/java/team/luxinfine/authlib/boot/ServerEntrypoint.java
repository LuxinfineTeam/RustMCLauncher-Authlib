package team.luxinfine.authlib.boot;

import team.luxinfine.authlib.CommonAPI;
import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Прелоудер сервера<BR>
 * Именно этот класс будет вызывать лаунчер для запуска ядра сервера<HR>
 * Доступно 2 варианта запуска:<BR>
 * 1. Запуск через {@code java -DLFLauncher.Boot.Core=serverCore.jar -jar authlib.jar}. Этот способ подходит в случаях, когда не требуется указывать вручную -cp<BR>
 * 2. Запуск через {@code java -DLFLauncher.Boot.MainClass=my.class.Name -cp authlib.jar:coreName.jar:libraries/* team.luxinfine.authlib.boot.ServerEntrypoint}. Этот способ используется при запуске ядра с ручным указанием класспаза через -cp.<HR>
 * ВНИМАНИЕ: при 2 способе запуска на современных java могут потребоваться флаги --add-opens / --add-exports и т.д. в строке запуска<BR><BR>
 * ВНИМАНИЕ: Для запуска старых версий игры (1.7.5 и ниже) дотупен ТОЛЬКО ПЕРВЫЙ способ запуска (java -jar)! При запуске через -cp не установится URL хук и работать не будет!<BR><BR>
 * Первый вариант запуска поддерживает опционально {@code -DLFLauncher.Boot.ExtraClassPath=classpath}. Значение в том же формате, как и для -cp, поддерживается *, найденные .jar будут подключены в classpath сервера
 */
@Environment(TargetSide.SERVER)
public final class ServerEntrypoint {
    private static final Logger LOGGER = Logger.getLogger("AuthLib");
    private static final MethodHandle FIND_LOADED_CLASS;
    private static final MethodHandle URL_CLASS_LOADER_FIND_CLASS;

    static {
        try {
            Method findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            findLoadedClass.setAccessible(true);
            FIND_LOADED_CLASS = MethodHandles.lookup().unreflect(findLoadedClass)
                    .asType(MethodType.methodType(Class.class, ClassLoader.class, String.class));

            Method findClass = URLClassLoader.class.getDeclaredMethod("findClass", String.class);
            findClass.setAccessible(true);
            URL_CLASS_LOADER_FIND_CLASS = MethodHandles.lookup().unreflect(findClass)
                    .asType(MethodType.methodType(Class.class, URLClassLoader.class, String.class));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final String PROP_CORE = "LFLauncher.Boot.Core";
    private static final String PROP_MAIN_CLASS = "LFLauncher.Boot.MainClass";
    private static final String PROP_EXTRA_CLASSPATH = "LFLauncher.Boot.ExtraClassPath";

    public static void main(String[] args) throws Throwable {
        CommonAPI.init();
        initURLHooksIfNeeded();

        String corePath = System.getProperty(PROP_CORE);
        String mainClass = System.getProperty(PROP_MAIN_CLASS);
        String extraCP = System.getProperty(PROP_EXTRA_CLASSPATH);

        if (isNotEmpty(corePath)) {
            //Обычный запуск (java -jar)
            System.clearProperty(PROP_CORE);
            System.clearProperty(PROP_MAIN_CLASS);
            System.clearProperty(PROP_EXTRA_CLASSPATH);
            launchCore(Paths.get(corePath), mainClass, extraCP, args);
        } else if (isNotEmpty(mainClass)) {
            //Запуск через -cp
            System.clearProperty(PROP_MAIN_CLASS);
            if (extraCP != null) {
                LOGGER.log(Level.SEVERE, "Property -D" + PROP_EXTRA_CLASSPATH + " not compatible with -cp");
                System.exit(1);
            }
            launchMainClass(ServerEntrypoint.class.getClassLoader(), mainClass, args);
        } else {
            LOGGER.log(Level.SEVERE, "Invalid server start command: Core path not specified. Please add jvm arg -D" + PROP_CORE + "=coreFilename.jar or -D" + PROP_MAIN_CLASS + "=class.name");
            System.exit(1);
        }
    }

    //В некоторых старых версиях игры нужно установить глобальный URL хук
    private static void initURLHooksIfNeeded() throws Throwable {
        Class<?> clazz;
        try {
            clazz = Class.forName("team.luxinfine.authlib.boot.JavaURLHooks");
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

    private static void launchCore(@Nonnull Path corePath, @Nonnull String mainClass, @Nullable String extraCP, @Nonnull String[] args) throws Throwable {
        Path absoluteCore = corePath.toAbsolutePath();
        if (!Files.isRegularFile(absoluteCore)) {
            LOGGER.log(Level.SEVERE, "Invalid server start command: Core path invalid - file not found: " + corePath);
            System.exit(1);
            return;
        }

        Manifest manifest;
        try (JarFile jar = new JarFile(absoluteCore.toFile())) {
            manifest = jar.getManifest();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Invalid server start command: Unable to read core manifest '" + corePath + "': " + e);
            System.exit(1);
            return;
        }

        String entrypoint = mainClass;
        if (!isNotEmpty(entrypoint) && manifest != null)
            entrypoint = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);

        if (!isNotEmpty(entrypoint)) {
            LOGGER.log(Level.SEVERE, "Invalid server start command: Core main class not found. Please add jvm arg -D" + PROP_MAIN_CLASS + "=class.name");
            System.exit(1);
            return;
        }

        List<URL> classpath;
        try {
            classpath = resolveClasspath(absoluteCore, manifest);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Invalid server start command: Unable to build core classpath: " + e);
            System.exit(1);
            return;
        }

        if (isNotEmpty(extraCP))
            classpath.addAll(resolveExtraClasspath(extraCP));

        URLClassLoader loader = createCoreClassLoader(classpath);
        Thread.currentThread().setContextClassLoader(loader);
        launchMainClass(loader, entrypoint, args);
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

    //Резолвит класспаз из экстра аргумента
    private static List<URL> resolveExtraClasspath(@Nonnull String extraCP) {
        List<URL> urls = new ArrayList<>();

        for (String entry : extraCP.split(Pattern.quote(File.pathSeparator))) {
            entry = entry.trim();
            if (entry.isEmpty())
                continue;

            try {
                boolean wildcard = entry.endsWith("*");
                String pathString = wildcard ? entry.substring(0, entry.length() - 1) : entry;

                Path path = Paths.get(pathString.isEmpty() ? "." : pathString)
                        .toAbsolutePath()
                        .normalize();

                if (wildcard) {
                    if (!Files.isDirectory(path)) {
                        LOGGER.warning("Classpath wildcard directory not found: " + path);
                        continue;
                    }

                    try (Stream<Path> files = Files.list(path)) {
                        files.filter(Files::isRegularFile)
                                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                                .sorted()
                                .forEach(p -> {
                                    try {
                                        urls.add(p.toUri().toURL());
                                    } catch (Exception e) {
                                        throw new RuntimeException("Unable to add classpath entry: " + p, e);
                                    }
                                });
                    }
                } else if (Files.exists(path)) {
                    urls.add(path.toUri().toURL());
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid classpath entry '" + entry + "'", e);
            }
        }

        return urls;
    }

    //Собираем класспаз из ядра и прописанных файлов в манифесте
    private static List<URL> resolveClasspath(@Nonnull Path corePath, @Nullable Manifest manifest) throws IOException {
        List<URL> urls = new ArrayList<>();
        URI coreUri = corePath.toUri();
        urls.add(coreUri.toURL());

        if (manifest != null) {
            String classPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (classPath != null) {
                for (String entry : classPath.split("\\s+")) {
                    if (entry.isEmpty())
                        continue;
                    try {
                        urls.add(coreUri.resolve(entry).toURL());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Skip malformed Class-Path entry '" + entry + "' in " + corePath + ": " + e);
                    }
                }
            }
        }

        return urls;
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    //Создание специального загрузчика классов для java -jar authlib.jar запуска
    //Этот загрузчик будет отдавать приоритетно классы authlib из нашего jar, вместо класспаза ядра
    private static URLClassLoader createCoreClassLoader(@Nonnull List<URL> coreUrls) {
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        RuntimeBridgeClassLoader bridge = new RuntimeBridgeClassLoader(systemLoader);
        URL authlibUrl = ServerEntrypoint.class.getProtectionDomain().getCodeSource().getLocation();
        AuthlibClassLoader authlibLoader = new AuthlibClassLoader(new URL[]{authlibUrl}, bridge);
        bridge.setAuthlibLoader(authlibLoader);
        URLClassLoader coreLoader = new URLClassLoader(coreUrls.toArray(new URL[0]), bridge);
        bridge.setCoreLoader(coreLoader);
        return coreLoader;
    }

    //Специальный класслоудер, который будет родителем класслоудера ядра.
    //Здесь реализованы трюки для перехвата загрузки authlib классов и подмены на наши, вместо тех, что в ядре
    private static final class RuntimeBridgeClassLoader extends ClassLoader {
        private final ClassLoader systemLoader;
        private volatile AuthlibClassLoader authlibLoader;
        private volatile URLClassLoader coreLoader;

        private RuntimeBridgeClassLoader(ClassLoader systemLoader) {
            super(null);
            this.systemLoader = systemLoader;
        }

        private void setAuthlibLoader(AuthlibClassLoader loader) {
            this.authlibLoader = loader;
        }

        private void setCoreLoader(URLClassLoader loader) {
            this.coreLoader = loader;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null)
                    return loaded;

                if (name.startsWith("com.mojang.authlib.")) {
                    try {
                        return authlibLoader.loadAuthlibClass(name);
                    } catch (ClassNotFoundException missingFromLauncherAuthlib) {
                        ClassLoader runtimeLoader = Thread.currentThread().getContextClassLoader();
                        if (runtimeLoader != this && isPlainUrlClassLoader(runtimeLoader)) {
                            try {
                                return findInUrlLoader((URLClassLoader) runtimeLoader, name);
                            } catch (ClassNotFoundException ignored) {}
                        }
                        throw missingFromLauncherAuthlib;
                    }
                }
                if (name.startsWith("team.luxinfine.authlib."))
                    return systemLoader.loadClass(name);

                ClassLoader runtimeLoader = Thread.currentThread().getContextClassLoader();
                if (runtimeLoader != this && isPlainUrlClassLoader(runtimeLoader)) {
                    try {
                        return findInUrlLoader((URLClassLoader) runtimeLoader, name);
                    } catch (ClassNotFoundException ignored) {}
                }

                try {
                    return systemLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    if (coreLoader != null && coreLoader != runtimeLoader) {
                        try {
                            return findInUrlLoader(coreLoader, name);
                        } catch (ClassNotFoundException ignoredCore) {}
                    }
                    throw new ClassNotFoundException(name);
                }
            }
        }
    }

    private static final class AuthlibClassLoader extends URLClassLoader {
        private AuthlibClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        private Class<?> loadAuthlibClass(String name) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null)
                    return loaded;
                return findClass(name);
            }
        }
    }

    private static Class<?> findInUrlLoader(URLClassLoader loader, String name) throws ClassNotFoundException {
        try {
            Class<?> loaded = (Class<?>) FIND_LOADED_CLASS.invokeExact((ClassLoader) loader, name);
            if (loaded != null)
                return loaded;

            return (Class<?>) URL_CLASS_LOADER_FIND_CLASS.invokeExact(loader, name);
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to inspect runtime classloader. The JVM must open java.base/java.lang and java.base/java.net to unnamed modules.", e);
        }
    }

    private static boolean isPlainUrlClassLoader(ClassLoader loader) {
        return loader != null && loader.getClass() == URLClassLoader.class;
    }
}
