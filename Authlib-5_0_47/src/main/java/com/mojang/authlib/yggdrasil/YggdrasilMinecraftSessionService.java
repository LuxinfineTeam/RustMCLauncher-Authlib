package com.mojang.authlib.yggdrasil;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.luxinfine.authlib.NativeAPI;
import team.luxinfine.authlib.ServerAPI;
import team.luxinfine.mcsplitter.api.Invoke;

import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YggdrasilMinecraftSessionService implements MinecraftSessionService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Gson gson = new GsonBuilder().create();

    //В оригинале (keySet, proxy, environment) используются для HTTP-запросов к Mojang;
    //лаунчер их не требует, но сигнатура обязана совпадать — конструктор вызывает YggdrasilAuthenticationService
    protected YggdrasilMinecraftSessionService(ServicesKeySet servicesKeySet, Proxy proxy, Environment environment) {}

    @Override
    public void joinServer(UUID profileId, String authenticationToken, String serverId) throws AuthenticationException {
        Invoke.invokeClient(() -> {
            try {
                NativeAPI.joinMCServer(serverId);
            } catch (Exception e) {
                throw new AuthenticationException(e.getMessage());
            }
        });
    }

    @Override
    public ProfileResult hasJoinedServer(String user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
        return Invoke.invokeServer(() -> {
            team.luxinfine.authlib.GameProfile profile = ServerAPI.has_joined(user, false, serverId);
            if (profile == null) return null;
            return new ProfileResult(convertProfile(profile));
        });
    }

    @Override
    public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
        //В оригинале здесь проверяется подпись проперти ключами Mojang; данные идут от лаунчера, проверку пропускаем
        return property.value();
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) throws InsecurePublicKeyException {
        Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);
        if (textureProperty == null)
            return new HashMap<>();

        MinecraftTexturesPayload result;
        try {
            String json = new String(Base64.getDecoder().decode(textureProperty.value()), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap<>();
        }

        return result.textures() == null ? new HashMap<>() : result.textures();
    }

    @Override
    public ProfileResult fetchProfile(UUID profileId, boolean requireSecure) {
        return Invoke.invoke(() -> {
            team.luxinfine.authlib.GameProfile profile = ServerAPI.lookupProfiles(Collections.singletonList(profileId.toString()), true).get(profileId.toString());
            if (profile == null) return null;
            return new ProfileResult(convertProfile(profile));
        }, () -> {
            try {
                team.luxinfine.authlib.GameProfile profile = NativeAPI.lookupProfiles(Collections.singletonList(profileId.toString()), true).get(profileId.toString());
                if (profile == null) return null;
                return new ProfileResult(convertProfile(profile));
            } catch (Exception e) {
                LOGGER.warn("Error while fetching profile {}", e.toString());
                return null;
            }
        });
    }

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
