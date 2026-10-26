package com.mojang.authlib.properties;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.security.PublicKey;

public record Property(
        @SerializedName("name")
        String name,
        @SerializedName("value")
        String value,
        @SerializedName("signature")
        @Nullable String signature
) {
    public Property(final String name, final String value) {
        this(name, value, null);
    }

    public boolean hasSignature() {
        return signature != null;
    }

    @Deprecated
    public boolean isSignatureValid(final PublicKey publicKey) {
        return true;
    }
}
