package io.github.manucherf.dynamicfelling.compat;


import io.github.manucherf.dynamicfelling.DynamicFelling;
import io.github.manucherf.dynamicfelling.FellingTiming;
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

        int hits = FellingTiming.hitsToFell(swings);
        tooltip.add(Component.translatable("jade.dynamicfelling.swings", hits));
    }
}