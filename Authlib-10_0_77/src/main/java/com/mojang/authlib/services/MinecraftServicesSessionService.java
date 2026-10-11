package com.mojang.authlib.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.services.response.MinecraftTexturesPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.luxinfine.authlib.NativeAPI;
import team.luxinfine.authlib.ServerAPI;
import team.luxinfine.mcsplitter.api.Invoke;

import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MinecraftServicesSessionService implements SessionService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Gson gson = new GsonBuilder().create();

    //Сигнатура обязана совпадать с оригиналом: конструктор вызывает MinecraftServicesDiscoveryService
    protected MinecraftServicesSessionService(ServicesKeySet servicesKeySet, Proxy proxy, MinecraftServicesDiscoveryService discoveryService) {}

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
        return property.value();
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

    @Override
    public Property getPackedTextures(GameProfile profile) {
        return Iterables.getFirst(profile.properties().get("textures"), null);
    }

    @Override
    public MinecraftProfileTextures unpackTextures(Property property) {
        if (property == null)
            return MinecraftProfileTextures.EMPTY;

        MinecraftTexturesPayload result;
        try {
            String json = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException | IllegalArgumentException e) {
            LOGGER.error("Could not decode textures payload", e);
            return MinecraftProfileTextures.EMPTY;
        }

        if (result == null || result.textures() == null || result.textures().isEmpty())
            return MinecraftProfileTextures.EMPTY;

        return new MinecraftProfileTextures(
            result.textures().get(MinecraftProfileTexture.Type.SKIN),
            result.textures().get(MinecraftProfileTexture.Type.CAPE),
            result.textures().get(MinecraftProfileTexture.Type.ELYTRA),
            SignatureState.SIGNED
        );
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
        //В этих версиях PropertyMap неизменяемый — наполняем многомэп до конструктора
        HashMultimap<String, Property> properties = HashMultimap.create();
        for (team.luxinfine.authlib.GameProfileProperty p : profile.properties)
            properties.put(p.name, new Property(p.name, p.value, ""));
        return new GameProfile(profile.id, profile.name, new PropertyMap(properties));
    }
}