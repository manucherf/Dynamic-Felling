package io.github.manucherf.dynamicfelling;

import com.dtteam.dynamictrees.block.branch.BasicRootsBlock;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
//import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;


public final class FellingTiming {
    public static final int FIRST_HIT_TICKS = 8;
    public static final int TICKS_PER_SWING = 20;

    private static final float WOODEN_AXE_SWINGS = 10.0F;
    private static final float SWINGS_SAVED_PER_TIER = 1.0F;
    private static final float SWINGS_SAVED_PER_EFFICIENCY = 0.5F;
    private static final float ONE_BLOCK_RADIUS = 8.0F;

    private FellingTiming() {}

    //replaces mining speed on Dynamic Trees so blocks break on last hit
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockState state = event.getState();
        BlockPos pos = event.getPosition().orElse(null);
        float swings = swingsToFell(player, state);

        //when to skip
        if (pos == null || swings <= 0.0F) {
            return;
        }

        float hardness = state.getDestroySpeed(player.level(), pos);
        if (hardness <= 0.0F) {
            return;
        }
        float divisor = !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state, player.level(), pos) ? 30.0F : 100.0F;
        float progressPerTick = 1.0F / (breakTick(swings) - 0.5F);
        event.setNewSpeed(progressPerTick * hardness * divisor);
    }

    //returns 0 when block or tool isn't a handled one
    public static float swingsToFell(Player player, BlockState state) {
        ItemStack axe = player.getMainHandItem();
        if (player.isCreative() || !isAxe(axe)
                || !(state.getBlock() instanceof BranchBlock branch) || branch instanceof BasicRootsBlock) {
            return 0.0F;
        }
        return swingsNeeded(player, axe, branch.getRadius(state));
    }

    //swap shell with center branch
    public static float swingsToFell(Player player, BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TrunkShellBlock shell) {
            TrunkShellBlock.ShellMuse muse = shell.getMuse(level, state, pos);
            if (muse == null) {
                return 0.0F;
            }
            state = muse.state();
        }
        return swingsToFell(player, state);
    }

    public static int hitsToFell(float swings) {
        return Mth.ceil(swings - 0.001F);
    }

    //hit on 8 28 48 etc
    public static int breakTick(float swings) {
        return FIRST_HIT_TICKS + (hitsToFell(swings) - 1) * TICKS_PER_SWING;
    }

    public static boolean isHitTick(int ticks) {
        return ticks >= FIRST_HIT_TICKS && (ticks - FIRST_HIT_TICKS) % TICKS_PER_SWING == 0;
    }

    public static int hitsLanded(int ticks) {
        return ticks < FIRST_HIT_TICKS ? 0 : 1 + (ticks - FIRST_HIT_TICKS) / TICKS_PER_SWING;
    }

    //swings needed for one wide, scaled by width
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

    //Wood/gold 0, stone 1, iron 2, diamond 3, netherite 4.
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
        // Modded tiers: guess from mining speed.
        float speed = tier.getSpeed();
        return speed <= 2.0F ? 0 : speed <= 4.0F ? 1 : speed <= 6.0F ? 2 : speed <= 8.0F ? 3 : 4;
    }

    private static int efficiency(Player player, ItemStack stack) {
        //axe efficiency level
        return player.level().registryAccess().registry(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.getHolder(Enchantments.EFFICIENCY))
                .map(holder -> stack.getEnchantmentLevel(holder))
                .orElse(0);
    }
}