package io.github.mightycode.mightylibdemo.scenes.Platformer2D;

import org.jbox2d.common.Vec2;

public class PlayerValues {
    public final Vec2 position = new Vec2(0, 0);
    public final Vec2 size = new Vec2(1, 1);

    public static final float MOVE_SPEED = 100;
    public static final float JUMP_FORCE = 500f;
    public int groundCount;

    public boolean onGround() {
        return groundCount > 0;
    }

    public void setGroundCount(int groundCount) {
        this.groundCount += groundCount;
        System.out.println(groundCount);
    }
}
