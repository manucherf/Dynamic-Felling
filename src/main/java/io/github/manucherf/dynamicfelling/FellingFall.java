package io.github.manucherf.dynamicfelling;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

public class FellingFall {
    public static Direction direction(LivingEntity entity, Direction dtChoice) {
        if (dtChoice.getAxis().isVertical()) {
            return dtChoice;
        }
        //falling back is only ever bad luck
        Direction away = entity.isShiftKeyDown() ? dtChoice.getOpposite() : dtChoice;
        return entity.getRandom().nextDouble() < FellingConfig.BACKFALL_CHANCE.get() ? away.getOpposite() : away;
    }
}