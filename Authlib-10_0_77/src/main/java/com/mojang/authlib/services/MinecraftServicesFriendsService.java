package com.mojang.authlib.services;

import com.mojang.authlib.services.response.*;

import java.net.Proxy;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class MinecraftServicesFriendsService implements FriendsService {

    public MinecraftServicesFriendsService(final String accessToken, final Proxy proxy, final MinecraftServicesDiscoveryService discoveryService) {}

    @Override
    public Optional<Duration> getFriendsPollInterval() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> getPresencePollInterval() {
        return Optional.empty();
    }

    @Override
    public ResultCode getFriendData(final Consumer<FriendData> friendData) {
        friendData.accept(FriendData.empty());
        return ResultCode.SUCCESS;
    }

    @Override public ResultCode removeFriend(UUID playerId) { return ResultCode.SERVICE_NOT_AVAILABLE; }
    @Override public ResultCode acceptIncomingFriendRequest(UUID playerId) { return ResultCode.SERVICE_NOT_AVAILABLE; }
    @Override public ResultCode declineIncomingFriendRequest(UUID playerId) { return ResultCode.SERVICE_NOT_AVAILABLE; }
    @Override public ResultCode sendFriendRequest(String name) { return ResultCode.SERVICE_NOT_AVAILABLE; }
    @Override public ResultCode sendFriendRequest(UUID playerId) { return ResultCode.SERVICE_NOT_AVAILABLE; }
    @Override public ResultCode revokeOutgoingFriendRequest(UUID playerId) { return ResultCode.SERVICE_NOT_AVAILABLE; }
    @Override public ResultCode updateFriendSettings(boolean enableFriendList, boolean enableFriendInvites) { return ResultCode.SERVICE_NOT_AVAILABLE; }
    @Override public PresenceResponse presence(String status) { return PresenceResponse.empty(); }
}

