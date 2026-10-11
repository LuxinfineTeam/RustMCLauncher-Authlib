package team.luxinfine.authlib;

import javax.annotation.Nonnull;

public final class GameProfileProperty {
    @Nonnull
    public String name;
    @Nonnull
    public String value;

    public GameProfileProperty(@Nonnull String name, @Nonnull String value) {
        this.name = name;
        this.value = value;
    }
}
