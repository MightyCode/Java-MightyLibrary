package MightyLibrary.mightylib.physics.collision;

import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import MightyLibrary.mightylib.utils.math.EShapeType;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

public class CollisionBoundedVolume2D extends Collision2D {

    public final Map<String, Collision2D> Collisions;

    public CollisionBoundedVolume2D(){
        Collisions = new HashMap<>();
        this.shapeType = EShapeType.BoundedVolume2D;
    }

    @Override
    public boolean isColliding(Collision2D collision) {
        return bounds().isColliding(collision);
    }

    @Override
    public void replaceComparedTo(Collision2D b, EDirection direction) {
        switch (b.getShapeType()){
            case Rectangle:
                ReplaceBoundingVolume2DRectangle(this, (CollisionRectangle)b, direction);
            case BoundedVolume2D:
                ReplaceBoundingVolume2DRectangle(this, b.bounds(), direction);
        }
    }

    @Override
    public void setX(float x) {
        float diff = x - this.position.x;
        if (diff == 0 || Float.isInfinite(diff))
            return;

        this.position.x = x;

        for (Map.Entry<String, Collision2D> entry : Collisions.entrySet()){
            entry.getValue().moveX(diff);
        }
    }

    @Override
    public void setY(float y) {
        float diff = y - this.position.y;
        if (diff == 0 || Float.isInfinite(diff))
            return;

        this.position.y = y;
        for (Map.Entry<String, Collision2D> entry : Collisions.entrySet()){
            entry.getValue().moveY(diff);
        }
    }

    @Override
    public CollisionRectangle bounds() {
        Vector4f bound = null;
        CollisionRectangle temp;

        for (Map.Entry<String, Collision2D> entry : Collisions.entrySet()){
            temp = entry.getValue().bounds();

            if (bound == null){
                bound = new Vector4f(temp.x(), temp.y(), temp.oppX(), temp.oppY());
                continue;
            }

            if (temp.x() < bound.x) bound.x = temp.x();
            if (temp.y() < bound.y) bound.y = temp.y();
            if (temp.oppX() > bound.z) bound.z = temp.oppX();
            if (temp.oppY() > bound.w) bound.w = temp.oppY();
        }

        if (bound == null)
            return new CollisionRectangle(0, 0, 0, 0);

        return new CollisionRectangle(bound.x, bound.y,  bound.z - bound.x, bound.w - bound.y);
    }

    @Override
    public CollisionBoundedVolume2D copy() {
        CollisionBoundedVolume2D temp = new CollisionBoundedVolume2D();

        temp.setX(position.x);
        temp.setY(position.y);

        temp.Collisions.putAll(Collisions);

        return temp;
    }

    public void ReplaceBoundingVolume2DRectangle(CollisionBoundedVolume2D toReplace, CollisionRectangle reference, EDirection direction){
        CollisionRectangle temp = toReplace.bounds();

        switch(direction){
            case Right:
            case RightDown:
            case RightUp:
                toReplace.setX(reference.oppX());
                break;
            case Left:
            case LeftDown:
            case LeftUp:
                toReplace.setX(reference.x() - temp.w());
        }

        switch(direction){
            case Up:
            case LeftUp:
            case RightUp:
                toReplace.setY(reference.y() - temp.h());
                break;
            case Down:
            case LeftDown:
            case RightDown:
                toReplace.setY(reference.oppY());
        }
    }
}
