package MightyLibrary.mightylib.sounds;

import MightyLibrary.mightylib.util.Timer;
import org.joml.Vector3f;

public class SoundSourceCreationInfo {
    public String name;
    public Vector3f position;
    public Vector3f speed;
    public float gain;
    public boolean loop;
    public boolean relative;

    public float delay;

    private final Timer timer;

    int managerId;

    public SoundSourceCreationInfo(){
        position = new Vector3f();
        speed = new Vector3f();
        gain = 1.0f;
        delay = 0.0f;

        timer = new Timer();

        managerId = -1;
    }


    void startTimer(){
        timer.start(delay);
    }

    void updateTimer(){
        timer.update();
    }

    boolean isTimerFinished(){
        return timer.isFinished();
    }

    public SoundSourceCreationInfo copy(){
        SoundSourceCreationInfo temp = new SoundSourceCreationInfo();

        temp.name = name;
        temp.position = new Vector3f(position);
        temp.speed = new Vector3f(speed);
        temp.speed = new Vector3f(speed);
        temp.gain = gain;
        temp.loop = loop;
        temp.relative = relative;

        return temp;
    }
}
