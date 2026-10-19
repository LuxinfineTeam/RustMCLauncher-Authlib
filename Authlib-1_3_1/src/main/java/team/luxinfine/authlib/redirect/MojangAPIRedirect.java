package team.luxinfine.authlib.redirect;

import team.luxinfine.authlib.GameProfile;
import team.luxinfine.authlib.NativeAPI;
import team.luxinfine.authlib.ServerAPI;
import team.luxinfine.mcsplitter.api.Invoke;

import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.logging.Logger;

public final class MojangAPIRedirect {
    private static final Logger LOGGER = Logger.getLogger("MojangAPIRedirect");

    /**
     * Пробует перехватить запрос к mojang api и вернуть синтетический ответ.<HR>
     * Возвращаемое значение {@code null} означает, что запрос не относится к перехватываемым
     * и должен быть выполнен штатным http-обработчиком.
     *
     * @param u запрашиваемый url
     * @return готовое соединение с ответом лаунчера, либо {@code null}
     */
    public static URLConnection hookMinecraftHttpAPI(URL u) {
        String domain = u.getAuthority();

        switch (domain) {
            case "skins.minecraft.net": {
                // Скины: http://skins.minecraft.net/MinecraftSkins/${username}.png
                // Плащи: http://skins.minecraft.net/MinecraftCloaks/${username}.png
                String path = u.getPath();
                if (!path.endsWith(".png")) {
                    LOGGER.warning("Try fetch unknown mojang skin url - ignore: " + u.toExternalForm());
                    return null;
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
                    return null;
                }

                return new UserTextureConnection(u, username, property);
            }
            case "s3.amazonaws.com": {
                //Старый майнкрафт докачивал с амазона ассеты. Сейчас их там нет, зато возможны таймауты для РФ...
                //Потому сразу кидаем 404, это заставит игру прогрузить ресурсы из локальных папок
                String path = u.getPath();
                if (path.equals("/MinecraftResources/"))
                    return new NotFoundConnection(u);
                return null;
            }
        }

        return null;
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
