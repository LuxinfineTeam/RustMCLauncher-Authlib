package com.mojang.authlib.yggdrasil;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.luxinfine.authlib.NativeAPI;
import team.luxinfine.authlib.ServerAPI;
import team.luxinfine.mcsplitter.api.Invoke;

import java.util.*;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    protected YggdrasilMinecraftSessionService(YggdrasilAuthenticationService authenticationService) {
        super(authenticationService);
    }

    public static YggdrasilMinecraftSessionService create() {
        return new YggdrasilMinecraftSessionService(null);
    }

    @Override
    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
        Invoke.invokeClient(() -> {
            try {
                NativeAPI.joinMCServer(serverId);
            } catch (Exception e) {
                throw new AuthenticationException(e.getMessage());
            }
        });
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {
        return Invoke.invokeServer(() -> {
            team.luxinfine.authlib.GameProfile profile = ServerAPI.has_joined(user.getName(), false, serverId);
            if (profile == null) return null;
            return convertProfile(profile);
        });
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);
        if (textureProperty == null)
            return new HashMap<>();

        MinecraftTexturesPayload result;
        try {
            String json = new String(Base64.decodeBase64(textureProperty.getValue()), java.nio.charset.StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap<>();
        }

        return result.getTextures() == null ? new HashMap<>() : result.getTextures();
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
        if (profile.getId() == null)
            return profile;

        String key = profile.getId().toString();
        team.luxinfine.authlib.GameProfile result = lookupProfiles(Collections.singletonList(key), true).get(key);
        if (result == null)
            LOGGER.debug("Couldn't fetch profile properties for {} as the profile does not exist", profile);
        //Согласно ориг. либе authlib, если профиль не найден - нужно вернуть то, что закинули на вход
        return result == null ? profile : convertProfile(result);
    }

    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }

    //В ориг сорцах этот метод для отладки. В целях совместимости пусть будет, но без реализации
    public static void main(String[] args) {}

    public static Map<String, team.luxinfine.authlib.GameProfile> lookupProfiles(List<String> users, boolean byUuid) {
        return Invoke.invoke(() ->
            ServerAPI.lookupProfiles(users, byUuid)
        , () -> {
            try {
                return NativeAPI.lookupProfiles(users, byUuid);
            } catch (Exception e) {
                LOGGER.warn("Error while lookup profiles {}", e.toString());
                return Collections.emptyMap();
            }
        });
    }

    public static GameProfile convertProfile(team.luxinfine.authlib.GameProfile profile) {
        if (profile == null) return null;
        GameProfile out = new GameProfile(profile.id, profile.name);
        for (team.luxinfine.authlib.GameProfileProperty p : profile.properties)
            out.getProperties().put(p.name, new Property(p.name, p.value, ""));
        return out;
    }
}
