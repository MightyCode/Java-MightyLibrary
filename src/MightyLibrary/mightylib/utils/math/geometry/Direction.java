package MightyLibrary.mightylib.utils.math.geometry;

import org.joml.Vector2f;
import org.joml.Vector4f;

public abstract class Direction {
    public static EDirection addToAnotherDirection(EDirection a, EDirection b) {
        if (a == EDirection.None) return b;
        else if (b == EDirection.None) return a;

        if (a == EDirection.Down) {
            if (b == EDirection.Up) {
                return EDirection.None;
            } else if (b == EDirection.Left) {
                return EDirection.LeftDown;
            } else if (b == EDirection.Right) {
                return EDirection.RightDown;
            } else if (b == EDirection.Down) {
                return EDirection.Down;
            } else {
                return EDirection.None;
            }
        } else if (a == EDirection.Right) {
            if (b == EDirection.Up) {
                return EDirection.RightUp;
            } else if (b == EDirection.Left) {
                return EDirection.None;
            } else if (b == EDirection.Right) {
                return EDirection.Right;
            } else if (b == EDirection.Down) {
                return EDirection.RightDown;
            } else {
                return EDirection.None;
            }
        } else if (a == EDirection.Up) {
            if (b == EDirection.Up) {
                return EDirection.Up;
            } else if (b == EDirection.Left) {
                return EDirection.LeftUp;
            } else if (b == EDirection.Right) {
                return EDirection.RightUp;
            } else if (b == EDirection.Down) {
                return EDirection.None;
            } else {
                return EDirection.None;
            }
        } else if (a == EDirection.Left) {
            if (b == EDirection.Up) {
                return EDirection.LeftUp;
            } else if (b == EDirection.Left) {
                return EDirection.Left;
            } else if (b == EDirection.Right) {
                return EDirection.None;
            } else if (b == EDirection.Down) {
                return EDirection.LeftDown;
            } else {
                return EDirection.None;
            }
        }

        return EDirection.None;
    }

    public static EDirection strToEDirection(String direction){
        direction = direction.trim().toLowerCase();

        if (direction.equals("left"))
            return EDirection.Left;
        if (direction.equals("right"))
            return EDirection.Right;
        if (direction.equals("up"))
            return EDirection.Up;
        if (direction.equals("down"))
            return EDirection.Down;
        if (direction.equals("leftup"))
            return EDirection.LeftUp;
        if (direction.equals("rightup"))
            return EDirection.RightUp;
        if (direction.equals("leftdown"))
            return EDirection.LeftDown;
        if (direction.equals("rightdown"))
            return EDirection.RightDown;

        return EDirection.None;
    }

    // range -> x from, y from, x to, y to
    public static EDirection RangedVectorToDirection(Vector2f vector, Vector4f range){
        return NormalizedVectorToDirection(
                (new Vector2f(
                        vector.x - range.x / (range.z - range.x),
                        vector.y - range.y / (range.w - range.y))).mul(2).sub(1, 1)
                );
    }

    public static EDirection NormalizedVectorToDirection(Vector2f vector){
        if (vector.x == -1) {
            if (vector.y == -1)
                return EDirection.LeftUp;
            else if (vector.y == 0)
                return EDirection.Left;
            else if (vector.y == 1)
                return EDirection.LeftDown;
        } else if (vector.x == 0) {
            if (vector.y == -1)
                return EDirection.Up;
            else if (vector.y == 0)
                return EDirection.None;
            else if (vector.y == 1)
                return EDirection.Down;
        } else if (vector.x == 1) {
            if (vector.y == -1)
                return EDirection.RightUp;
            else if (vector.y == 0)
                return EDirection.Right;
            else if (vector.y == 1)
                return EDirection.RightDown;
        }

        return EDirection.Undef;
    }

    public static Vector2f DirectionToNormalizedVector(EDirection direction){
        Vector2f vectorDirection = new Vector2f();

        switch (direction){
            case Left:
            case LeftUp:
            case LeftDown:
                vectorDirection.x = -1;
                break;
            case Right:
            case RightDown:
            case RightUp:
                vectorDirection.x = 1;
        }

        switch (direction){
            case Up:
            case LeftUp:
            case RightUp:
                vectorDirection.y = -1;
                break;
            case Down:
            case RightDown:
            case LeftDown:
                vectorDirection.y = 1;
        }

        return vectorDirection;
    }

    public static Vector2f DirectionToRangedVector(EDirection direction, Vector4f range){
        Vector2f temp = DirectionToNormalizedVector(direction);

        temp = temp.add(1, 1).div(2);

        return new Vector2f(
                temp.x * (range.z - range.x) + range.x,
                temp.y * (range.w - range.y) + range.y
        );
    }
}
