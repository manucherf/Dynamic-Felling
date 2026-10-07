package io.github.manucherf.dynamicfelling;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FellingComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DynamicFelling.MODID);

    //0 dull, 50 normal, 100 keen
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EDGE = COMPONENTS.register("edge",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 100))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private FellingComponents() {}
}