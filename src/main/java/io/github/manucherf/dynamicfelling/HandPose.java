package io.github.manucherf.dynamicfelling;

import net.minecraft.util.Mth;

//x right, y up, z toward, and angles for first person poses
record HandPose(float x, float y, float z, float lean, float tilt, float twist) {
    HandPose lerp(HandPose to, float t) {
        return new HandPose(Mth.lerp(t, x, to.x), Mth.lerp(t, y, to.y), Mth.lerp(t, z, to.z),
                Mth.lerp(t, lean, to.lean), Mth.lerp(t, tilt, to.tilt), Mth.lerp(t, twist, to.twist));
    }

    //ease in and out
    static float smooth(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}