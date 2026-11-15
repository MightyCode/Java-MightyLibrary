package io.github.mightycode.mightylib.sounds;

import org.joml.Vector2f;
import org.joml.Vector3f;

// The listener of the silent service: keeps its position and speed
public final class SilentSoundListener implements ISoundListener {
    private final Vector3f position = new Vector3f();
    private final Vector3f speed = new Vector3f();

    SilentSoundListener() {}

    @Override
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    @Override
    public void setPosition(Vector2f position) {
        this.position.set(position.x, position.y, 0);
    }

    @Override
    public Vector3f get3DPosition() {
        return new Vector3f(position);
    }

    @Override
    public Vector2f get2DPosition() {
        return new Vector2f(position.x, position.y);
    }

    @Override
    public void move2D(Vector2f move) {
        position.x += move.x;
        position.y += move.y;
    }

    @Override
    public void setSpeed(Vector3f speed) {
        this.speed.set(speed);
    }

    @Override
    public void setSpeed(Vector2f speed) {
        this.speed.set(speed.x, speed.y, 0);
    }

    @Override
    public Vector3f get3DSpeed() {
        return new Vector3f(speed);
    }

    @Override
    public Vector2f get2DSpeed() {
        return new Vector2f(speed.x, speed.y);
    }
}
