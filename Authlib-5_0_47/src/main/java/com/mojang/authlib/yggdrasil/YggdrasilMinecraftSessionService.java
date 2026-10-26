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
import com.mojang.util.UUIDTypeAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.luxinfine.authlib.CommonAPI;

import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YggdrasilMinecraftSessionService implements MinecraftSessionService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    //В оригинале (keySet, proxy, environment) используются для HTTP-запросов к Mojang;
    //лаунчер их не требует, но сигнатура обязана совпадать — конструктор вызывает YggdrasilAuthenticationService
    protected YggdrasilMinecraftSessionService(ServicesKeySet servicesKeySet, Proxy proxy, Environment environment) {}

    @Override
    public void joinServer(UUID profileId, String authenticationToken, String serverId) throws AuthenticationException {
        try {
            CommonAPI.joinMCServer(serverId);
        } catch (Exception e) {
            throw new AuthenticationException(e.getMessage());
        }
    }

    @Override
    public ProfileResult hasJoinedServer(String user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
        team.luxinfine.authlib.GameProfile profile = CommonAPI.has_joined(user, false, serverId);
        if (profile == null) return null;
        return new ProfileResult(convertProfile(profile));
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
        team.luxinfine.authlib.GameProfile profile = CommonAPI.lookupProfiles(Collections.singletonList(profileId.toString()), true).get(profileId.toString());
        if (profile == null) return null;
        return new ProfileResult(convertProfile(profile));
    }

    public static GameProfile convertProfile(team.luxinfine.authlib.GameProfile profile) {
        if (profile == null) return null;
        GameProfile out = new GameProfile(profile.id, profile.name);
        for (java.util.Map.Entry<String, String> p : profile.toMojangProperties().entrySet())
            out.getProperties().put(p.getKey(), new Property(p.getKey(), p.getValue(), ""));
        return out;
    }
}
