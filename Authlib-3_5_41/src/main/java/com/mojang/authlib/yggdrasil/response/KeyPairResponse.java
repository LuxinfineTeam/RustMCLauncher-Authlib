package com.mojang.authlib.yggdrasil.response;

public class KeyPairResponse extends Response {
    private KeyPair keyPair;
    private String publicKeySignature;
    private String expiresAt;
    private String refreshedAfter;

    public KeyPairResponse(KeyPair keyPair, String publicKeySignature, String expiresAt, String refreshedAfter) {
        this.keyPair = keyPair;
        this.publicKeySignature = publicKeySignature;
        this.expiresAt = expiresAt;
        this.refreshedAfter = refreshedAfter;
    }

    public String getPrivateKey() {
        return keyPair.privateKey;
    }

    public String getPublicKey() {
        return keyPair.publicKey;
    }

    public String getPublicKeySignature() {
        return publicKeySignature;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getRefreshedAfter() {
        return refreshedAfter;
    }

    public static final class KeyPair {
        private String privateKey;
        private String publicKey;

        public KeyPair(String privateKey, String publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }
    }
}
