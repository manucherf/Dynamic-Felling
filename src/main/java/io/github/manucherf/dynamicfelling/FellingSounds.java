package io.github.manucherf.dynamicfelling;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FellingSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DynamicFelling.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOP = SOUNDS.register("chop", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOSH = SOUNDS.register("woosh", SoundEvent::createVariableRangeEvent);

    private FellingSounds() {}

    //0 on thin branches, 1 on trunks about three blocks thick
    public static float chopWeight(float size) {
        return Mth.clamp((size - 0.5F) / 2.5F, 0.0F, 1.0F);
    }

    public static float chopVolume(float size) {
        return 0.8F + 0.2F * size;
    }

    //deeper on thicker trunks
    public static float chopPitch(float size, RandomSource random) {
        return Math.max(0.7F, 1.0F - 0.15F * (size - 1.0F)) * (0.95F + random.nextFloat() * 0.1F);
    }

    //low wooden layer under every chop
    public static float layerVolume(float size) {
        return 0.15F + 0.55F * chopWeight(size);
    }

    public static float layerPitch(float size, RandomSource random) {
        return 0.75F - 0.2F * chopWeight(size) + random.nextFloat() * 0.05F;
    }
}