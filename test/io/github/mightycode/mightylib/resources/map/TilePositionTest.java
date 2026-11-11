package io.github.mightycode.mightylib.resources.map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TilePositionTest {
    @Test
    void constructors() {
        TilePosition position = new TilePosition(1, 2, 3);
        assertEquals(1, position.x());
        assertEquals(2, position.y());
        assertEquals(3, position.layer());

        TilePosition origin = new TilePosition();
        assertEquals(0, origin.x() + origin.y() + origin.layer());

        TilePosition copy = new TilePosition(position);
        assertEquals(position, copy);
        assertNotSame(position, copy);
    }

    @Test
    void settersChainAndChangeOneCoordinate() {
        TilePosition position = new TilePosition().setX(4).setY(5).setLayer(6);
        assertEquals(new TilePosition(4, 5, 6), position);
    }

    @Test
    void equalityComparesTheThreeCoordinates() {
        TilePosition position = new TilePosition(1, 2, 3);
        assertEquals(position, new TilePosition(1, 2, 3));
        assertNotEquals(position, new TilePosition(9, 2, 3));
        assertNotEquals(position, new TilePosition(1, 9, 3));
        assertNotEquals(position, new TilePosition(1, 2, 9));
        assertNotEquals(position, null);
        assertNotEquals(position, "1,2,3");
    }

    @Disabled("Known bug: TilePosition overrides equals but not hashCode, equal positions are lost in a HashSet / HashMap")
    @Test
    void equalPositionsHaveTheSameHashCode() {
        assertEquals(new TilePosition(1, 2, 3).hashCode(), new TilePosition(1, 2, 3).hashCode());
        assertEquals(1, new HashSet<>(Arrays.asList(new TilePosition(1, 2, 3), new TilePosition(1, 2, 3))).size());
    }

    @Test
    void positionsAreOrderedByLayerThenRowThenColumn() {
        TilePosition first = new TilePosition(5, 5, 0), second = new TilePosition(0, 0, 1);
        assertTrue(first.compareTo(second) < 0, "the layer first");

        assertTrue(new TilePosition(9, 1, 0).compareTo(new TilePosition(0, 2, 0)) < 0, "then the row");
        assertTrue(new TilePosition(1, 3, 0).compareTo(new TilePosition(2, 3, 0)) < 0, "then the column");
        assertEquals(0, new TilePosition(1, 3, 0).compareTo(new TilePosition(1, 3, 0)));
        assertTrue(second.compareTo(first) > 0);
    }

    @Test
    void sortingFollowsTheOrderOfTheMapFile() {
        List<TilePosition> positions = new ArrayList<>(Arrays.asList(
                new TilePosition(1, 1, 1), new TilePosition(0, 1, 0), new TilePosition(1, 0, 0), new TilePosition(0, 0, 1),
                new TilePosition(0, 0, 0)));

        Collections.sort(positions);

        assertEquals(Arrays.asList(
                new TilePosition(0, 0, 0), new TilePosition(1, 0, 0), new TilePosition(0, 1, 0),
                new TilePosition(0, 0, 1), new TilePosition(1, 1, 1)), positions);
    }

    @Test
    void cloneIsAnEqualButIndependentPosition() {
        TilePosition position = new TilePosition(1, 2, 3);
        TilePosition clone = position.clone();

        clone.setX(50);

        assertEquals(new TilePosition(1, 2, 3), position);
        assertEquals(new TilePosition(50, 2, 3), clone);
    }
}
