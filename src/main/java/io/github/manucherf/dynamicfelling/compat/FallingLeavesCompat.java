package io.github.manucherf.dynamicfelling.compat;

import de.cheaterpaul.fallingleaves.config.LeafSettingsEntry;
import de.cheaterpaul.fallingleaves.data.LeafLoader;
import de.cheaterpaul.fallingleaves.util.LeafUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class FallingLeavesCompat {
    public static void spawnLeaf(ClientLevel level, BlockPos pos, BlockState state, RandomSource random) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        LeafSettingsEntry settings = LeafLoader.getLeafSetting(id);
        LeafUtil.trySpawnLeafParticle(state, level, pos, random, settings);
    }
}
