package io.github.mightycode.mightylib.algorithms.graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ListNodeTest {
    private final Graph graph = new Graph();
    private final Node a = new Node(5, 0, 0);
    private final Node b = new Node(2, 1, 0);
    private final Node c = new Node(9, 2, 0);

    @Test
    void addKeepsTheInsertionOrderForIndexesAndTheIdOrderForNodes() {
        graph.add(a);
        graph.add(b);
        graph.add(c);

        assertEquals(3, graph.size());
        assertSame(a, graph.get(0));
        assertSame(b, graph.get(1));
        assertSame(c, graph.get(2));
        assertEquals(Arrays.asList(5L, 2L, 9L), new ArrayList<>(graph.getIds()));
        assertEquals(Arrays.asList(b, a, c), new ArrayList<>(graph.getNodes()), "the nodes are sorted by id");
    }

    @Test
    void lookups() {
        graph.add(a);
        graph.add(b);

        assertTrue(graph.contains(a));
        assertTrue(graph.containsId(2));
        assertFalse(graph.containsId(9));
        assertFalse(graph.contains(c));
        assertSame(b, graph.getById(2));
        assertNull(graph.getById(9));
        assertEquals(0, graph.indexOf(a));
        assertEquals(1, graph.indexOf(2));
        assertEquals(-1, graph.indexOf(c));
    }

    @Test
    void removeByNodeOrById() {
        graph.add(a);
        graph.add(b);
        graph.add(c);

        assertTrue(graph.remove(b));
        assertTrue(graph.removeById(9));
        assertEquals(1, graph.size());
        assertSame(a, graph.get(0));
        assertFalse(graph.remove(b), "already removed");
        assertFalse(graph.removeById(42));
    }

    @Test
    void addAllAndClear() {
        graph.addAll(Arrays.asList(a, b, c));
        assertEquals(3, graph.size());

        graph.clear();
        assertEquals(0, graph.size());
        assertFalse(graph.containsId(5));
    }

    @Test
    void objectIdsAreUniqueEvenWithTheSameId() {
        Node same = new Node(5, 0, 0);
        assertEquals(a.getId(), same.getId());
        assertNotEquals(a.getObjectId(), same.getObjectId());
    }

    @Test
    void edgesAndGraphAreContainersOfNodes() {
        Edges edges = new Edges(7);
        edges.add(a);
        assertEquals(7, edges.getId());
        assertEquals(-1, graph.getId());
        assertTrue(edges.contains(a));
    }

    @Test
    void addingTheSameNodeTwiceIsRefused() {
        assertTrue(graph.add(a));
        assertFalse(graph.add(a));
        assertEquals(1, graph.getIds().size());
    }

    @Test
    void clearEmptiesTheIdsToo() {
        graph.add(a);
        graph.clear();
        assertTrue(graph.getIds().isEmpty());
    }
}
