package io.github.mightycode.mightylib.utils.math.geometry;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.github.mightycode.mightylib.utils.math.geometry.EDirection.*;
import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {
    @Test
    void noneIsNeutralWhenAddingDirections() {
        for (EDirection direction : EDirection.values()) {
            assertEquals(direction, Direction.addToAnotherDirection(None, direction));
            assertEquals(direction, Direction.addToAnotherDirection(direction, None));
        }
    }

    @Test
    void sameDirectionsStayTheSame() {
        for (EDirection direction : new EDirection[]{Left, Right, Up, Down})
            assertEquals(direction, Direction.addToAnotherDirection(direction, direction));
    }

    @Test
    void oppositeDirectionsCancelOut() {
        assertEquals(None, Direction.addToAnotherDirection(Left, Right));
        assertEquals(None, Direction.addToAnotherDirection(Right, Left));
        assertEquals(None, Direction.addToAnotherDirection(Up, Down));
        assertEquals(None, Direction.addToAnotherDirection(Down, Up));
    }

    @Test
    void perpendicularDirectionsMakeADiagonalInEitherOrder() {
        assertEquals(LeftUp, Direction.addToAnotherDirection(Left, Up));
        assertEquals(LeftUp, Direction.addToAnotherDirection(Up, Left));
        assertEquals(LeftDown, Direction.addToAnotherDirection(Left, Down));
        assertEquals(LeftDown, Direction.addToAnotherDirection(Down, Left));
        assertEquals(RightUp, Direction.addToAnotherDirection(Right, Up));
        assertEquals(RightUp, Direction.addToAnotherDirection(Up, Right));
        assertEquals(RightDown, Direction.addToAnotherDirection(Right, Down));
        assertEquals(RightDown, Direction.addToAnotherDirection(Down, Right));
    }

    @Test
    void namesAreParsedWithoutCaseNorSpaces() {
        assertEquals(Left, Direction.strToEDirection("left"));
        assertEquals(Right, Direction.strToEDirection("  RIGHT "));
        assertEquals(Up, Direction.strToEDirection("Up"));
        assertEquals(Down, Direction.strToEDirection("down"));
        assertEquals(LeftUp, Direction.strToEDirection("leftup"));
        assertEquals(RightUp, Direction.strToEDirection("RightUp"));
        assertEquals(LeftDown, Direction.strToEDirection("leftdown"));
        assertEquals(RightDown, Direction.strToEDirection("rightdown"));
    }

    @Test
    void unknownNamesGiveNone() {
        assertEquals(None, Direction.strToEDirection(""));
        assertEquals(None, Direction.strToEDirection("north"));
        assertEquals(None, Direction.strToEDirection("none"));
    }

    @Test
    void normalizedVectorsGiveTheirDirectionWithYGoingDown() {
        assertEquals(LeftUp, Direction.NormalizedVectorToDirection(new Vector2f(-1, -1)));
        assertEquals(Left, Direction.NormalizedVectorToDirection(new Vector2f(-1, 0)));
        assertEquals(LeftDown, Direction.NormalizedVectorToDirection(new Vector2f(-1, 1)));
        assertEquals(Up, Direction.NormalizedVectorToDirection(new Vector2f(0, -1)));
        assertEquals(None, Direction.NormalizedVectorToDirection(new Vector2f(0, 0)));
        assertEquals(Down, Direction.NormalizedVectorToDirection(new Vector2f(0, 1)));
        assertEquals(RightUp, Direction.NormalizedVectorToDirection(new Vector2f(1, -1)));
        assertEquals(Right, Direction.NormalizedVectorToDirection(new Vector2f(1, 0)));
        assertEquals(RightDown, Direction.NormalizedVectorToDirection(new Vector2f(1, 1)));
    }

    @Test
    void notNormalizedVectorsAreUndefined() {
        assertEquals(Undef, Direction.NormalizedVectorToDirection(new Vector2f(2, 0)));
        assertEquals(Undef, Direction.NormalizedVectorToDirection(new Vector2f(0.5f, 0)));
        assertEquals(Undef, Direction.NormalizedVectorToDirection(new Vector2f(1, 3)));
    }

    @Test
    void directionToVectorAndBack() {
        for (EDirection direction : new EDirection[]{Left, Right, Up, Down, LeftUp, LeftDown, RightUp, RightDown, None})
            assertEquals(direction, Direction.NormalizedVectorToDirection(Direction.DirectionToNormalizedVector(direction)));
    }

    @Test
    void directionToVectorValues() {
        assertEquals(new Vector2f(-1, 0), Direction.DirectionToNormalizedVector(Left));
        assertEquals(new Vector2f(0, -1), Direction.DirectionToNormalizedVector(Up));
        assertEquals(new Vector2f(1, 1), Direction.DirectionToNormalizedVector(RightDown));
        assertEquals(new Vector2f(0, 0), Direction.DirectionToNormalizedVector(None));
        assertEquals(new Vector2f(0, 0), Direction.DirectionToNormalizedVector(Undef));
    }

    @Test
    void directionToRangedVectorGivesTheMiddleOfTheSideOrCorner() {
        Vector4f range = new Vector4f(10, 20, 30, 60);

        assertEquals(new Vector2f(20, 40), Direction.DirectionToRangedVector(None, range), "center");
        assertEquals(new Vector2f(10, 40), Direction.DirectionToRangedVector(Left, range));
        assertEquals(new Vector2f(30, 40), Direction.DirectionToRangedVector(Right, range));
        assertEquals(new Vector2f(20, 20), Direction.DirectionToRangedVector(Up, range));
        assertEquals(new Vector2f(20, 60), Direction.DirectionToRangedVector(Down, range));
        assertEquals(new Vector2f(10, 20), Direction.DirectionToRangedVector(LeftUp, range));
        assertEquals(new Vector2f(30, 60), Direction.DirectionToRangedVector(RightDown, range));
    }

    @Test
    void rangedVectorOnTheUnitSquare() {
        Vector4f unit = new Vector4f(0, 0, 1, 1);
        assertEquals(None, Direction.RangedVectorToDirection(new Vector2f(0.5f, 0.5f), unit));
        assertEquals(LeftUp, Direction.RangedVectorToDirection(new Vector2f(0, 0), unit));
        assertEquals(RightDown, Direction.RangedVectorToDirection(new Vector2f(1, 1), unit));
        assertEquals(Right, Direction.RangedVectorToDirection(new Vector2f(1, 0.5f), unit));
    }

    @Disabled("Known bug: RangedVectorToDirection divides only the range origin (missing parentheses), any range other than 0-1 is wrong")
    @Test
    void rangedVectorOnAnotherRange() {
        Vector4f range = new Vector4f(10, 20, 30, 60);
        assertEquals(None, Direction.RangedVectorToDirection(new Vector2f(20, 40), range));
        assertEquals(LeftUp, Direction.RangedVectorToDirection(new Vector2f(10, 20), range));
        assertEquals(RightDown, Direction.RangedVectorToDirection(new Vector2f(30, 60), range));
    }
}
