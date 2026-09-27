package io.github.manucherf.dynamicfelling.mixin;

import io.github.manucherf.dynamicfelling.ChopTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @ModifyVariable(method = "continueAttack", at = @At("STORE"), ordinal = 0)
    private BlockPos dynamicfelling$stickToTrunk(BlockPos pos) {
        return ChopTracker.stickyTarget(pos);
    }
}