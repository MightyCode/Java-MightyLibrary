package MightyLibrary.mightylib.util.math;

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
}
