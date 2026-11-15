package io.github.mightycode.mightylib.algorithms.graph;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    @Test
    void distanceIsEuclideanByDefault() {
        Node a = new Node(1, 0, 0), b = new Node(2, 3, 4);
        assertEquals(5f, a.getDist(b), 1e-6);
        assertEquals(5f, b.getDist(a), 1e-6);
    }

    @Test
    void anOverriddenDistanceWinsOverThePositionButOnlyInOneDirection() {
        Node a = new Node(1, 0, 0), b = new Node(2, 3, 4);
        a.add(b, 42f);

        assertEquals(42f, a.getDist(b));
        assertEquals(5f, b.getDist(a), 1e-6);
        assertTrue(a.contains(b));
        assertFalse(b.contains(a), "links are one way");
    }

    @Test
    void positionIsACopy() {
        Node node = new Node(1, 2, 3);
        Vector2f position = node.getPosition();
        position.set(99, 99);
        assertEquals(new Vector2f(2, 3), node.getPosition());
    }

    @Test
    void copyKeepsIdAndPositionButNotLinksNorObjectId() {
        Node node = new Node(7, 2, 3), neighbour = new Node(8, 0, 0);
        node.add(neighbour);

        Node copy = node.copy();
        assertEquals(7, copy.getId());
        assertEquals(new Vector2f(2, 3), copy.getPosition());
        assertEquals(0, copy.size());
        assertNotEquals(node.getObjectId(), copy.getObjectId());
    }

    @Test
    void positionInBoundariesMapsTheWorldToTheScreenWithTheYAxisReversed() {
        Vector4f world = new Vector4f(0, 0, 10, 10);
        // x = left/right, y = top/bottom of the screen area
        Vector4f screen = new Vector4f(100, 200, 200, 300);

        Vector2f middle = new Node(1, 5, 5).getPositionInBoundaries(world, screen);
        assertEquals(150f, middle.x, 1e-4);
        assertEquals(250f, middle.y, 1e-4);

        Vector2f origin = new Node(2, 0, 0).getPositionInBoundaries(world, screen);
        assertEquals(100f, origin.x, 1e-4);
        assertEquals(300f, origin.y, 1e-4, "world y = 0 is the bottom of the screen area");
    }
}
