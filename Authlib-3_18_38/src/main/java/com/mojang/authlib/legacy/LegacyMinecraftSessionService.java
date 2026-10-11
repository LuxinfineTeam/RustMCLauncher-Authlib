package com.mojang.authlib.legacy;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;

import java.net.InetAddress;
import java.util.Map;

public class LegacyMinecraftSessionService extends HttpMinecraftSessionService {
    private final YggdrasilMinecraftSessionService delegate = YggdrasilMinecraftSessionService.create();

    protected LegacyMinecraftSessionService(LegacyAuthenticationService authenticationService) {
        super(authenticationService);
    }

    @Override
    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
        delegate.joinServer(profile, authenticationToken, serverId);
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
        return delegate.hasJoinedServer(user, serverId, address);
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        return delegate.getTextures(profile, requireSecure);
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
        return delegate.fillProfileProperties(profile, requireSecure);
    }

    @Override
    public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
        return delegate.getSecurePropertyValue(property);
    }

    @Override
    public LegacyAuthenticationService getAuthenticationService() {
        return (LegacyAuthenticationService) super.getAuthenticationService();
    }
}
