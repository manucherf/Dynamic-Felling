package io.github.manucherf.dynamicfelling;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
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
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

public final class SharpenAnimation {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "sharpen");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "sharpen");
    private static final int OUT_TICKS = 6;
    private static final Axis HANDLE = Axis.of(new Vector3f(0.0F, 0.985F, -0.174F));

    private SharpenAnimation() {}

    static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 41, player -> new SharpenLayer());
    }

    private static SharpenLayer layer(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof SharpenLayer layer ? layer : null;
    }

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        for (AbstractClientPlayer player : minecraft.level.players()) {
            SharpenLayer layer = layer(player);
            if (layer == null) {
                continue;
            }
            boolean sharpening = (player.isUsingItem() && player.getUseItem().is(FellingItems.WHETSTONE.get()))
                    || Grinding.isGrinding(player);
            if (sharpening && !layer.playing) {
                start(player, layer);
            } else if (!sharpening && layer.playing) {
                layer.playing = false;
                layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(OUT_TICKS, Ease.INOUTSINE), null);
            }
        }
    }

    private static void start(AbstractClientPlayer player, SharpenLayer layer) {
        if (!(PlayerAnimationRegistry.getAnimation(ANIMATION) instanceof KeyframeAnimation animation)) {
            return;
        }
        float t = WhetstoneItem.cycleTick(WhetstoneItem.usedTicks(player));
        int fade = Math.max(1, Math.round(WhetstoneItem.IN_END - t));
        int offset = Math.max(0, Math.round(t - WhetstoneItem.IN_END));
        layer.playing = true;
        layer.mirror.setEnabled(player.getMainArm() == HumanoidArm.LEFT);
        layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(fade, Ease.INOUTSINE),
                new KeyframeAnimationPlayer(animation, offset), true);
    }

    private static final class SharpenLayer extends ModifierLayer<IAnimation> {
        private final MirrorModifier mirror = new MirrorModifier();
        private boolean playing;

        private SharpenLayer() {
            addModifierBefore(mirror);
        }
    }

    public static void transformAxe(LivingEntity entity, HumanoidArm arm, PoseStack pose) {
        float used = WhetstoneItem.usedTicks(entity) + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float t = WhetstoneItem.cycleTick(used);
        float in = Math.min(1.0F, t / WhetstoneItem.IN_END);
        float side = arm == HumanoidArm.LEFT ? -1.0F : 1.0F;
        float sec = Math.max(0.0F, t - WhetstoneItem.IN_END) / 20.0F;

        float flip = WhetstoneItem.flip(t);
        float x     = Mth.lerp(flip, -0.5F, 0.3F) - 2.0F * Mth.sin(flip * Mth.PI);
        float y     = Mth.lerp(flip, 0.0F, 0.0F);
        float z     = Mth.lerp(flip, 1.6F, 2.0F);
        float tip   = Mth.lerp(flip, 50.0F, 30.0F);
        float swing = Mth.lerp(flip, 5.0F, 10.0F);
        float lean  = -10.0F * Mth.sin(flip * Mth.PI);
        float twist = Mth.lerp(flip, 30.0F, 0.0F);

        pose.translate(side * x / 16.0F * in, y / 16.0F * in, z / 16.0F * in);
        pose.mulPose(Axis.XP.rotationDegrees(tip * in));
        pose.mulPose(Axis.ZP.rotationDegrees(side * swing * in));
        pose.translate(0.0F, 0.25F, 0.03125F);
        pose.mulPose(Axis.XP.rotationDegrees(lean));
        pose.mulPose(HANDLE.rotationDegrees(side * (twist * in + 180.0F * flip)));
        pose.translate(0.0F, -0.25F, -0.03125F);
    }

    public static float windIn(LivingEntity entity) {
        float used = WhetstoneItem.usedTicks(entity) + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        return Math.min(1.0F, WhetstoneItem.cycleTick(used) / WhetstoneItem.IN_END);
    }
}