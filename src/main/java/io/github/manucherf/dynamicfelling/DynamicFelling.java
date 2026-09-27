package io.github.manucherf.dynamicfelling;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;

@Mod(DynamicFelling.MODID)
public final class DynamicFelling {
    public static final String MODID = "dynamicfelling";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DynamicFelling(IEventBus modBus, ModContainer container) {
        FellingSounds.SOUNDS.register(modBus);
        modBus.addListener(FellingNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, FellingConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FellingTiming::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(FellingNetwork::onStartTracking);
        NeoForge.EVENT_BUS.addListener(FellingNetwork::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(FellingNetwork::onServerStopping);
    }
}
