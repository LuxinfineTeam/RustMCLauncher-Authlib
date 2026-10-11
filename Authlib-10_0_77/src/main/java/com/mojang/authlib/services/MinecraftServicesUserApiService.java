package com.mojang.authlib.services;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.services.request.AbuseReportRequest;
import com.mojang.authlib.services.response.KeyPairResponse;

import java.net.Proxy;
import java.util.UUID;
import java.util.concurrent.Executor;

public class MinecraftServicesUserApiService implements UserApiService {
    private final MinecraftServicesDiscoveryService discoveryService;
    private final MinecraftClient minecraftClient;

    public MinecraftServicesUserApiService(final String accessToken, final MinecraftServicesDiscoveryService discoveryService, final Proxy proxy) {
        this.discoveryService = discoveryService;
        this.minecraftClient = new MinecraftClient(accessToken, proxy);
    }

    @Override
    public TelemetrySession newTelemetrySession(final Executor executor) {
        return new MinecraftTelemetrySession(minecraftClient, discoveryService, executor);
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

    @Override
    public UserProperties fetchProperties() throws AuthenticationException {
        return OFFLINE_PROPERTIES;
    }

    @Override
    public void reportAbuse(final AbuseReportRequest request) {}

    @Override
    public boolean canSendReports() {
        return false;
    }

    @Override
    public AbuseReportLimits getAbuseReportLimits() {
        return AbuseReportLimits.DEFAULTS;
    }
}
