package MightyLibrary.mightylib.physics.collision;

import MightyLibrary.mightylib.utils.math.EDirection;
import MightyLibrary.mightylib.utils.math.EShapeType;
import MightyLibrary.mightylib.utils.valueDebug.UniqId;
import org.joml.Vector2f;

public abstract class Collision2D extends UniqId {
    protected final Vector2f position;
    protected EShapeType shapeType;

    public Collision2D(){
        this.shapeType = EShapeType.Undefined;
        position = new Vector2f();
    }

    public abstract boolean isColliding(Collision2D collision);
    public abstract void replaceComparedTo(Collision2D b, EDirection direction);

    public abstract void setX(float x);
    public abstract void setY(float y);

    public void setPosition(float x, float y){
        if(Float.isFinite(x) && x != position.x)
            setX(x);

        if (Float.isFinite(y) && y != position.y)
            setY(y);
    }

    public void move(float x, float y){
        setPosition(position.x + x, position.y + y);
    }

    public void moveX(float x){
        if (x == 0f || Float.isInfinite(x))
            return;

        move(x, 0f);
    }

    public void moveY(float y){
        if (y == 0f || Float.isInfinite(y))
            return;

        move(0f, y);
    }


    public abstract CollisionRectangle bounds();

    public Collision2D toCollisionType(){
        return this;
    }

    public EShapeType getShapeType(){
        return shapeType;
    }

    public abstract Collision2D copy();

    @Override
    public String toString(){
        return shapeType + " " + id();
    }
}
