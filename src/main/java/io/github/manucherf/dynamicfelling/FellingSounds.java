package io.github.manucherf.dynamicfelling;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FellingSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DynamicFelling.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOP = SOUNDS.register("chop", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOSH = SOUNDS.register("woosh", SoundEvent::createVariableRangeEvent);

    private FellingSounds() {}
}