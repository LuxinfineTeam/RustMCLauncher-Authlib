package com.mojang.authlib.properties;

import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;

import java.security.PublicKey;

public class Property {
    private final String name;
    private final String value;
    private final String signature;

    public Property(final String name, final String value) {
        this(name, value, null);
    }

    public Property(final String name, final String value, final String signature) {
        this.name = name;
        this.value = value;
        this.signature = signature;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    public boolean hasSignature() {
        return signature != null;
    }

    /**
     * @deprecated Use {@link YggdrasilServicesKeyInfo#validateProperty(Property)}
     */
    @Deprecated
    public boolean isSignatureValid(final PublicKey publicKey) {
        return true;
    }
}
