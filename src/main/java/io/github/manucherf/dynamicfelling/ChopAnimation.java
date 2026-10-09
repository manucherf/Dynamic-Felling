package io.github.manucherf.dynamicfelling;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.ClientTickEvent;


public final class ChopAnimation {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "chop");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "chop");
    private static final int IMPACT_TICK = 8;

    private ChopAnimation() {}

    static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 42, player -> new ChopLayer());
    }

    private static ChopLayer layer(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof ChopLayer layer ? layer : null;
    }

    static void start(AbstractClientPlayer player) {
        //lookup animation
        ChopLayer layer = layer(player);
        if (layer != null && PlayerAnimationRegistry.getAnimation(ANIMATION) instanceof KeyframeAnimation animation) {
            //left-handed
            layer.mirror.setEnabled(player.getMainArm() == HumanoidArm.LEFT);
            //blend over 4t
            layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(4, Ease.INOUTSINE),
                    new KeyframeAnimationPlayer(animation, IMPACT_TICK + FellingTiming.TICKS_PER_SWING - FellingTiming.FIRST_HIT_TICKS), true);
        }
    }

    static void stop(AbstractClientPlayer player) {
        //fade to normal
        ChopLayer layer = layer(player);
        if (layer != null) {
            layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(4, Ease.INOUTSINE), null);
        }
    }

    //animation pace
    static void setPace(AbstractClientPlayer player, float pace) {
        ChopLayer layer = layer(player);
        if (layer != null) {
            layer.pace = pace;
        }
    }

    private static final class ChopLayer extends ModifierLayer<IAnimation> {
        private final SpeedModifier speed = new SpeedModifier(1.0F);
        private final MirrorModifier mirror = new MirrorModifier();
        private float pace = 1.0F;
        private ChopLayer() {
            addModifierBefore(speed);
            addModifierBefore(mirror);
        }
    }

    static boolean isPlaying(AbstractClientPlayer player) {
        ChopLayer layer = layer(player);
        return layer != null && layer.getAnimation() != null && layer.getAnimation().isActive();
    }

    static void showOther(int entityId, boolean chopping, float pace) {
        //find packet owner
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(entityId) instanceof AbstractClientPlayer player)
                || player == minecraft.player) {
            return;
        }
        if (!chopping) {
            stop(player);
            return;
        }
        if (!isPlaying(player)) {
            start(player);
        }
        setPace(player, pace);
    }

    private static void resync(AbstractClientPlayer player) {
        ChopLayer layer = layer(player);
        if (layer != null && PlayerAnimationRegistry.getAnimation(ANIMATION) instanceof KeyframeAnimation animation) {
            layer.setAnimation(new KeyframeAnimationPlayer(animation, IMPACT_TICK - ChopTracker.SWING_LEAD_TICKS));
        }
    }

    static void onClientTick(ClientTickEvent.Post event) {
        //check players game knows about
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        for (AbstractClientPlayer player : minecraft.level.players()) {
            if (player != minecraft.player && player.swinging && player.swingTime == 0 && isPlaying(player)) {
                resync(player);
            }
            ChopLayer layer = layer(player);
            if (layer != null && layer.getAnimation() instanceof KeyframeAnimationPlayer anim) {
                layer.speed.speed = rate(anim.getTick(), layer.pace);
            }
        }
    }

    //swing full speed, slow tempo stretches rest and hold
    private static float rate(int tick, float pace) {
        if (pace >= 1.0F) {
            return pace;
        }
        float s = Mth.positiveModulo(tick - IMPACT_TICK, FellingTiming.TICKS_PER_SWING);
        float extra = FellingTiming.TICKS_PER_SWING / pace - FellingTiming.TICKS_PER_SWING;
        if (s < 6.0F) {
            return 6.0F / (6.0F + extra * (1.0F - ChopHand.HOLD_SHARE));
        }
        if (s < 12.0F) {
            return 1.0F;
        }
        if (s < 15.0F) {
            return 3.0F / (3.0F + extra * ChopHand.HOLD_SHARE);
        }
        return 1.0F;
    }

}