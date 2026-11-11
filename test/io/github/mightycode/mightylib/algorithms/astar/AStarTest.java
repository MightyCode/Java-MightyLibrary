package io.github.mightycode.mightylib.algorithms.astar;

import io.github.mightycode.mightylib.algorithms.EVerbosity;
import io.github.mightycode.mightylib.algorithms.dijkstra.Dijkstra;
import io.github.mightycode.mightylib.algorithms.graph.Graph;
import io.github.mightycode.mightylib.algorithms.graph.Node;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.mightycode.mightylib.algorithms.graph.GraphFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AStarTest {
    private static List<Long> ids(List<Node> path) {
        List<Long> result = new ArrayList<>();
        for (Node node : path)
            result.add(node.getId());
        return result;
    }

    private static String capture(Runnable run) {
        PrintStream old = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            run.run();
        } finally {
            System.setOut(old);
        }
        return out.toString();
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

        assertEquals("", capture(() -> AStar.findShortestPath(graph, a, b)));
        assertEquals("", capture(() -> Dijkstra.findShortestPath(graph, a, b)));

        String essential = capture(() -> AStar.findShortestPath(graph, a, b, EVerbosity.ESSENTIAL));
        String all = capture(() -> AStar.findShortestPath(graph, a, b, EVerbosity.ALL));
        assertEquals(1, essential.split("\n").length);
        assertTrue(all.split("\n").length > 1);
    }
}
