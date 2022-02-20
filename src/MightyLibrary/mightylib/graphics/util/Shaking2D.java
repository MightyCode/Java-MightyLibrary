package MightyLibrary.mightylib.graphics.util;

import MightyLibrary.mightylib.physics.tweenings.type.Vector2fTweening;
import org.joml.Vector2f;

public class Shaking2D {
    private final Vector2fTweening tweening;
    private final Vector2f value;

    public Shaking2D(Vector2fTweening tweening){
        this.tweening = tweening;
        value = new Vector2f(0, 0);
    }

    public void update(){
        tweening.update();

        value.x = (float)((Math.random() - 0.5) * tweening.value().x * 2);
        value.y = (float)((Math.random() - 0.5) * tweening.value().y * 2);
    }

    public Vector2f value(){
        return new Vector2f(value);
    }

    public boolean isFinished(){
        return tweening.finished();
    }
}
