package io.github.mightycode.mightylibdemo.scenes.Platformer2D;

import org.jbox2d.common.Vec2;

public class PlayerValues {
    public Vec2 position = new Vec2(0, 0);
    public Vec2 size = new Vec2(1, 1);

    public float moveSpeed = 100;
    public float jumpForce = 500f;
    public int groundCount;

    public boolean onGround() {
        return groundCount > 0;
    }

    public void setGroundCount(int groundCount) {
        this.groundCount += groundCount;
        System.out.println(groundCount);
    }
}
