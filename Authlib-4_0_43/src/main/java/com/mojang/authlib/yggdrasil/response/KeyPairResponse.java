package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;

import java.nio.ByteBuffer;

public class KeyPairResponse extends Response {
    private KeyPair keyPair;
    @SerializedName("publicKeySignatureV2")
    private ByteBuffer publicKeySignature;
    private String expiresAt;
    private String refreshedAfter;

    public KeyPairResponse(
        final String privateKey,
        final String publicKey,
        final ByteBuffer publicKeySignature,
        final String expiresAt,
        final String refreshedAfter
    ) {
        this.keyPair = new KeyPair(privateKey, publicKey);
        this.publicKeySignature = publicKeySignature;
        this.expiresAt = expiresAt;
        this.refreshedAfter = refreshedAfter;
    }

    public String getPrivateKey() { return keyPair.privateKey; }
    public String getPublicKey() { return keyPair.publicKey; }
    public ByteBuffer getPublicKeySignature() { return publicKeySignature; }
    public String getExpiresAt() { return expiresAt; }
    public String getRefreshedAfter() { return refreshedAfter; }

    private static final class KeyPair {
        private final String privateKey;
        private final String publicKey;

        private KeyPair(final String privateKey, final String publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }
    }
}
