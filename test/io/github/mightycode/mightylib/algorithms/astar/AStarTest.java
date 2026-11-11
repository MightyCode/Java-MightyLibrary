package io.github.mightycode.mightylib.algorithms.astar;

import io.github.mightycode.mightylib.algorithms.EVerbosity;
import io.github.mightycode.mightylib.algorithms.OutputCapture;
import io.github.mightycode.mightylib.algorithms.dijkstra.Dijkstra;
import io.github.mightycode.mightylib.algorithms.graph.Graph;
import io.github.mightycode.mightylib.algorithms.graph.Node;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.mightycode.mightylib.algorithms.OutputCapture.lines;
import static io.github.mightycode.mightylib.algorithms.graph.GraphFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AStarTest {
    private static List<Long> ids(List<Node> path) {
        List<Long> result = new ArrayList<>();
        for (Node node : path)
            result.add(node.getId());
        return result;
    }

    @Test
    void aPathAcrossSeveralBranches() {
        Graph graph = yShape();
        List<Node> path = AStar.findShortestPath(graph, graph.getById(LEAF_A), graph.getById(LEAF_B));
        assertEquals(Arrays.asList(LEAF_A, ARM_A2, ARM_A1, CENTER, ARM_B1, LEAF_B), ids(path));
    }

    @Test
    void startEqualToEndGivesTheStartAlone() {
        Graph graph = yShape();
        Node start = graph.getById(CENTER);
        assertEquals(List.of(CENTER), ids(AStar.findShortestPath(graph, start, start)));
    }

    @Test
    void unreachableEndGivesAnEmptyPath() {
        Graph graph = yShape();
        Node island = node(graph, 99, 50, 50);
        assertTrue(AStar.findShortestPath(graph, graph.getById(CENTER), island).isEmpty());
    }

    @Test
    void sameLengthAsDijkstraOnAGrid() {
        Graph graph = new Graph();
        Node[][] grid = new Node[6][6];
        for (int y = 0; y < 6; ++y)
            for (int x = 0; x < 6; ++x)
                if (!(x == 3 && y < 5))
                    grid[y][x] = node(graph, y * 6 + x, x, y);
        for (int y = 0; y < 6; ++y)
            for (int x = 0; x < 6; ++x) {
                if (grid[y][x] == null)
                    continue;
                if (x + 1 < 6 && grid[y][x + 1] != null)
                    link(grid[y][x], grid[y][x + 1]);
                if (y + 1 < 6 && grid[y + 1][x] != null)
                    link(grid[y][x], grid[y + 1][x]);
            }

        List<Node> expected = Dijkstra.findShortestPath(graph, grid[0][0], grid[0][5]);
        List<Node> path = AStar.findShortestPath(graph, grid[0][0], grid[0][5]);
        assertFalse(path.isEmpty());
        assertEquals(expected.size(), path.size());
    }

    @Test
    void silentByDefaultAndTalkativeOnRequest() {
        Graph graph = yShape();
        Node a = graph.getById(LEAF_A), b = graph.getById(LEAF_B);

        assertEquals("", OutputCapture.of(() -> AStar.findShortestPath(graph, a, b)));
        assertEquals("", OutputCapture.of(() -> Dijkstra.findShortestPath(graph, a, b)));

        String essential = OutputCapture.of(() -> AStar.findShortestPath(graph, a, b, EVerbosity.ESSENTIAL));
        String all = OutputCapture.of(() -> AStar.findShortestPath(graph, a, b, EVerbosity.ALL));
        assertEquals(1, essential.split("\n").length);
        assertTrue(all.split("\n").length > 1);
    }

    @Test
    void aFixedDistanceShorterThanTheStraightLineCanFoolTheHeuristic() {
        Graph graph = new Graph();
        Node a = node(graph, 1, 0, 0), b = node(graph, 2, 10, 0), x = node(graph, 3, 0, 1);
        a.add(b, 3f);
        a.add(x);
        x.add(b, 0.1f);

        // Dijkstra finds a - x - b (1.1), A* expands b first through the direct link (3): documented limit
        assertEquals(Arrays.asList(1L, 3L, 2L), ids(Dijkstra.findShortestPath(graph, a, b)));
        assertEquals(Arrays.asList(1L, 2L), ids(AStar.findShortestPath(graph, a, b)));
    }
}
