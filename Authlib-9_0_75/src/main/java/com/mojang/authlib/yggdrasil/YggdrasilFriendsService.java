package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.response.*;

import java.net.Proxy;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class YggdrasilFriendsService implements FriendsService {

    public YggdrasilFriendsService(final String accessToken, final Proxy proxy, final Environment env) {}

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

