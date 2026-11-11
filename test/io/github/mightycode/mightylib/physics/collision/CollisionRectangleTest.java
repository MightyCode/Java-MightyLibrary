package io.github.mightycode.mightylib.physics.collision;

import io.github.mightycode.mightylib.utils.math.EShapeType;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Rectangles use screen coordinates: (x, y) is the top left corner, y grows downward
class CollisionRectangleTest {
    private static void assertRect(float x, float y, float w, float h, CollisionRectangle rectangle) {
        assertEquals(x, rectangle.x(), 1e-5, "x");
        assertEquals(y, rectangle.y(), 1e-5, "y");
        assertEquals(w, rectangle.w(), 1e-5, "w");
        assertEquals(h, rectangle.h(), 1e-5, "h");
        assertEquals(x + w, rectangle.oppX(), 1e-5, "oppX");
        assertEquals(y + h, rectangle.oppY(), 1e-5, "oppY");
    }

    @Test
    void constructors() {
        assertRect(1, 2, 3, 4, new CollisionRectangle(1, 2, 3, 4));
        assertRect(1, 2, 3, 4, new CollisionRectangle(new Vector2f(1, 2), new Vector2f(3, 4)));
        assertRect(1, 2, 3, 4, new CollisionRectangle(new Vector4f(1, 2, 3, 4)));
        assertRect(0, 0, 5, 5, new CollisionRectangle(0, 0, 5, 5));
        assertRect(-4, -3, 2, 1, new CollisionRectangle(-4, -3, 2, 1));
    }

    @Test
    void shapeAndText() {
        CollisionRectangle rectangle = new CollisionRectangle(0, 0, 1, 1);
        assertEquals(EShapeType.Rectangle, rectangle.getShapeType());
        assertSame(rectangle, rectangle.toCollisionType());
        assertEquals("Rectangle " + rectangle.id(), rectangle.toString());
    }

    @Test
    void eachCollisionHasItsOwnId() {
        assertNotEquals(new CollisionRectangle(0, 0, 1, 1).id(), new CollisionRectangle(0, 0, 1, 1).id());
    }

    @Test
    void settersMoveTheOppositeCorner() {
        CollisionRectangle rectangle = new CollisionRectangle(0, 0, 4, 2);

        rectangle.setX(10);
        rectangle.setY(20);

        assertRect(10, 20, 4, 2, rectangle);
    }

    @Test
    void setPositionIgnoresNotFiniteValues() {
        CollisionRectangle rectangle = new CollisionRectangle(5, 6, 1, 1);

        rectangle.setPosition(Float.NaN, Float.POSITIVE_INFINITY);
        assertRect(5, 6, 1, 1, rectangle);

        rectangle.setPosition(Float.NEGATIVE_INFINITY, 9);
        assertRect(5, 9, 1, 1, rectangle);
    }

    @Test
    void moves() {
        CollisionRectangle rectangle = new CollisionRectangle(1, 1, 2, 2);

        rectangle.move(3, -1);
        assertRect(4, 0, 2, 2, rectangle);

        rectangle.moveX(2);
        assertRect(6, 0, 2, 2, rectangle);

        rectangle.moveY(5);
        assertRect(6, 5, 2, 2, rectangle);
    }

    @Test
    void movingByZeroOrInfinityDoesNothing() {
        CollisionRectangle rectangle = new CollisionRectangle(1, 1, 2, 2);

        rectangle.moveX(0);
        rectangle.moveY(0);
        rectangle.moveX(Float.POSITIVE_INFINITY);
        rectangle.moveY(Float.NEGATIVE_INFINITY);

        assertRect(1, 1, 2, 2, rectangle);
    }

    @Test
    void sizeChangeKeepsTheTopLeftCorner() {
        CollisionRectangle rectangle = new CollisionRectangle(2, 3, 4, 5);

        rectangle.setSize(10, 1);

        assertRect(2, 3, 10, 1, rectangle);
    }

    @Test
    void oppositePositionPlacesTheRectangleFromItsBottomRightCorner() {
        CollisionRectangle rectangle = new CollisionRectangle(0, 0, 4, 2);

        rectangle.setOppositePosition(10, 8);

        assertRect(6, 6, 4, 2, rectangle);
    }

    @Test
    void boundsAndCopyAreIndependentRectangles() {
        CollisionRectangle rectangle = new CollisionRectangle(1, 2, 3, 4);

        CollisionRectangle bounds = rectangle.bounds();
        CollisionRectangle copy = rectangle.copy();
        bounds.setX(50);
        copy.setY(60);

        assertRect(1, 2, 3, 4, rectangle);
        assertRect(50, 2, 3, 4, bounds);
        assertRect(1, 60, 3, 4, copy);
        assertNotEquals(rectangle.id(), copy.id());
    }

    @Test
    void overlappingRectanglesCollideInBothDirections() {
        CollisionRectangle a = new CollisionRectangle(0, 0, 10, 10), b = new CollisionRectangle(5, 5, 10, 10);
        assertTrue(a.isColliding(b));
        assertTrue(b.isColliding(a));
    }

    @Test
    void containedRectangleCollides() {
        assertTrue(new CollisionRectangle(0, 0, 10, 10).isColliding(new CollisionRectangle(4, 4, 1, 1)));
        assertTrue(new CollisionRectangle(4, 4, 1, 1).isColliding(new CollisionRectangle(0, 0, 10, 10)));
    }

    @Test
    void touchingRectanglesDoNotCollide() {
        CollisionRectangle a = new CollisionRectangle(0, 0, 10, 10);
        assertFalse(a.isColliding(new CollisionRectangle(10, 0, 5, 5)), "right side");
        assertFalse(a.isColliding(new CollisionRectangle(0, 10, 5, 5)), "bottom side");
        assertFalse(a.isColliding(new CollisionRectangle(-5, 0, 5, 5)), "left side");
        assertFalse(a.isColliding(new CollisionRectangle(0, -5, 5, 5)), "top side");
    }

    @Test
    void farRectanglesDoNotCollide() {
        assertFalse(new CollisionRectangle(0, 0, 1, 1).isColliding(new CollisionRectangle(100, 100, 1, 1)));
    }

    @Test
    void anEmptyRectangleActsAsAPoint() {
        CollisionRectangle area = new CollisionRectangle(0, 0, 10, 10);
        assertTrue(new CollisionRectangle(5, 5, 0, 0).isColliding(area), "inside");
        assertFalse(new CollisionRectangle(10, 5, 0, 0).isColliding(area), "on the edge, like touching rectangles");
        assertFalse(new CollisionRectangle(15, 5, 0, 0).isColliding(area), "outside");
    }

    @Test
    void aShapeThatIsNotKnownDoesNotCollide() {
        Collision2D unknown = new CollisionRectangle(0, 0, 10, 10) {
            {
                shapeType = EShapeType.Circle;
            }
        };
        assertFalse(new CollisionRectangle(0, 0, 10, 10).isColliding(unknown));
    }

    @Test
    void collidingWithABoundedVolumeUsesItsBounds() {
        CollisionBoundedVolume2D volume = new CollisionBoundedVolume2D()
                .put("a", new CollisionRectangle(0, 0, 2, 2))
                .put("b", new CollisionRectangle(8, 8, 2, 2));

        assertTrue(new CollisionRectangle(4, 4, 1, 1).isColliding(volume), "inside the bounds, between the parts");
        assertFalse(new CollisionRectangle(20, 20, 1, 1).isColliding(volume));
    }

    // The moving rectangle is placed against the reference one, on the side of the direction
    private static CollisionRectangle placed(EDirection direction) {
        CollisionRectangle moving = new CollisionRectangle(5, 5, 2, 4);
        CollisionRectangle reference = new CollisionRectangle(10, 20, 6, 8);
        moving.replaceComparedTo(reference, direction);
        return moving;
    }

    @Test
    void placedOnTheRight() {
        assertRect(16, 5, 2, 4, placed(EDirection.Right));
    }

    @Test
    void placedOnTheLeft() {
        assertRect(8, 5, 2, 4, placed(EDirection.Left));
    }

    @Test
    void placedAbove() {
        assertRect(5, 16, 2, 4, placed(EDirection.Up));
    }

    @Test
    void placedBelow() {
        assertRect(5, 28, 2, 4, placed(EDirection.Down));
    }

    @Test
    void placedInACorner() {
        assertRect(16, 16, 2, 4, placed(EDirection.RightUp));
        assertRect(16, 28, 2, 4, placed(EDirection.RightDown));
        assertRect(8, 16, 2, 4, placed(EDirection.LeftUp));
        assertRect(8, 28, 2, 4, placed(EDirection.LeftDown));
    }

    @Test
    void noDirectionLeavesTheRectangleWhereItIs() {
        assertRect(5, 5, 2, 4, placed(EDirection.None));
    }

    @Test
    void aPlacedRectangleDoesNotOverlapItsReference() {
        CollisionRectangle reference = new CollisionRectangle(10, 20, 6, 8);
        for (EDirection direction : new EDirection[]{EDirection.Left, EDirection.Right, EDirection.Up, EDirection.Down,
                EDirection.LeftUp, EDirection.LeftDown, EDirection.RightUp, EDirection.RightDown}) {
            CollisionRectangle moving = new CollisionRectangle(12, 22, 2, 4);
            moving.replaceComparedTo(reference, direction);
            assertFalse(moving.isColliding(reference), direction.toString());
        }
    }

    @Test
    void placedAgainstABoundedVolumeUsesItsBounds() {
        CollisionBoundedVolume2D volume = new CollisionBoundedVolume2D()
                .put("a", new CollisionRectangle(10, 20, 3, 3))
                .put("b", new CollisionRectangle(13, 25, 3, 3));
        CollisionRectangle moving = new CollisionRectangle(0, 0, 2, 4);

        moving.replaceComparedTo(volume, EDirection.Right);

        assertEquals(16f, moving.x(), 1e-5, "the volume spans x 10 to 16");
    }
}
