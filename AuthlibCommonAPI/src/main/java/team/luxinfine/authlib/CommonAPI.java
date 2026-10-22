package team.luxinfine.authlib;

import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.Invoke;
import team.luxinfine.mcsplitter.api.TargetSide;
import team.luxinfine.rslauncher.client.NativeAPI;
import team.luxinfine.rslauncher.server.ServerAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Основной API лаунчера, доступный с клиента и сервера<HR>
 * Весь mojang api реализуется через делегаты на эти методы
 */
public final class CommonAPI {
    private static final Logger LOGGER = Logger.getLogger("AuthLib");

    /**
     * Инициализация API
     */
    public static void init() {
        Invoke.invokeServer(ServerAPI::init);
    }

    /**
     * Запрос начала входа на сервер
     */
    public static void joinMCServer(@Nonnull String serverID) throws Exception {
        Invoke.invokeClient(() -> NativeAPI.joinMCServer(serverID));
    }

    /**
     * Проверка входа игрока на сервер
     * @return профиль игрока либо null, если сессия невалидна
     */
    @Nullable
    public static GameProfile has_joined(@Nonnull String user, boolean by_uuid, @Nonnull String serverID) {
        return Invoke.invokeServer(() -> ServerAPI.has_joined(user, by_uuid, serverID));
    }

    /**
     * Поиск профилей игроков
     */
    @Nonnull
    public static Map<String, GameProfile> lookupProfiles(@Nonnull List<String> users, boolean by_uuid) {
        return Invoke.invoke(
                () -> ServerAPI.lookupProfiles(users, by_uuid),
                () -> {
                    try {
                        Map<String, Map<String, Object>> raw = NativeAPI.lookupProfiles(users, by_uuid);
                        if (raw.isEmpty())
                            return Collections.emptyMap();
                        Map<String, GameProfile> result = new HashMap<>(raw.size());
                        for (Map.Entry<String, Map<String, Object>> entry : raw.entrySet()) {
                            GameProfile profile = toGameProfile(entry.getValue());
                            if (profile != null)
                                result.put(entry.getKey(), profile);
                        }
                        return result;
                    } catch (TimeoutException | RuntimeException e) {
                        LOGGER.warning("Error while looking up profiles: " + e);
                        return Collections.emptyMap();
                    }
                }
        );
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Environment(TargetSide.CLIENT)
    private static GameProfile toGameProfile(Map<String, Object> map) {
        Object idValue = map.get("id");
        Object nameValue = map.get("name");
        if (idValue == null || nameValue == null)
            return null;

        List<GameProfileProperty> properties = new ArrayList<>();
        Object rawProperties = map.get("properties");
        if (rawProperties instanceof List) {
            for (Object rawProperty : (List<Object>) rawProperties) {
                if (!(rawProperty instanceof Map))
                    continue;
                Map<String, Object> property = (Map<String, Object>) rawProperty;
                Object propertyName = property.get("name");
                Object propertyValue = property.get("value");
                if (propertyName == null || propertyValue == null)
                    continue;
                properties.add(new GameProfileProperty(propertyName.toString(), propertyValue.toString()));
            }
        }

        return new GameProfile(UUID.fromString(idValue.toString()), nameValue.toString(), properties);
    }
}
