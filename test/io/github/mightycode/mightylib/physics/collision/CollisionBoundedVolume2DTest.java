package io.github.mightycode.mightylib.physics.collision;

import io.github.mightycode.mightylib.utils.math.EShapeType;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class CollisionBoundedVolume2DTest {
    private static CollisionRectangle rect(float x, float y, float w, float h) {
        return new CollisionRectangle(x, y, w, h);
    }

    private static void assertBounds(float x, float y, float w, float h, Collision2D volume) {
        CollisionRectangle bounds = volume.bounds();
        assertEquals(x, bounds.x(), 1e-5, "x");
        assertEquals(y, bounds.y(), 1e-5, "y");
        assertEquals(w, bounds.w(), 1e-5, "w");
        assertEquals(h, bounds.h(), 1e-5, "h");
    }

    // Two rectangles whose bounds go from (0, 0) to (10, 6)
    private static CollisionBoundedVolume2D twoParts() {
        return new CollisionBoundedVolume2D().put("a", rect(0, 0, 4, 6)).put("b", rect(8, 2, 2, 2));
    }

    @Test
    void shape() {
        assertEquals(EShapeType.BoundedVolume2D, new CollisionBoundedVolume2D().getShapeType());
    }

    @Test
    void anEmptyVolumeHasEmptyBoundsAtTheOrigin() {
        assertBounds(0, 0, 0, 0, new CollisionBoundedVolume2D());
    }

    @Disabled("Known bug: the bounds of an empty volume are a point at the origin, which collides with whatever covers the origin")
    @Test
    void anEmptyVolumeCollidesWithNothing() {
        assertFalse(new CollisionBoundedVolume2D().isColliding(rect(-5, -5, 10, 10)));
    }

    @Test
    void partsAreAddedReplacedRemovedByName() {
        CollisionBoundedVolume2D volume = new CollisionBoundedVolume2D();
        CollisionRectangle first = rect(0, 0, 1, 1), second = rect(5, 5, 1, 1);

        assertSame(volume, volume.put("a", first));
        volume.put("b", second);
        assertSame(first, volume.get("a"));
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), volume.names());

        volume.put("a", second);
        assertSame(second, volume.get("a"), "same name: replaced");
        assertEquals(2, volume.names().size());

        assertSame(second, volume.remove("b"));
        assertNull(volume.get("b"));
        assertNull(volume.remove("b"));
    }

    @Test
    void aPartCannotBeNull() {
        assertThrows(NullPointerException.class, () -> new CollisionBoundedVolume2D().put("a", null));
    }

    @Test
    void theNamesCannotBeChangedFromOutside() {
        CollisionBoundedVolume2D volume = twoParts();
        assertThrows(UnsupportedOperationException.class, () -> volume.names().add("c"));
        assertThrows(UnsupportedOperationException.class, () -> volume.names().clear());
    }

    @Test
    void boundsCoverAllTheParts() {
        assertBounds(0, 0, 10, 6, twoParts());
        assertBounds(-3, -2, 5, 4, new CollisionBoundedVolume2D().put("a", rect(-3, -2, 2, 2)).put("b", rect(1, 0, 1, 2)));
    }

    @Test
    void boundsOfASinglePartAreThePart() {
        assertBounds(2, 3, 4, 5, new CollisionBoundedVolume2D().put("a", rect(2, 3, 4, 5)));
    }

    @Test
    void collidesWhenItsBoundsDo() {
        CollisionBoundedVolume2D volume = twoParts();
        assertTrue(volume.isColliding(rect(5, 1, 1, 1)), "between the parts but inside the bounds");
        assertTrue(volume.isColliding(rect(9, 3, 5, 5)));
        assertFalse(volume.isColliding(rect(10, 0, 5, 5)), "touching");
        assertFalse(volume.isColliding(rect(50, 50, 1, 1)));
    }

    @Test
    void collidesWithAnotherVolume() {
        CollisionBoundedVolume2D other = new CollisionBoundedVolume2D().put("x", rect(9, 5, 4, 4));
        assertTrue(twoParts().isColliding(other));
    }

    @Test
    void movingTheVolumeMovesEveryPart() {
        CollisionBoundedVolume2D volume = twoParts();

        volume.setX(5);
        volume.setY(-1);

        assertBounds(5, -1, 10, 6, volume);
        assertEquals(5f, ((CollisionRectangle) volume.get("a")).x(), 1e-5);
        assertEquals(13f, ((CollisionRectangle) volume.get("b")).x(), 1e-5);
        assertEquals(1f, ((CollisionRectangle) volume.get("b")).y(), 1e-5);
    }

    @Test
    void relativeMovesAddUp() {
        CollisionBoundedVolume2D volume = twoParts();

        volume.move(2, 3);
        volume.moveX(1);
        volume.moveY(-1);

        assertBounds(3, 2, 10, 6, volume);
    }

    @Test
    void settingTheSamePositionDoesNothing() {
        CollisionBoundedVolume2D volume = twoParts();
        volume.setX(0);
        volume.setY(0);
        volume.setPosition(Float.NaN, Float.NaN);
        assertBounds(0, 0, 10, 6, volume);
    }

    @Test
    void placedAgainstARectangleOnTheSideOfTheDirection() {
        CollisionRectangle reference = rect(20, 30, 10, 10);

        CollisionBoundedVolume2D right = twoParts();
        right.replaceComparedTo(reference, EDirection.Right);
        assertEquals(30f, right.bounds().x(), 1e-5);

        CollisionBoundedVolume2D left = twoParts();
        left.replaceComparedTo(reference, EDirection.Left);
        assertEquals(20f, left.bounds().oppX(), 1e-5, "the right side of the volume touches the left side of the reference");

        CollisionBoundedVolume2D up = twoParts();
        up.replaceComparedTo(reference, EDirection.Up);
        assertEquals(30f, up.bounds().oppY(), 1e-5);

        CollisionBoundedVolume2D down = twoParts();
        down.replaceComparedTo(reference, EDirection.Down);
        assertEquals(40f, down.bounds().y(), 1e-5);
    }

    @Test
    void aPlacedVolumeDoesNotOverlapItsReference() {
        CollisionRectangle reference = rect(20, 30, 10, 10);
        for (EDirection direction : new EDirection[]{EDirection.Left, EDirection.Right, EDirection.Up, EDirection.Down,
                EDirection.LeftUp, EDirection.RightDown}) {
            CollisionBoundedVolume2D volume = twoParts();
            volume.replaceComparedTo(reference, direction);
            assertFalse(volume.isColliding(reference), direction.toString());
        }
    }

    @Test
    void placedAgainstAnotherVolumeUsesItsBounds() {
        CollisionBoundedVolume2D reference = new CollisionBoundedVolume2D().put("a", rect(20, 30, 4, 4)).put("b", rect(26, 30, 4, 4));
        CollisionBoundedVolume2D volume = twoParts();

        volume.replaceComparedTo(reference, EDirection.Right);

        assertEquals(30f, volume.bounds().x(), 1e-5);
    }

    @Disabled("Known bug: copy() copies the list of parts but not the parts, moving the copy moves the original")
    @Test
    void copyIsIndependentFromTheOriginal() {
        CollisionBoundedVolume2D volume = twoParts();
        CollisionBoundedVolume2D copy = volume.copy();

        copy.moveX(100);

        assertBounds(0, 0, 10, 6, volume);
        assertBounds(100, 0, 10, 6, copy);
    }

    @Test
    void copyHasTheSamePartsAndItsOwnIdentity() {
        CollisionBoundedVolume2D volume = twoParts();
        CollisionBoundedVolume2D copy = volume.copy();

        assertNotSame(volume, copy);
        assertNotEquals(volume.id(), copy.id());
        assertEquals(volume.names(), copy.names());
        assertBounds(0, 0, 10, 6, copy);
    }
}
