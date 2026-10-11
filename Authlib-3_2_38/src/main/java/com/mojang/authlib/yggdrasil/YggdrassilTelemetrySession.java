package com.mojang.authlib.yggdrasil;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.mojang.authlib.Environment;
import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetryPropertyContainer;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.client.MinecraftClient;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class YggdrassilTelemetrySession implements TelemetrySession {

    @VisibleForTesting
    YggdrassilTelemetrySession(final MinecraftClient minecraftClient, final Environment environment, final Executor ioExecutor) {}

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public TelemetryEvent createNewEvent(final String type) {
        return new YggdrassilTelemetryEvent(this, type);
    }

    @Override
    public TelemetryPropertyContainer globalProperties() {
        return TelemetryPropertyContainer.forJsonObject(new JsonObject());
    }

    @Override
    public void eventSetupFunction(final Consumer<TelemetryPropertyContainer> eventSetupFunction) {}

    void sendEvent(final String type, final JsonObject data) {}
}
