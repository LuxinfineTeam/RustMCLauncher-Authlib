package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import team.luxinfine.authlib.NativeAPI;
import team.luxinfine.authlib.ServerAPI;
import team.luxinfine.mcsplitter.api.Invoke;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService {

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
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }

    private static GameProfile convertProfile(team.luxinfine.authlib.GameProfile profile) {
        //В старых GameProfile, где id - еще был String, а не UUID тип - требуется hex-формат UUID без дефисов
        return new GameProfile(profile.id.toString().replace("-", ""), profile.name);
    }
}
