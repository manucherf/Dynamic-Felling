package io.github.manucherf.dynamicfelling;

import com.dtteam.dynamictrees.block.branch.BasicRootsBlock;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Locale;

public final class FellingTiming {
    public static final int FIRST_HIT_TICKS = 8;
    public static final int TICKS_PER_SWING = 20;

    private static final float WOODEN_AXE_SWINGS = 10.0F;
    private static final float SWINGS_SAVED_PER_TIER = 1.0F;
    private static final float SWINGS_SAVED_PER_EFFICIENCY = 0.5F;
    private static final float ONE_BLOCK_RADIUS = 8.0F;

    private FellingTiming() {}

    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockState state = event.getState();
        BlockPos pos = event.getPosition().orElse(null);
        ItemStack axe = player.getMainHandItem();

        //when to skip
        if (pos == null || player.isCreative() || !isAxe(axe) || !(state.getBlock() instanceof BranchBlock branch) || branch instanceof BasicRootsBlock) {
            return;
        }

        //compute break speed
        float hardness = state.getDestroySpeed(player.level(), pos);
        if (hardness <= 0.0F) {
            return;
        }
        float divisor = !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state) ? 30.0F : 100.0F;

        //when block should break
        float swings = swingsNeeded(player, axe, branch.getRadius(state));
        int breakTick = FIRST_HIT_TICKS + (Mth.ceil(swings - 0.001F) - 1) * TICKS_PER_SWING;
        float progressPerTick = 1.0F / (breakTick - 0.5F);
        event.setNewSpeed(progressPerTick * hardness * divisor);

        //test readout
        if (player.level().isClientSide()) {
            player.displayClientMessage(Component.literal(String.format(Locale.ROOT, "Swings to fell: %.1f", swings)), true);
        }
    }
    public static float swingsNeeded(Player player, ItemStack axe, int radius) {
        //swing math
        float forOneBlock = WOODEN_AXE_SWINGS
                - tierLevel(axe) * SWINGS_SAVED_PER_TIER
                - efficiency(player, axe) * SWINGS_SAVED_PER_EFFICIENCY;
        return Math.max(1.0F, forOneBlock * radius / ONE_BLOCK_RADIUS);
    }

    public static boolean isAxe(ItemStack stack) {
        return !stack.isEmpty()
                && !FellingConfig.isExcluded(stack)
                && (stack.getItem() instanceof AxeItem || stack.is(ItemTags.AXES));
    }
}