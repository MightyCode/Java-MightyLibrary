package io.github.mightycode.mightylib.algorithms.graph;

// Small graphs shared by the graph and path tests
public final class GraphFixtures {
    public static final long CENTER = 10, ARM_A1 = 11, ARM_A2 = 12, LEAF_A = 13, ARM_B1 = 21, LEAF_B = 22, LEAF_C = 31;

    private GraphFixtures() {
    }

    public static Node node(Graph graph, long id, float x, float y) {
        Node node = new Node(id, x, y);
        graph.add(node);
        return node;
    }

    public static void link(Node first, Node second) {
        first.add(second);
        second.add(first);
    }

    /**
     * <pre>
     *            22 (0,2)
     *             |
     *            21
     *             |
     * 31 ------- 10 ---- 11 ---- 12 ---- 13 (3,0)
     * </pre>
     * Intersections (reduced graph) : 10, 13, 22, 31. Sub nodes : 11, 12, 21.
     */
    public static Graph yShape() {
        Graph graph = new Graph();
        Node center = node(graph, CENTER, 0, 0);
        Node a1 = node(graph, ARM_A1, 1, 0);
        Node a2 = node(graph, ARM_A2, 2, 0);
        Node leafA = node(graph, LEAF_A, 3, 0);
        Node b1 = node(graph, ARM_B1, 0, 1);
        Node leafB = node(graph, LEAF_B, 0, 2);
        Node leafC = node(graph, LEAF_C, -1, 0);

        link(center, a1);
        link(a1, a2);
        link(a2, leafA);
        link(center, b1);
        link(b1, leafB);
        link(center, leafC);
        return graph;
    }
}
