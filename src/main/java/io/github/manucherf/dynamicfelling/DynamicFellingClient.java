package io.github.manucherf.dynamicfelling;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = DynamicFelling.MODID, dist = Dist.CLIENT)
public final class DynamicFellingClient {
    public DynamicFellingClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onClientTick);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onPlaySound);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onInteraction);
    }
}