package io.github.manucherf.dynamicfelling;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public final class ChopAnimation {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "chop");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "chop");

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
            //blend over 4t
            layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(4, Ease.INOUTSINE),
                    new KeyframeAnimationPlayer(animation), true);
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
            layer.speed.speed = pace;
        }
    }

    private static final class ChopLayer extends ModifierLayer<IAnimation> {
        private final SpeedModifier speed = new SpeedModifier(1.0F);

        private ChopLayer() {
            addModifierBefore(speed);
        }
    }
}