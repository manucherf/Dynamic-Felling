package io.github.manucherf.dynamicfelling;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class Grinding {
    public static final TagKey<Item> STONES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "whetstone_stones"));
    public static final float BLOCK_SCALE = 0.5F;

    private Grinding() {}

    //stone main hand, sand offhand
    private static boolean canGrind(LivingEntity entity) {
        return entity.getMainHandItem().is(STONES) && entity.getOffhandItem().is(Tags.Items.SANDS);
    }

    public static boolean isGrinding(LivingEntity entity) {
        return entity.isUsingItem() && entity.getUsedItemHand() == InteractionHand.OFF_HAND
                && entity.getUseItem().is(Tags.Items.SANDS);
    }

    //no placing while grinding
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (canGrind(event.getEntity()) && !event.getEntity().isSecondaryUseActive()) {
            event.setUseItem(TriState.FALSE);
        }
    }

    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.OFF_HAND || !canGrind(player) || player.isUsingItem()) {
            return;
        }
        player.startUsingItem(InteractionHand.OFF_HAND);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getHand() == InteractionHand.OFF_HAND && event.getItem().is(Tags.Items.SANDS) && canGrind(event.getEntity())) {
            event.setDuration(WhetstoneItem.USE_TICKS);
        }
    }

    static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        LivingEntity entity = event.getEntity();
        if (!isGrinding(entity)) {
            return;
        }
        if (!canGrind(entity)) {
            entity.stopUsingItem();
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }
        float t = WhetstoneItem.cycleTick(WhetstoneItem.USE_TICKS - event.getDuration());
        boolean stroke = WhetstoneItem.strokeEffects(entity, event.getItem(), t);
        if (t == WhetstoneItem.SIDE_B_END) {
            grind(entity);
        } else if (stroke && t != WhetstoneItem.IN_END) {
            useSand(entity);
        }
    }

    private static void grind(LivingEntity entity) {
        entity.getOffhandItem().consume(1, entity);
        entity.getMainHandItem().consume(1, entity);
        ItemStack whetstone = new ItemStack(FellingItems.WHETSTONE.get());
        if (!(entity instanceof Player player) || !player.getInventory().add(whetstone)) {
            entity.spawnAtLocation(whetstone);
        }
        entity.level().playSound(null, entity.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 0.8F, 1.0F);
        if (!canGrind(entity)) {
            entity.stopUsingItem();
        }
    }

    public static float blockScale(float shrink) {
        return Mth.lerp(shrink, 1.0F, BLOCK_SCALE);
    }

    //pays for the stroke just done
    private static void useSand(LivingEntity entity) {
        entity.getOffhandItem().consume(1, entity);
        if (!canGrind(entity)) {
            entity.stopUsingItem();
        }
    }
}