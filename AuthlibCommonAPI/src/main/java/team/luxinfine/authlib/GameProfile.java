package team.luxinfine.authlib;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class GameProfile {
    @Nonnull
    public UUID id;
    @Nonnull
    public String name;
    //На данный момент здесь могут быть:
    // SKIN -> ссылка на png
    // CAPE -> ссылка на png
    // ELYTRA -> ссылка на png
    @Nonnull
    public Map<String, String> properties;

    public GameProfile(@Nonnull UUID id, @Nonnull String name, @Nonnull Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.properties = properties;
    }

    //Запаковывает полученные параметры в формат mojang
    @Nonnull
    public Map<String, String> toMojangProperties() {
        Map<String, String> result = new LinkedHashMap<>();
        if (properties.containsKey("SKIN") || properties.containsKey("CAPE") || properties.containsKey("ELYTRA")) {
            StringBuilder textures = new StringBuilder("{");
            textures.append("\"timestamp\":").append(System.currentTimeMillis()).append(',');
            textures.append("\"profileId\":").append(SimpleJson.quote(id.toString().replace("-", ""))).append(',');
            textures.append("\"profileName\":").append(SimpleJson.quote(name)).append(',');
            textures.append("\"signatureRequired\":false,\"textures\":{");
            boolean first = true;
            for (String type : new String[]{"SKIN", "CAPE", "ELYTRA"}) {
                String url = properties.get(type);
                if (url == null) continue;
                if (!first) textures.append(',');
                first = false;
                textures.append(SimpleJson.quote(type)).append(":{\"url\":").append(SimpleJson.quote(url)).append('}');
            }
            textures.append("}}");
            result.put("textures", java.util.Base64.getEncoder().encodeToString(textures.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        return result;
    }
}
