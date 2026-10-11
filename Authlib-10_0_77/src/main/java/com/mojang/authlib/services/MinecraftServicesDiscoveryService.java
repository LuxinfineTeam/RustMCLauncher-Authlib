package com.mojang.authlib.services;

import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpDiscoveryService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientException.ErrorType;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.response.discovery.DiscoveryResponse;
import com.mojang.authlib.services.response.discovery.Endpoint;
import com.mojang.authlib.services.response.discovery.Service;
import java.net.Proxy;
import java.util.List;
import java.util.function.Supplier;

public class MinecraftServicesDiscoveryService extends HttpDiscoveryService {

    private MinecraftServicesDiscoveryService(
            final Proxy proxy,
            final boolean servicesKeySetEnabled,
            final Supplier<DiscoveryResponse> discoverySupplier
    ) {
        super(proxy);
    }

    public static MinecraftServicesDiscoveryService create(final Proxy proxy) {
        return create(proxy, true, determineEnvironment());
    }

    public static MinecraftServicesDiscoveryService create(final Proxy proxy, final boolean servicesKeySetEnabled) {
        return create(proxy, servicesKeySetEnabled, determineEnvironment());
    }

    public static MinecraftServicesDiscoveryService create(
            final Proxy proxy,
            final boolean servicesKeySetEnabled,
            final Environment environment
    ) {
        return new MinecraftServicesDiscoveryService(
                proxy,
                servicesKeySetEnabled,
                createDiscoverySupplier(proxy, environment)
        );
    }

    private static Supplier<DiscoveryResponse> createDiscoverySupplier(final Proxy proxy, final Environment environment) {
        return DiscoveryResponse::offline;
    }

    public static MinecraftServicesDiscoveryService createOffline(final Proxy proxy) {
        return new MinecraftServicesDiscoveryService(proxy, false, DiscoveryResponse::offline);
    }

    private static Environment determineEnvironment() {
        return EnvironmentParser
                .getEnvironmentFromProperties()
                .orElse(MinecraftServicesEnvironment.PROD.getEnvironment());
    }

    @Override
    public SessionService createMinecraftSessionService() {
        return new MinecraftServicesSessionService(getServicesKeySet(), getProxy(), this);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new MinecraftServicesProfileRepository(getProxy(), this);
    }

    public UserApiService createUserApiService(final String accessToken) {
        return new MinecraftServicesUserApiService(accessToken, this, getProxy());
    }

    public FriendsService createFriendsService(final String accessToken) {
        return new MinecraftServicesFriendsService(accessToken, getProxy(), this);
    }

    public ServicesKeySet getServicesKeySet() {
        return ServicesKeySet.EMPTY;
    }

    public String getUrl(final Service service, final String endpointKey) {
        final Endpoint endpoint = getEndpoint(service, endpointKey);
        return endpoint.uri();
    }

    public List<String> getValidUris(final Service service, final String endpointKey) {
        final Endpoint endpoint = getEndpoint(service, endpointKey);
        final List<String> validUris = endpoint.validUris();
        return validUris != null ? validUris : List.of();
    }

    private Endpoint getEndpoint(final Service service, final String endpointKey) {
        throw new MinecraftClientException(ErrorType.SERVICE_UNAVAILABLE, "Services are unavailable");
    }

    public boolean isAllowedTextureDomain(final String url) {
        return true;
    }
}
