package io.github.mightycode.mightylib.algorithms.graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.mightycode.mightylib.algorithms.graph.GraphFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ReducedGraphTest {
    private final ReducedGraph reduced = ReducedGraph.constructFrom(yShape());

    private static List<Long> ids(NodeIntersectionList list) {
        return new ArrayList<>(list.getIds());
    }

    @Test
    void onlyTheNodesThatAreNotCorridorsAreIntersections() {
        assertEquals(4, reduced.size());
        for (long id : new long[]{CENTER, LEAF_A, LEAF_B, LEAF_C})
            assertTrue(reduced.containsId(id), "intersection " + id);

        assertEquals(3, reduced.getSubNodeGraph().size());
        for (long id : new long[]{ARM_A1, ARM_A2, ARM_B1})
            assertTrue(reduced.getSubNodeGraph().containsId(id), "sub node " + id);
    }

    @Test
    void aSubNodeKnowsTheTwoIntersectionsAtTheEndsOfItsCorridor() {
        NodeSubIntersection sub = reduced.getSubNodeGraph().getById(ARM_B1);
        assertEquals(CENTER, sub.getParent1().getId());
        assertEquals(LEAF_B, sub.getParent2().getId());
        assertEquals(ARM_B1, sub.getId());
    }

    @Test
    void eachIntersectionHasOneListPerNeighbourIntersectionInTheTraveledOrder() {
        NodeIntersection center = reduced.getById(CENTER);
        assertEquals(3, center.size());
        assertEquals(Arrays.asList(ARM_A1, ARM_A2), ids(center.getById(LEAF_A)));
        assertEquals(List.of(ARM_B1), ids(center.getById(LEAF_B)));
        assertTrue(ids(center.getById(LEAF_C)).isEmpty(), "directly linked");

        NodeIntersection leaf = reduced.getById(LEAF_A);
        assertEquals(Arrays.asList(ARM_A2, ARM_A1), ids(leaf.getById(CENTER)), "the other way round");
    }

    @Test
    void theDistanceBetweenTwoIntersectionsFollowsTheCorridor() {
        NodeIntersection center = reduced.getById(CENTER);
        assertEquals(3f, center.getDist(reduced.getById(LEAF_A)), 1e-5);
        assertEquals(2f, center.getDist(reduced.getById(LEAF_B)), 1e-5);
        assertEquals(1f, center.getDist(reduced.getById(LEAF_C)), 1e-5);
    }

    @Test
    void correspondingGraphKeepsTheIntersectionsWithTheCorridorLengthsAsDistances() {
        Graph graph = reduced.createCorresponding();

        assertEquals(4, graph.size());
        Node center = graph.getById(CENTER);
        assertEquals(3, center.size());
        assertEquals(3f, center.getDist(graph.getById(LEAF_A)), 1e-5);
        assertEquals(2f, center.getDist(graph.getById(LEAF_B)), 1e-5);
        assertEquals(3f, graph.getById(LEAF_A).getDist(center), 1e-5);
        assertNotSame(reduced.getById(CENTER).getReferenceNode(), center, "the nodes are copies");
    }

    @Test
    void subGraphKeepsOnlyTheCorridorsLinkedTogether() {
        Graph graph = reduced.createCorrespondingSubGraph();

        assertEquals(3, graph.size());
        assertFalse(graph.containsId(CENTER));
        assertTrue(graph.getById(ARM_A1).contains(graph.getById(ARM_A2)));
        assertTrue(graph.getById(ARM_A2).contains(graph.getById(ARM_A1)));
        assertEquals(0, graph.getById(ARM_B1).size(), "a corridor of one node has no link");
    }

    @Test
    void aGraphWithoutCorridorHasNoSubNode() {
        Graph graph = new Graph();
        Node a = node(graph, 1, 0, 0), b = node(graph, 2, 1, 0), c = node(graph, 3, 2, 0);
        link(a, b);
        link(a, c);
        link(b, c);

        // every node has two neighbours: no intersection to start from
        ReducedGraph loop = ReducedGraph.constructFrom(graph);
        assertEquals(0, loop.size());
        assertEquals(0, loop.getSubNodeGraph().size());
    }

    @Test
    void aSingleStraightPathReducesToItsTwoEnds() {
        Graph graph = new Graph();
        Node a = node(graph, 1, 0, 0), b = node(graph, 2, 1, 0), c = node(graph, 3, 2, 0), d = node(graph, 4, 3, 0);
        link(a, b);
        link(b, c);
        link(c, d);

        ReducedGraph line = ReducedGraph.constructFrom(graph);

        assertEquals(2, line.size());
        assertEquals(2, line.getSubNodeGraph().size());
        assertEquals(Arrays.asList(2L, 3L), ids(line.getById(1).getById(4)));
    }
}
