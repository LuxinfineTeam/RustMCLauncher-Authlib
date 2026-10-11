package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.properties.Property;

import java.security.*;

public class YggdrasilServicesKeyInfo implements ServicesKeyInfo {

    private YggdrasilServicesKeyInfo(final PublicKey publicKey) {}

    public static ServicesKeyInfo createFromResources() {
        return new YggdrasilServicesKeyInfo(null);
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
