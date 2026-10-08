package io.github.manucherf.dynamicfelling.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.manucherf.dynamicfelling.FellingItems;
import io.github.manucherf.dynamicfelling.Grinding;
import io.github.manucherf.dynamicfelling.SharpenAnimation;
import io.github.manucherf.dynamicfelling.WhetstoneItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void dynamicfelling$flipAxe(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                        PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        boolean grinding = Grinding.isGrinding(entity);
        if (arm == entity.getMainArm() && entity.isUsingItem()
                && (entity.getUseItem().is(FellingItems.WHETSTONE.get()) || grinding)) {
            SharpenAnimation.transformAxe(entity, arm, poseStack);
        }
        if (grinding && stack.getItem() instanceof BlockItem) {
            float s = Grinding.blockScale(SharpenAnimation.windIn(entity));
            poseStack.scale(s, s, s);
        }
    }
}