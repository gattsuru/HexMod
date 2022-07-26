package at.petrak.hexcasting.api.misc;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record GravitySetting(Direction gravityDirection, boolean permanent, int timeLeft, Vec3 origin) {
    public static GravitySetting deny() {
        return new GravitySetting(Direction.DOWN, true, 0, new Vec3(0, 0, 0));
    }
}
