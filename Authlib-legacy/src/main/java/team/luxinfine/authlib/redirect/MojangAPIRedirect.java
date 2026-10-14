package team.luxinfine.authlib.redirect;

import team.luxinfine.authlib.GameProfile;
import team.luxinfine.authlib.NativeAPI;
import team.luxinfine.authlib.ServerAPI;
import team.luxinfine.mcsplitter.api.Invoke;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.logging.Logger;

public final class MojangAPIRedirect {
    private static final Logger LOGGER = Logger.getLogger("MojangAPIRedirect");

    /**
     * Устанавливает глобальный URL хук для перенаправления mojang API запросов на API лаунчера<HR>
     * Долже быть вызван только один раз
     */
    public static void initURLHook() {
        URL.setURLStreamHandlerFactory(protocol -> {
            //legacy версии игры для mojang api использовали только http протокол
            if (protocol.equals("http"))
                return new HTTPHandler();

            //Остальные протоколы делегируем на дефолтные обработчики
            String name = "sun.net.www.protocol." + protocol + ".Handler";
            try {
                Class<?> c = Class.forName(name);
                return (URLStreamHandler)c.newInstance();
            } catch (Exception e) {
                throw new InternalError("could not load " + protocol + "system protocol handler", e);
            }
        });
        LOGGER.info("Authlib redirects success injected");
    }

    private static class HTTPHandler extends sun.net.www.protocol.http.Handler {
        @Override
        protected URLConnection openConnection(URL u, Proxy p) throws IOException {
            String domain = u.getAuthority();

            switch (domain) {
                case "skins.minecraft.net": {
                    // Скины: http://skins.minecraft.net/MinecraftSkins/${username}.png
                    // Плащи: http://skins.minecraft.net/MinecraftCloaks/${username}.png
                    String path = u.getPath();
                    if (!path.endsWith(".png")) {
                        LOGGER.warning("Try fetch unknown mojang skin url - ignore: " + u.toExternalForm());
                        return super.openConnection(u, p);
                    }
                    path = path.substring(0, path.length() - ".png".length());

                    String username;
                    String property;
                    if (path.startsWith("/MinecraftSkins/")) {
                        username = path.substring("/MinecraftSkins/".length());
                        property = "SKIN";
                    } else if (path.startsWith("/MinecraftCloaks/")) {
                        username = path.substring("/MinecraftCloaks/".length());
                        property = "CLOAK";
                    } else {
                        LOGGER.warning("Try fetch unknown mojang skin url - ignore: " + u.toExternalForm());
                        return super.openConnection(u, p);
                    }

                    return new UserTextureConnection(u, username, property);
                }
                case "session.minecraft.net": {
                    // API авторизации
                    String path = u.getPath();
                    if (path.equals(JoinServerConnection.JOIN_PATH))
                        return new JoinServerConnection(u);
                    if (path.equals(CheckServerConnection.CHECK_PATH))
                        return new CheckServerConnection(u);

                    LOGGER.warning("Try fetch unknown mojang session url - ignore: " + u.toExternalForm());
                    return super.openConnection(u, p);
                }
                case "s3.amazonaws.com": {
                    //Старый майнкрафт докачивал с амазона ассеты. Сейчас их там нет, зато возможны таймауты для РФ...
                    //Потому сразу кидаем 404, это заставит игру прогрузить ресурсы из локальных папок
                    String path = u.getPath();
                    if (path.equals("/MinecraftResources/"))
                        return new NotFoundConnection(u);
                }
            }

            return super.openConnection(u, p);
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return openConnection(u, null);
        }
    }

    static GameProfile lookupProfile(String username) {
        return Invoke.invoke(
            () -> ServerAPI.lookupProfiles(Collections.singletonList(username), false),
            () -> {
                try {
                    return NativeAPI.lookupProfiles(Collections.singletonList(username), false);
                } catch (Exception e) {
                    LOGGER.warning("Error while lookup profiles " + e);
                    return Collections.<String, GameProfile>emptyMap();
                }
            }
        ).get(username);
    }
}
