package MightyLibrary.mightylib.physics.collision;

import MightyLibrary.mightylib.utils.math.EDirection;
import MightyLibrary.mightylib.utils.math.EShapeType;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class CollisionRectangle extends Collision2D {
    private final Vector2f oppositePosition; // Down Left
    private final Vector2f size;

    private final Vector2f leftDown;
    private final Vector2f rightUp;

    public CollisionRectangle(float x, float y, float w, float h){
        shapeType = EShapeType.Rectangle;
        oppositePosition = new Vector2f();
        size = new Vector2f();
        leftDown = new Vector2f();
        rightUp = new Vector2f();

        size.x = w;
        size.y = h;

        position.x = x - 1;
        position.y = y - 1;
        setPosition(x, y);
    }

    public CollisionRectangle(Vector2f position, Vector2f size){
        this(position.x, position.y, size.x, size.y);
    }

    public CollisionRectangle(Vector4f positionSize){
        this(positionSize.x, positionSize.y, positionSize.z, positionSize.w);
    }

    @Override
    public boolean isColliding(Collision2D collision) {
        switch(collision.getShapeType()){
            case Rectangle:
                return CollisionRectangleRectangle(this, (CollisionRectangle) collision);
            case BoundedVolume2D:
                return CollisionRectangleRectangle(this, collision.bounds());
        }

        return false;
    }

    @Override
    public void replaceComparedTo(Collision2D b, EDirection direction) {
        switch(b.getShapeType()){
            case Rectangle:
                ReplaceRectangleRectangle(this, (CollisionRectangle) b, direction);
            case BoundedVolume2D:
                ReplaceRectangleRectangle(this, b.bounds(), direction);
        }
    }

    @Override
    public void setX(float x) {
        if(Float.isInfinite(x) || x == position.x)
            return;

        position.x = x;
        oppositePosition.x = x + w();

        rightUp.x = oppositePosition.x;
        leftDown.x = x;

    }

    @Override
    public void setY(float y) {
        if(Float.isInfinite(y) || y == position.y)
            return;

        position.y = y;
        oppositePosition.y = y + h();

        rightUp.y = y;
        leftDown.y = oppositePosition.y;
    }

    public void setSize(float width, float height){
        size.x = width;
        size.y = height;

        position.x -= 1;
        position.y -= 1;

        setPosition(position.x + 1, position.y + 1);
    }

    public void setOppositePosition(float rightPos, float lowerPos){
        setX(rightPos - w());
        setY(lowerPos - h());
    }

    @Override
    public CollisionRectangle bounds() {
        return copy();
    }

    @Override
    public CollisionRectangle copy() {
        return new CollisionRectangle(position.x, position.y, size.x, size.y);
    }

    public float x(){
        return position.x;
    }

    public float y(){
        return position.y;
    }

    public float w(){
        return size.x;
    }

    public float h(){
        return size.y;
    }

    public float oppX(){
        return oppositePosition.x;
    }

    public float oppY(){
        return oppositePosition.y;
    }


    /***
     *** STATIC METHOD
     ***/

    public static boolean CollisionRectangleRectangle(CollisionRectangle a, CollisionRectangle b){
        return !(a.position.x >= b.oppositePosition.x
                || b.position.x >= a.oppositePosition.x
                || a.position.y >= b.oppositePosition.y
                || b.position.y >= a.oppositePosition.y);
    }

    public static void ReplaceRectangleRectangle(CollisionRectangle toReplace, CollisionRectangle reference, EDirection direction){
        switch(direction){
            case Right:
            case RightDown:
            case RightUp:
                toReplace.setX(reference.oppositePosition.x);
                break;
            case Left:
            case LeftDown:
            case LeftUp:
                toReplace.setOppositePosition(reference.position.x, toReplace.oppositePosition.y);
        }

        switch(direction){
            case Up:
            case LeftUp:
            case RightUp:
                toReplace.setOppositePosition(toReplace.oppositePosition.x, reference.position.y);
                break;
            case Down:
            case LeftDown:
            case RightDown:
                toReplace.setY(reference.oppositePosition.y);
        }
    }
}
