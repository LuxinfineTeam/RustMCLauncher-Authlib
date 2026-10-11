package team.luxinfine.authlib;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public final class GameProfile {
    @Nonnull
    public UUID id;
    @Nonnull
    public String name;
    @Nonnull
    public List<GameProfileProperty> properties;

    public GameProfile(@Nonnull UUID id, @Nonnull String name, @Nonnull List<GameProfileProperty> properties) {
        this.id = id;
        this.name = name;
        this.properties = properties;
    }
}
