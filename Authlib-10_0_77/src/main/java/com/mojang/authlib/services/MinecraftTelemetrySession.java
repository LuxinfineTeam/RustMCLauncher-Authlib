package com.mojang.authlib.services;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.client.MinecraftClient;

import java.util.concurrent.Executor;

public class MinecraftTelemetrySession implements TelemetrySession {

    @VisibleForTesting
    MinecraftTelemetrySession(
        final MinecraftClient minecraftClient,
        final MinecraftServicesDiscoveryService discoveryService,
        final Executor ioExecutor
    ) {}

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public TelemetryEvent createNewEvent(final String type) {
        return new MinecraftTelemetryEvent(this, type);
    }

    void sendEvent(final String type, final JsonObject data) {}
}
