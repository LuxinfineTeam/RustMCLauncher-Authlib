package com.mojang.authlib.yggdrasil;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.luxinfine.authlib.CommonAPI;
import team.luxinfine.authlib.ServerJoinError;

import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YggdrasilMinecraftSessionService implements MinecraftSessionService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    //Сигнатура обязана совпадать с оригиналом: конструктор вызывает YggdrasilAuthenticationService
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
        team.luxinfine.authlib.GameProfile profile;
        try {
            profile = CommonAPI.has_joined(user, false, serverId);
        } catch (ServerJoinError e) {
            LOGGER.error("Server join rejected: " + e.getMessage(), e);
            throw new AuthenticationUnavailableException(e.getMessage());
        }
        if (profile == null) return null;
        return new ProfileResult(convertProfile(profile));
    }

    @Override
    public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
        return property.value();
    }

    @Override
    public ProfileResult fetchProfile(UUID profileId, boolean requireSecure) {
        team.luxinfine.authlib.GameProfile profile = CommonAPI.lookupProfiles(Collections.singletonList(profileId.toString()), true).get(profileId.toString());
        if (profile == null) return null;
        return new ProfileResult(convertProfile(profile));
    }

    @Override
    public Property getPackedTextures(GameProfile profile) {
        return Iterables.getFirst(profile.getProperties().get("textures"), null);
    }

    @Override
    public MinecraftProfileTextures unpackTextures(Property property) {
        if (property == null)
            return MinecraftProfileTextures.EMPTY;

        MinecraftTexturesPayload result;
        try {
            String json = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return MinecraftProfileTextures.EMPTY;
        }

        if (result == null || result.textures() == null)
            return MinecraftProfileTextures.EMPTY;

        return new MinecraftProfileTextures(
            result.textures().get(MinecraftProfileTexture.Type.SKIN),
            result.textures().get(MinecraftProfileTexture.Type.CAPE),
            result.textures().get(MinecraftProfileTexture.Type.ELYTRA),
            SignatureState.SIGNED
        );
    }

    public static GameProfile convertProfile(team.luxinfine.authlib.GameProfile profile) {
        if (profile == null) return null;
        GameProfile out = new GameProfile(profile.id, profile.name);
        for (java.util.Map.Entry<String, String> p : profile.toMojangProperties().entrySet())
            out.getProperties().put(p.getKey(), new Property(p.getKey(), p.getValue(), ""));
        return out;
    }
}
