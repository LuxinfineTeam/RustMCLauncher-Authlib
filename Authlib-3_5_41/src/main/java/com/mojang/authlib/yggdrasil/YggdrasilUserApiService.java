package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;

import java.net.Proxy;
import java.util.UUID;
import java.util.concurrent.Executor;

public class YggdrasilUserApiService implements UserApiService {

    public YggdrasilUserApiService(final String accessToken, final Proxy proxy, final Environment env) throws AuthenticationException {}

    @Override
    public UserProperties properties() {
        return OFFLINE_PROPERTIES;
    }

    @Override
    public TelemetrySession newTelemetrySession(final Executor executor) {
        return TelemetrySession.DISABLED;
    }

    @Override
    public KeyPairResponse getKeyPair() {
        return null;
    }

    @Override
    public boolean isBlockedPlayer(final UUID playerID) {
        return false;
    }

    @Override
    public void refreshBlockList() {}
}
