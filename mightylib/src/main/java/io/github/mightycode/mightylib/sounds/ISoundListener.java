package io.github.mightycode.mightylib.sounds;

import org.joml.Vector2f;
import org.joml.Vector3f;

// Where the sounds are heard from (ISoundService.getListener)
public interface ISoundListener {
    void setPosition(Vector3f position);

    // z = 0
    void setPosition(Vector2f position);

    Vector3f get3DPosition();

    Vector2f get2DPosition();

    void move2D(Vector2f move);

    void setSpeed(Vector3f speed);

    // z = 0
    void setSpeed(Vector2f speed);

    Vector3f get3DSpeed();

    Vector2f get2DSpeed();
}
