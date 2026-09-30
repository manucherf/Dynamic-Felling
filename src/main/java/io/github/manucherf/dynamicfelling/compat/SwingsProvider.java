package io.github.manucherf.dynamicfelling.compat;


import io.github.manucherf.dynamicfelling.DynamicFelling;
import io.github.manucherf.dynamicfelling.FellingTiming;
import io.github.manucherf.dynamicfelling.SavedChops;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class SwingsProvider implements IBlockComponentProvider {
    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "swings_to_fell");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        float swings = FellingTiming.swingsToFell(accessor.getPlayer(), accessor.getLevel(), accessor.getPosition());
        if (swings <= 0) {
            return;
        }

        BlockPos center = FellingTiming.trunkCenter(accessor.getLevel(), accessor.getPosition());
        int saved = SavedChops.saved((ClientLevel) accessor.getLevel(), center);

        int hits = FellingTiming.hitsToFell(Math.max(1.0F, swings - saved));
        tooltip.add(Component.translatable("jade.dynamicfelling.swings", hits));
    }
}