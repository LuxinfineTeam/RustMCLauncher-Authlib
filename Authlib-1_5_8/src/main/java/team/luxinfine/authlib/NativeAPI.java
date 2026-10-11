package team.luxinfine.authlib;

import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

//Нативный API, предоставляемый Luxinfine rust лаунчером
@Environment(TargetSide.CLIENT)
public final class NativeAPI {
    //Блокирующий запрос начала подключения к майнкрафт серверу
    public static native void joinMCServer(@Nonnull String serverID) throws TimeoutException, RuntimeException;

    //Блокирующий запрос профилей игроков по никнеймам или по UUID
    @Nonnull
    public static native Map<String, GameProfile> lookupProfiles(@Nonnull List<String> users, boolean by_uuid) throws TimeoutException, RuntimeException;
}
