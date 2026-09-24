package io.github.manucherf.dynamicfelling;

import com.dtteam.dynamictrees.block.branch.BasicRootsBlock;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
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

    private static int tierLevel(ItemStack stack) {
        //get axe material
        if (!(stack.getItem() instanceof TieredItem tiered)) {
            return 0;
        }
        Tier tier = tiered.getTier();
        TagKey<Block> incorrect = tier.getIncorrectBlocksForDrops();
        if (incorrect.equals(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
            return 4;
        }
        if (incorrect.equals(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)) {
            return 3;
        }
        if (incorrect.equals(BlockTags.INCORRECT_FOR_IRON_TOOL)) {
            return 2;
        }
        if (incorrect.equals(BlockTags.INCORRECT_FOR_STONE_TOOL)) {
            return 1;
        }
        if (incorrect.equals(BlockTags.INCORRECT_FOR_WOODEN_TOOL) || incorrect.equals(BlockTags.INCORRECT_FOR_GOLD_TOOL)) {
            return 0;
        }
        float speed = tier.getSpeed();
        return speed <= 2.0F ? 0 : speed <= 4.0F ? 1 : speed <= 6.0F ? 2 : speed <= 8.0F ? 3 : 4;
    }

    private static int efficiency(Player player, ItemStack stack) {
        //axe efficiency level
        return player.level().registryAccess().registry(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.getHolder(Enchantments.EFFICIENCY))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }
}