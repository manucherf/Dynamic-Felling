package io.github.manucherf.dynamicfelling;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class WhetstoneItem extends Item {
    public static final int USE_TICKS = 72000;

    //timeline in ticks
    public static final float IN_END = 6.0F;
    public static final float SIDE_A_END = 26.0F;
    public static final float FLIP_END = 36.0F;
    public static final float SIDE_B_END = 56.0F;
    public static final float CYCLE_END = SIDE_B_END + FLIP_END - SIDE_A_END;
    public static final int STROKES = 3;

    public WhetstoneItem(Properties properties) {
        super(properties);
    }

    //offhand only
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stone = player.getItemInHand(hand);
        ItemStack axe = player.getMainHandItem();
        if (hand != InteractionHand.OFF_HAND || !FellingTiming.canSharpen(axe) || FellingTiming.edge(axe) >= 100) {
            return InteractionResultHolder.pass(stone);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stone);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    private static void sharpen(LivingEntity entity, ItemStack stone) {
        ItemStack axe = entity.getMainHandItem();
        if (FellingTiming.canSharpen(axe) && FellingTiming.edge(axe) < 100) {
            axe.set(FellingComponents.EDGE.get(), Math.min(100, FellingTiming.edge(axe) + FellingConfig.WHETSTONE_RESTORE.get()));
            stone.hurtAndBreak(1, entity, EquipmentSlot.OFFHAND);
        }
    }

    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        ItemStack axe = entity.getMainHandItem();
        if (!FellingTiming.canSharpen(axe) || FellingTiming.edge(axe) >= 100) {
            entity.stopUsingItem();
            return;
        }

        if (level.isClientSide()) {
            return;
        }
        float t = cycleTick(getUseDuration(stack, entity) - remaining);
        if (strokeStarts(t, IN_END, SIDE_A_END) || strokeStarts(t, FLIP_END, SIDE_B_END)) {
            level.playSound(null, entity.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.3F, 1.6F);
            if (level instanceof ServerLevel server) {
                Vec3 at = entity.getEyePosition().add(entity.getLookAngle().scale(0.7)).add(0.0, -0.2, 0.0);
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack),
                        at.x, at.y, at.z, 4, 0.05, 0.05, 0.05, 0.02);
            }
        }
        if (t == SIDE_B_END) {
            sharpen(entity, stack);
        }
    }

    private static boolean strokeStarts(float t, float from, float to) {
        float length = (to - from) / STROKES;
        for (int i = 0; i < STROKES; i++) {
            if (t == Math.round(from + i * length)) {
                return true;
            }
        }
        return false;
    }

    public static float cycleTick(float used) {
        return used < IN_END ? used : IN_END + (used - IN_END) % (CYCLE_END - IN_END);
    }

    //axe flip 3rd person
    public static float flip(float t) {
        float f;
        if (t < SIDE_A_END) {
            f = 0.0F;
        } else if (t < FLIP_END) {
            f = (t - SIDE_A_END) / (FLIP_END - SIDE_A_END);
        } else if (t < SIDE_B_END) {
            f = 1.0F;
        } else {
            f = 1.0F - (t - SIDE_B_END) / (CYCLE_END - SIDE_B_END);
        }
        return f * f * (3.0F - 2.0F * f);
    }
}
