package io.github.mightycode.mightylib.algorithms.dijkstra;

import io.github.mightycode.mightylib.algorithms.graph.Graph;
import io.github.mightycode.mightylib.algorithms.graph.Node;
import io.github.mightycode.mightylib.algorithms.graph.ReducedGraph;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.mightycode.mightylib.algorithms.graph.GraphFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class DijkstraTest {
    private static List<Long> ids(List<Node> path) {
        List<Long> result = new ArrayList<>();
        for (Node node : path)
            result.add(node.getId());
        return result;
    }

    @Test
    void theShortestPathIsNotTheOneWithTheFewestNodes() {
        Graph graph = new Graph();
        Node a = node(graph, 1, 0, 0), b = node(graph, 2, 10, 0), c = node(graph, 3, 1, 0), d = node(graph, 4, 2, 0);
        link(a, b);
        link(a, c);
        link(c, d);
        link(d, b);
        // a - b is 10 long, a - c - d - b is 1 + 1 + 8 = 10: the direct link is made longer by hand
        a.add(b, 50f);

        assertEquals(Arrays.asList(1L, 3L, 4L, 2L), ids(Dijkstra.findShortestPath(graph, a, b)));
    }

    @Test
    void theDirectLinkIsUsedWhenItIsTheShortest() {
        Graph graph = yShape();
        List<Node> path = Dijkstra.findShortestPath(graph, graph.getById(LEAF_C), graph.getById(CENTER));
        assertEquals(Arrays.asList(LEAF_C, CENTER), ids(path));
    }

    @Test
    void aPathAcrossSeveralBranches() {
        Graph graph = yShape();
        List<Node> path = Dijkstra.findShortestPath(graph, graph.getById(LEAF_A), graph.getById(LEAF_B));
        assertEquals(Arrays.asList(LEAF_A, ARM_A2, ARM_A1, CENTER, ARM_B1, LEAF_B), ids(path));
    }

    @Test
    void startEqualToEndGivesTheStartAlone() {
        Graph graph = yShape();
        Node start = graph.getById(CENTER);
        assertEquals(Arrays.asList(CENTER), ids(Dijkstra.findShortestPath(graph, start, start)));
    }

    @Test
    void unreachableEndGivesAnEmptyPath() {
        Graph graph = yShape();
        Node island = node(graph, 99, 50, 50);
        assertTrue(Dijkstra.findShortestPath(graph, graph.getById(CENTER), island).isEmpty());
    }

    @Test
    void linksAreFollowedInTheirDirectionOnly() {
        Graph graph = new Graph();
        Node a = node(graph, 1, 0, 0), b = node(graph, 2, 1, 0);
        a.add(b);

        assertEquals(Arrays.asList(1L, 2L), ids(Dijkstra.findShortestPath(graph, a, b)));
        assertTrue(Dijkstra.findShortestPath(graph, b, a).isEmpty());
    }

    @Test
    void anOverriddenDistanceChangesTheChosenWay() {
        Graph graph = new Graph();
        Node a = node(graph, 1, 0, 0), b = node(graph, 2, 1, 0), c = node(graph, 3, 0, 1);
        a.add(b, 100f);
        b.add(a, 100f);
        link(a, c);
        link(c, b);

        assertEquals(Arrays.asList(1L, 3L, 2L), ids(Dijkstra.findShortestPath(graph, a, b)));
    }

    // Reduced graph: the path goes through the sub nodes of the corridors

    private final ReducedGraph reduced = ReducedGraph.constructFrom(yShape());

    private List<Long> reducedPath(long start, long end) {
        Graph corresponding = reduced.createCorresponding();
        Node startNode = reduced.getById(start) != null ? reduced.getById(start).getReferenceNode()
                : reduced.getSubNodeGraph().getById(start).getReferenceNode();
        Node endNode = reduced.getById(end) != null ? reduced.getById(end).getReferenceNode()
                : reduced.getSubNodeGraph().getById(end).getReferenceNode();
        return ids(Dijkstra.findShortestPathReduced(reduced, corresponding, startNode, endNode));
    }

    @Test
    void reducedPathBetweenTwoIntersections() {
        assertEquals(Arrays.asList(LEAF_A, ARM_A2, ARM_A1, CENTER, ARM_B1, LEAF_B), reducedPath(LEAF_A, LEAF_B));
    }

    @Test
    void reducedPathFromASubNode() {
        assertEquals(Arrays.asList(ARM_A2, ARM_A1, CENTER, ARM_B1, LEAF_B), reducedPath(ARM_A2, LEAF_B));
    }

    @Test
    void reducedPathFromASubNodeTowardTheFarEndOfItsOwnCorridor() {
        assertEquals(Arrays.asList(ARM_A1, ARM_A2, LEAF_A), reducedPath(ARM_A1, LEAF_A));
    }

    @Test
    void reducedPathToASubNode() {
        assertEquals(Arrays.asList(LEAF_B, ARM_B1, CENTER, ARM_A1), reducedPath(LEAF_B, ARM_A1));
    }

    @Test
    void reducedPathBetweenTwoSubNodesOfDifferentCorridors() {
        assertEquals(Arrays.asList(ARM_A2, ARM_A1, CENTER, ARM_B1), reducedPath(ARM_A2, ARM_B1));
    }

    @Test
    void reducedPathToItselfIsEmpty() {
        assertTrue(reducedPath(CENTER, CENTER).isEmpty());
    }

    @Test
    void theTemporaryNodesOfASubNodeSearchAreRemovedFromTheCorrespondingGraph() {
        Graph corresponding = reduced.createCorresponding();
        int size = corresponding.size();
        int centerLinks = corresponding.getById(CENTER).size();
        int leafLinks = corresponding.getById(LEAF_A).size();

        Node start = reduced.getSubNodeGraph().getById(ARM_A1).getReferenceNode();
        Node end = reduced.getSubNodeGraph().getById(ARM_B1).getReferenceNode();
        assertFalse(Dijkstra.findShortestPathReduced(reduced, corresponding, start, end).isEmpty());

        assertEquals(size, corresponding.size());
        assertEquals(centerLinks, corresponding.getById(CENTER).size());
        assertEquals(leafLinks, corresponding.getById(LEAF_A).size());
        assertFalse(corresponding.containsId(ARM_A1));
    }

    @Test
    void aNodeUnknownToTheReducedGraphGivesAnEmptyPath() {
        Graph corresponding = reduced.createCorresponding();
        Node unknown = new Node(777, 0, 0);
        Node start = reduced.getById(CENTER).getReferenceNode();
        assertTrue(Dijkstra.findShortestPathReduced(reduced, corresponding, start, unknown).isEmpty());
    }
}
