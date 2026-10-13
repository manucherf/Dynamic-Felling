package io.github.manucherf.dynamicfelling.mixin;

import io.github.manucherf.dynamicfelling.FellingTiming;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    private ItemStack destroyingItem;

    //don't let vanilla restart mining from axe data change
    @Inject(method = "sameDestroyTarget", at = @At("HEAD"))
    private void dynamicfelling$keepMining(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        ItemStack held = Minecraft.getInstance().player.getMainHandItem();
        if (FellingTiming.canSharpen(held) && ItemStack.isSameItem(held, destroyingItem)) {
            destroyingItem = held;
        }
    }

    @Shadow private boolean isDestroying;
    @Shadow private BlockPos destroyBlockPos;
    @Shadow private int destroyDelay;

    //start fresh instead of continuing
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void dynamicfelling$freshStart(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (!isDestroying && destroyDelay <= 0 && pos.equals(destroyBlockPos)) {
            destroyBlockPos = new BlockPos(-1, -1, -1);
        }
    }
}