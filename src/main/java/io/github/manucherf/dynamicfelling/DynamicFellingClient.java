package io.github.manucherf.dynamicfelling;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = DynamicFelling.MODID, dist = Dist.CLIENT)
public final class DynamicFellingClient {
    public DynamicFellingClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onClientTick);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onPlaySound);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onInteraction);
        ChopAnimation.register();
        NeoForge.EVENT_BUS.addListener(ChopAnimation::onClientTick);
        NeoForge.EVENT_BUS.addListener(ChopHand::onRenderHand);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChopOffhand::onRenderPre);
        NeoForge.EVENT_BUS.addListener(ChopOffhand::onRenderPost);
        NeoForge.EVENT_BUS.addListener(ChopHand::onClientTick);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onMovementInput);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(SavedChops::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(SavedChops::onClientTick);
        NeoForge.EVENT_BUS.addListener(ChopTracker::onRenderGui);
        container.registerConfig(ModConfig.Type.CLIENT, FellingClientConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(DynamicFellingClient::onTooltip);
        NeoForge.EVENT_BUS.addListener(SharpenHand::onRenderHand);
        //NeoForge.EVENT_BUS.addListener(SharpenHand::onClientTick);
        SharpenAnimation.register();
        NeoForge.EVENT_BUS.addListener(SharpenAnimation::onClientTick);
    }

    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(FellingItems.WHETSTONE.get())) {
            event.getToolTip().add(Component.translatable("tooltip.dynamicfelling.whetstone_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!FellingTiming.canSharpen(stack)) {
            return;
        }
        int edge = FellingTiming.edge(stack);
        String key = edge >= 75 ? "keen" : edge >= 50 ? "sharp" : edge >= 25 ? "worn" : "dull";
        event.getToolTip().add(Component.translatable("tooltip.dynamicfelling.edge." + key).withStyle(ChatFormatting.GRAY));
    }
}