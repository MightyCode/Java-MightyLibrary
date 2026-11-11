package io.github.mightycode.mightylib.physics.collision;

import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylib.utils.math.EShapeType;
import org.joml.Vector4f;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CollisionBoundedVolume2D extends Collision2D {

    private final Map<String, Collision2D> collisions;

    public CollisionBoundedVolume2D(){
        collisions = new HashMap<>();
        this.shapeType = EShapeType.BoundedVolume2D;
    }

    // Adds the collision, or replaces the one with this name
    public CollisionBoundedVolume2D put(String name, Collision2D collision) {
        collisions.put(name, Objects.requireNonNull(collision, "collision"));
        return this;
    }

    public Collision2D remove(String name) {
        return collisions.remove(name);
    }

    public Collision2D get(String name) {
        return collisions.get(name);
    }

    // The names of the collisions, unmodifiable
    public Set<String> names() {
        return Collections.unmodifiableSet(collisions.keySet());
    }

    @Override
    public boolean isColliding(Collision2D collision) {
        if (collisions.isEmpty())
            return false;

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

        for (Map.Entry<String, Collision2D> entry : collisions.entrySet()){
            entry.getValue().moveX(diff);
        }
    }

    @Override
    public void setY(float y) {
        float diff = y - this.position.y;
        if (diff == 0 || Float.isInfinite(diff))
            return;

        this.position.y = y;
        for (Map.Entry<String, Collision2D> entry : collisions.entrySet()){
            entry.getValue().moveY(diff);
        }
    }

    @Override
    public CollisionRectangle bounds() {
        Vector4f bound = null;
        CollisionRectangle temp;

        for (Map.Entry<String, Collision2D> entry : collisions.entrySet()){
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

        // The parts are copied: moving the copy must not move the original
        for (Map.Entry<String, Collision2D> entry : collisions.entrySet())
            temp.collisions.put(entry.getKey(), entry.getValue().copy());

        temp.position.set(position);

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
