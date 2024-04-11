package MightyLibrary.mightylib.sounds;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

public class SoundListener {
    private final Vector3f position;
    private final Vector3f speed;

    SoundListener(){
        position = new Vector3f();
        speed = new Vector3f();
    }

    public void setPosition(Vector3f position){
        this.position.x = position.x;
        this.position.y = position.y;
        this.position.z = position.z;

        AL10.alListener3f(AL10.AL_POSITION, position.x, position.y, position.z);
    }

    public void setPosition(Vector2f position){
        this.position.x = position.x;
        this.position.y = position.y;
        this.position.z = 0;

        AL10.alListener3f(AL10.AL_POSITION, position.x, position.y, this.position.z);
    }

    public Vector3f get3DPosition(){
        return new Vector3f(position);
    }

    public Vector2f get2DPosition(){
        return new Vector2f(position.x, position.y);
    }

    public void move2D(Vector2f move){
        this.position.x += move.x;
        this.position.y += move.y;

        AL10.alListener3f(AL10.AL_POSITION, position.x, position.y, position.z);
    }

    public void setSpeed(Vector3f speed){
        this.speed.x = speed.x;
        this.speed.y = speed.y;
        this.speed.z = speed.z;

        AL10.alListener3f(AL10.AL_VELOCITY, speed.x, speed.y, speed.z);
    }

    public void setSpeed(Vector2f speed){
        this.speed.x = speed.x;
        this.speed.y = speed.y;
        this.speed.z = 0;

        AL10.alListener3f(AL10.AL_VELOCITY, speed.x, speed.y, 0);
    }

    public Vector3f get3DSpeed(){
        return new Vector3f(speed);
    }

    public Vector2f get2DSpeed(){
        return new Vector2f(speed.x, speed.y);
    }
}
