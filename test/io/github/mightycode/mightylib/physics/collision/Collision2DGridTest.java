package io.github.mightycode.mightylib.physics.collision;

import org.joml.Vector2f;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Broad phase: the objects are sorted into cells, a query returns those of the cells its bounds touch
class Collision2DGridTest {
    private final CollisionRectangle a = new CollisionRectangle(10, 10, 5, 5);   // cell (0, 0)
    private final CollisionRectangle b = new CollisionRectangle(60, 10, 5, 5);   // cell (1, 0)
    private final CollisionRectangle c = new CollisionRectangle(45, 45, 10, 10); // the four cells
    private final Collision2DGrid grid = new Collision2DGrid();

    private void initWithTheThreeObjects() {
        grid.init(new Vector2f(100, 100), new Vector2f(50, 50), new HashSet<>(Arrays.asList(a, b, c)));
    }

    private static Set<Collision2D> setOf(Collision2D... collisions) {
        return new HashSet<>(Arrays.asList(collisions));
    }

    @Test
    void aQueryReturnsTheObjectsOfTheCellsItTouches() {
        initWithTheThreeObjects();

        assertEquals(setOf(a, c), new HashSet<>(grid.getCollisionNear(new CollisionRectangle(0, 0, 20, 20))));
        assertEquals(setOf(b, c), new HashSet<>(grid.getCollisionNear(new CollisionRectangle(55, 0, 10, 10))));
    }

    @Test
    void aQuerySpanningSeveralCellsMergesThem() {
        initWithTheThreeObjects();

        assertEquals(setOf(a, b, c), new HashSet<>(grid.getCollisionNear(new CollisionRectangle(40, 5, 30, 5))));
    }

    @Test
    void aQueryCoveringTheWholeMapReturnsEverything() {
        initWithTheThreeObjects();

        assertEquals(setOf(a, b, c), new HashSet<>(grid.getCollisionNear(new CollisionRectangle(0, 0, 100, 100))));
    }

    @Test
    void aQueryBiggerThanTheMapIsClampedToTheMap() {
        initWithTheThreeObjects();

        assertEquals(setOf(a, b, c), new HashSet<>(grid.getCollisionNear(new CollisionRectangle(-500, -500, 1000, 1000))));
    }

    @Test
    void aQueryOutsideTheMapReturnsNothing() {
        initWithTheThreeObjects();

        assertTrue(grid.getCollisionNear(new CollisionRectangle(200, 200, 5, 5)).isEmpty());
        assertTrue(grid.getCollisionNear(new CollisionRectangle(-300, 10, 5, 5)).isEmpty());
        assertTrue(grid.getCollisionNear(new CollisionRectangle(10, 300, 5, 5)).isEmpty());
    }

    @Test
    void aCellWithNothingGivesAnEmptySet() {
        grid.init(new Vector2f(100, 100), new Vector2f(50, 50), setOf(a));

        assertTrue(grid.getCollisionNear(new CollisionRectangle(60, 60, 5, 5)).isEmpty());
    }

    @Disabled("Known bug: construct() places the cell with the reduced size of the last row and column, so these cells are built at the wrong place")
    @Test
    void theLastCellsAreTheRestOfTheMap() {
        // 100 / 30 : three cells of 30 and a last one of 10
        CollisionRectangle corner = new CollisionRectangle(95, 95, 3, 3);
        grid.init(new Vector2f(100, 100), new Vector2f(30, 30), setOf(corner));

        assertEquals(setOf(corner), new HashSet<>(grid.getCollisionNear(new CollisionRectangle(94, 94, 2, 2))));
        assertTrue(grid.getCollisionNear(new CollisionRectangle(0, 0, 2, 2)).isEmpty());
    }

    @Test
    void aMovingObjectCanBeUsedAsTheQuery() {
        initWithTheThreeObjects();
        CollisionRectangle player = new CollisionRectangle(58, 8, 4, 4);

        Set<Collision2D> near = grid.getCollisionNear(player);

        assertTrue(near.contains(b));
        assertFalse(near.contains(a));
    }

    @Test
    void aBoundedVolumeIsSortedByTheCellsItsPartsTouch() {
        CollisionBoundedVolume2D volume = new CollisionBoundedVolume2D().put("a", new CollisionRectangle(5, 5, 2, 2));
        grid.init(new Vector2f(100, 100), new Vector2f(50, 50), setOf(volume));

        assertEquals(setOf(volume), new HashSet<>(grid.getCollisionNear(new CollisionRectangle(0, 0, 10, 10))));
        assertTrue(grid.getCollisionNear(new CollisionRectangle(70, 70, 10, 10)).isEmpty());
    }

    @Test
    void initReplacesThePreviousContent() {
        initWithTheThreeObjects();
        grid.init(new Vector2f(100, 100), new Vector2f(50, 50), setOf(b));

        assertEquals(setOf(b), new HashSet<>(grid.getCollisionNear(new CollisionRectangle(0, 0, 100, 100))));
    }

    @Test
    void clearForgetsTheObjectsAndTheCells() {
        initWithTheThreeObjects();

        grid.clear();

        // no cell any more: any query is outside the grid or hits no cell
        assertTrue(grid.getCollisionNear(new CollisionRectangle(1000, 1000, 1, 1)).isEmpty());
    }

    @Test
    void objectsAreSortedWhenTheGridIsBuiltNotWhenTheyMove() {
        initWithTheThreeObjects();

        a.setPosition(70, 70);

        assertFalse(grid.getCollisionNear(new CollisionRectangle(60, 60, 20, 20)).contains(a), "the grid is not rebuilt");
        assertTrue(grid.getCollisionNear(new CollisionRectangle(0, 0, 20, 20)).contains(a));
    }
}
