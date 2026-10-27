package com.mojang.authlib.services;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.services.request.AbuseReportRequest;
import com.mojang.authlib.services.response.KeyPairResponse;

import java.net.Proxy;
import java.nio.ByteBuffer;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

public class MinecraftServicesUserApiService implements UserApiService {
    //Из этих пропертей умышленно удалён realms, всё равно технически он недоступен,
    //зато хоть поменьше ошибок неудачных подключений в логах
    private static final UserProperties OFFLINE_PROPERTIES = new UserProperties(Set.of(UserFlag.CHAT_ALLOWED, UserFlag.SERVERS_ALLOWED), Map.of());

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
        try {
            // Майнкрафт блокирует чат если нет ключа, генерируем фейковые чтоб заглушить
            final java.security.KeyPair keyPair = KeyPairGenerator.getInstance("RSA")
                    .generateKeyPair();
            final Base64.Encoder encoder = Base64.getMimeEncoder(76, new byte[] {'\n'});
            final String publicKey = pem("RSA PUBLIC KEY", encoder.encodeToString(keyPair.getPublic().getEncoded()));
            final String privateKey = pem("RSA PRIVATE KEY", encoder.encodeToString(keyPair.getPrivate().getEncoded()));
            final Instant now = Instant.now();

            return new KeyPairResponse(
                    new KeyPairResponse.KeyPair(privateKey, publicKey),
                    ByteBuffer.wrap(new byte[] {0}),
                    now.plusSeconds(7L * 24 * 60 * 60).toString(),
                    now.plusSeconds(24L * 60 * 60).toString()
            );
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is not available", e);
        }
    }

    private static String pem(final String type, final String encoded) {
        return "-----BEGIN " + type + "-----\n" + encoded + "\n-----END " + type + "-----\n";
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
