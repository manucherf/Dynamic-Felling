package io.github.manucherf.dynamicfelling;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AngryBees {
    static void onHit(ServerPlayer player, BlockPos trunk) {
        ServerLevel level = player.serverLevel();
        //7×7 column around the trunk, from the hit up to 12 blocks higher
        for (BlockPos pos : BlockPos.betweenClosed(trunk.offset(-3, 0, -3), trunk.offset(3, 12, 3))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.BEEHIVES) || !(level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive)) {
                continue;
            }
            if (level.random.nextDouble() >= FellingConfig.BEE_ANGER_CHANCE.get()) {
                continue;
            }
            //calm if sedated
            boolean smoked = hive.isSedated();
            hive.emptyAllLivingFromHive(player, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            if (smoked) {
                continue;
            }
            //piss off nests higher up the tree
            for (Bee bee : level.getEntitiesOfClass(Bee.class, new AABB(pos).inflate(8.0, 6.0, 8.0))) {
                //muhahaha
                if (bee.getTarget() == null) {
                    bee.setTarget(player);
                }
            }
        }
    }
}