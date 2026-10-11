package com.mojang.authlib.services;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;

import java.net.URL;
import java.security.*;
import java.util.function.Supplier;

public class MinecraftServicesKeyInfo implements ServicesKeyInfo {

    private MinecraftServicesKeyInfo(final PublicKey publicKey) {}

    public static ServicesKeyInfo parse(final byte[] keyBytes) {
        return new MinecraftServicesKeyInfo(null);
    }

    public static ServicesKeySet get(final Supplier<URL> urlSupplier, final MinecraftClient client) {
        return ServicesKeySet.EMPTY;
    }

    @Override
    public Signature signature() {
        try {
            return Signature.getInstance("SHA1withRSA");
        } catch (final NoSuchAlgorithmException e) {
            throw new AssertionError("Failed to create signature", e);
        }
    }

    @Override
    public int keyBitCount() {
        return 4096;
    }

    @Override
    public boolean validateProperty(final Property property) {
        return true;
    }
}
