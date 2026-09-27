package io.github.manucherf.dynamicfelling;

import com.dtteam.dynamictrees.block.branch.BasicRootsBlock;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
//import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class FellingTiming {
    public static final int FIRST_HIT_TICKS = 11;
    public static final int TICKS_PER_SWING = 20;
    private static final float ONE_BLOCK_RADIUS = 8.0F;
    private static final float NETHERITE_SPEED = 9.0F;
    private static final float SPEED_PER_EXTRA_TIER = 1.0F;

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

        if (!inChopReach(player, pos)) {
            event.setNewSpeed(0.0F);
            return;
        }

        float hardness = state.getDestroySpeed(player.level(), pos);
        if (hardness <= 0.0F) {
            return;
        }
        float divisor = !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state, player.level(), pos) ? 30.0F : 100.0F;
        float remaining = Math.max(1.0F, swings - savedHits(player, pos));
        //remaining swings instead of swings
        float progressPerTick = 1.0F / (breakTick(remaining) - 0.5F);
        event.setNewSpeed(progressPerTick * pace(player) * hardness * divisor);
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

    public static float swingsToFell(Player player, BlockGetter level, BlockPos pos) {
        return swingsToFell(player, level.getBlockState(trunkCenter(level, pos)));
    }

    public static int hitsToFell(float swings) {
        return Mth.ceil(swings - 0.001F);
    }

    //hit on 8 28 48 etc
    public static int breakTick(float swings) {
        return FIRST_HIT_TICKS + (hitsToFell(swings) - 1) * TICKS_PER_SWING;
    }


    public static int hitsLanded(float ticks) {
        return ticks < FIRST_HIT_TICKS ? 0 : 1 + (int) ((ticks - FIRST_HIT_TICKS) / TICKS_PER_SWING);
    }

    //swings needed for one wide, scaled by width
    public static float swingsNeeded(Player player, ItemStack axe, int radius) {
        //swing math
        float forOneBlock = FellingConfig.WOODEN_AXE_SWINGS.get().floatValue()
                - tierLevel(axe) * FellingConfig.SWINGS_SAVED_PER_TIER.get().floatValue()
                - efficiency(player, axe) * FellingConfig.SWINGS_SAVED_PER_EFFICIENCY.get().floatValue();
        float swings = Math.max(1.0F, forOneBlock * radius / ONE_BLOCK_RADIUS);
        int max = FellingConfig.MAX_SWINGS.get();
        return max > 0 ? Math.min(swings, max) : swings;
    }

    public static boolean isAxe(ItemStack stack) {
        return !stack.isEmpty()
                && !FellingConfig.isExcluded(stack)
                && (stack.getItem() instanceof AxeItem || stack.is(ItemTags.AXES));
    }

    //Wood/gold 0, stone 1, iron 2, diamond 3, netherite 4.
    private static float tierLevel(ItemStack stack) {
        //get axe material
        if (!(stack.getItem() instanceof TieredItem tiered)) {
            return 0;
        }
        Tier tier = tiered.getTier();
        float speed = tier.getSpeed();
        TagKey<Block> incorrect = tier.getIncorrectBlocksForDrops();
        if (incorrect.equals(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
            return beyondNetherite(speed);
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
        //modded tiers: guess from mining speed.
        return speed <= 2.0F ? 0 : speed <= 4.0F ? 1 : speed <= 6.0F ? 2 : speed <= 8.0F ? 3 : beyondNetherite(speed);
    }

    private static int efficiency(Player player, ItemStack stack) {
        //axe efficiency level
        return player.level().registryAccess().registry(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.getHolder(Enchantments.EFFICIENCY))
                .map(holder -> stack.getEnchantmentLevel(holder))
                .orElse(0);
    }


    //find trunk center
    public static BlockPos trunkCenter(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TrunkShellBlock shell) {
            TrunkShellBlock.ShellMuse muse = shell.getMuse(level, state, pos);
            if (muse != null) {
                return muse.pos();
            }
        }
        return pos;
    }


    //haste/fatigue
    public static float pace(Player player) {
        float pace = 1.0F;
        if (MobEffectUtil.hasDigSpeed(player)) {
            pace *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(player) + 1) * 0.2F;
        }
        MobEffectInstance fatigue = player.getEffect(MobEffects.DIG_SLOWDOWN);
        if (fatigue != null) {
            pace *= switch (fatigue.getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
        }
        return pace;
    }

    static boolean inChopReach(Player player, BlockPos pos) {
        double reach = FellingConfig.CHOP_REACH.get();
        // distance from the eyes to the nearest point of block
        return new AABB(pos).distanceToSqr(player.getEyePosition()) <= reach * reach;
    }


    //chop session
    private record Session(BlockPos pos, int saved) {}

    private static final Map<UUID, Session> SERVER_SESSIONS = new HashMap<>();
    private static Session clientSession;

    static void startClientSession(BlockPos pos, int saved) {
        clientSession = pos == null ? null : new Session(pos.immutable(), saved);
    }

    static void endServerSession(UUID player) {
        SERVER_SESSIONS.remove(player);
    }

    static int savedHits(Player player, BlockPos pos) {
        BlockPos trunk = trunkCenter(player.level(), pos);
        if (player.level().isClientSide()) {
            return clientSession != null && clientSession.pos().equals(pos) ? clientSession.saved() : 0;
        }
        Session session = SERVER_SESSIONS.get(player.getUUID());
        if (session == null || !session.pos().equals(trunk)) {
            //read saved count once when mining starts on the block, keep it fixed
            session = new Session(trunk.immutable(), ChopMemory.saved((ServerLevel) player.level(), trunk));
            SERVER_SESSIONS.put(player.getUUID(), session);
        }
        return session.saved();
    }


    public static float trunkSize(BlockGetter level, BlockPos center) {
        BlockState state = level.getBlockState(center);
        // DT radius 8 is 1-block trunk, 16 is 2, 24 is 3
        float radius = state.getBlock() instanceof BranchBlock branch ? branch.getRadius(state) : 8.0F;
        return Mth.clamp(radius / 8.0F, 0.5F, 3.0F);
    }

    //every tier above netherite of mining speed adds a level
    private static float beyondNetherite(float speed) {
        return 4.0F + Math.max(0.0F, speed - NETHERITE_SPEED) / SPEED_PER_EXTRA_TIER;
    }
}