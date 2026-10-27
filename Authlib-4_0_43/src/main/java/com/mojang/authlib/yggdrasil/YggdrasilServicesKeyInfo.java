package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.properties.Property;

import java.net.URL;
import java.security.*;
import java.util.List;

public class YggdrasilServicesKeyInfo implements ServicesKeyInfo {
    private static final ServicesKeyInfo PERMISSIVE_KEY = new YggdrasilServicesKeyInfo(null);
    private static final PublicKey PLACEHOLDER_PUBLIC_KEY = new PublicKey() {
        @Override
        public String getAlgorithm() {
            return "RSA";
        }

        @Override
        public String getFormat() {
            return "X.509";
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }
    };

    private YggdrasilServicesKeyInfo(final PublicKey publicKey) {}

    public static ServicesKeyInfo parse(final byte[] keyBytes) {
        return PERMISSIVE_KEY;
    }

    public static ServicesKeySet get(final URL url, final YggdrasilAuthenticationService authenticationService) {
        final List<ServicesKeyInfo> keys = List.of(PERMISSIVE_KEY);
        return type -> keys;
    }

    @Override
    public Signature signature() {
        final Signature signature = new PermissiveSignature();
        try {
            signature.initVerify(PLACEHOLDER_PUBLIC_KEY);
        } catch (final InvalidKeyException e) {
            throw new AssertionError("Failed to initialize permissive signature", e);
        }
        return signature;
    }

    @Override
    public int keyBitCount() {
        return 4096;
    }

    @Override
    public boolean validateProperty(final Property property) {
        return false;
    }

    //Фейковая сигнатура, которая всегда будет успешно проходить проверку подписи, которой нет
    //Нужна для затычки проверок внутри майнкрафта через signature(), например для сообщений в чате
    private static final class PermissiveSignature extends Signature {
        private PermissiveSignature() {
            super("SHA1withRSA");
        }

        @Override
        protected void engineInitVerify(final PublicKey publicKey) {}

        @Override
        protected void engineInitSign(final PrivateKey privateKey) {}

        @Override
        protected void engineInitSign(final PrivateKey privateKey, final SecureRandom random) {}

        @Override
        protected void engineUpdate(final byte input) {}

        @Override
        protected void engineUpdate(final byte[] input, final int offset, final int length) {}

        @Override
        protected byte[] engineSign() {
            return new byte[0];
        }

        @Override
        protected boolean engineVerify(final byte[] signature) {
            return true;
        }

        @Override
        protected void engineSetParameter(final String parameter, final Object value) throws InvalidParameterException {}

        @Override
        protected Object engineGetParameter(final String parameter) throws InvalidParameterException {
            return null;
        }
    }
}
