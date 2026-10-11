package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.SocialInteractionsService;

import java.util.UUID;

public class YggdrasilSocialInteractionsService implements SocialInteractionsService {

    public YggdrasilSocialInteractionsService(final YggdrasilAuthenticationService authenticationService, final String accessToken, final Environment env) throws AuthenticationException {}

    @Override
    public boolean serversAllowed() {
        return true;
    }

    @Override
    public boolean realmsAllowed() {
        return true;
    }

    @Override
    public boolean chatAllowed() {
        return true;
    }

    @Override
    public boolean isBlockedPlayer(final UUID playerID) {
        return false;
    }
}
