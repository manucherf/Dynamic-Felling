package io.github.manucherf.dynamicfelling.mixin;


import com.dtteam.dynamictrees.utility.EntityUtils;
import io.github.manucherf.dynamicfelling.FellingFall;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityUtils.class, remap = false)
public class EntityUtilsMixin {
    @Inject(method = "getHitDirection", at = @At("RETURN"), cancellable = true)
    private static void dynamicfelling$fallDirection(LivingEntity entity, CallbackInfoReturnable<Direction> cir) {
        cir.setReturnValue(FellingFall.direction(entity, cir.getReturnValue()));
    }
}