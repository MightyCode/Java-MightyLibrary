package io.github.mightycode.mightylib.algorithms.graph;

import org.junit.jupiter.api.Test;

import static io.github.mightycode.mightylib.algorithms.graph.GraphFixtures.link;
import static io.github.mightycode.mightylib.algorithms.graph.GraphFixtures.node;
import static org.junit.jupiter.api.Assertions.*;

class GraphUtilTest {
    @Test
    void keepsTheConnectedMajority() {
        Graph graph = new Graph();
        Node a = node(graph, 1, 0, 0), b = node(graph, 2, 1, 0), c = node(graph, 3, 2, 0), d = node(graph, 4, 3, 0);
        Node e = node(graph, 5, 10, 10), f = node(graph, 6, 11, 10);
        link(a, b);
        link(b, c);
        link(c, d);
        link(e, f);

        Graph result = GraphUtil.eliminateNotConnectedNodes(graph);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsId(1) && result.containsId(4));
        assertFalse(result.containsId(5));
    }

    @Test
    void theMajorityMayNotBeTheFirstComponent() {
        Graph graph = new Graph();
        Node small1 = node(graph, 1, 0, 0), small2 = node(graph, 2, 1, 0);
        Node a = node(graph, 3, 5, 5), b = node(graph, 4, 6, 5), c = node(graph, 5, 7, 5);
        link(small1, small2);
        link(a, b);
        link(b, c);

        Graph result = GraphUtil.eliminateNotConnectedNodes(graph);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsId(3));
    }

    @Test
    void noMajorityGivesNull() {
        Graph graph = new Graph();
        Node a = node(graph, 1, 0, 0), b = node(graph, 2, 1, 0), c = node(graph, 3, 5, 5), d = node(graph, 4, 6, 5);
        link(a, b);
        link(c, d);

        assertNull(GraphUtil.eliminateNotConnectedNodes(graph), "two halves: none is more than 50 %");
    }

    @Test
    void anIsolatedNodeIsNotAComponent() {
        Graph graph = new Graph();
        node(graph, 1, 0, 0);
        assertNull(GraphUtil.eliminateNotConnectedNodes(graph));
    }
}
