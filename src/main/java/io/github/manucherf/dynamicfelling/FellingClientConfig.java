package io.github.manucherf.dynamicfelling;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FellingClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_COUNTER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SHOW_COUNTER = builder
                .comment("Show the swing counter beside the crosshair when looking at or chopping a trunk.")
                .define("showCounter", true);
        SPEC = builder.build();
    }

    private FellingClientConfig() {}

}