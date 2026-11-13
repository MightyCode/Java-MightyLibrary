package io.github.mightycode.mightylib.algorithms.astar;

import io.github.mightycode.mightylib.algorithms.EVerbosity;
import io.github.mightycode.mightylib.algorithms.graph.Graph;
import io.github.mightycode.mightylib.algorithms.graph.Node;

import java.util.*;

/**
 * A* on a Graph. The heuristic is the straight-line distance between node positions, so the path is the shortest
 * one as long as no link was given a fixed distance shorter than that straight line (Node.add(neighbour, distance)).
 */
public class AStar {
    public static List<Node> findShortestPath(Graph graph, Node startNode, Node endNode) {
        return findShortestPath(graph, startNode, endNode, EVerbosity.NONE);
    }

    public static List<Node> findShortestPath(Graph graph, Node startNode, Node endNode, EVerbosity verbosity) {
        Map<Node, Float> distances = new HashMap<>();
        Map<Node, Node> previousNodes = new HashMap<>();
        // Entries keep the estimate they were queued with, so the ordering never changes under the queue
        PriorityQueue<Map.Entry<Node, Float>> openSet = new PriorityQueue<>(Map.Entry.comparingByValue());
        Set<Node> closed = new HashSet<>();

        for (Node node : graph.getNodes())
            distances.put(node, Float.MAX_VALUE);

        distances.put(startNode, 0f);
        openSet.offer(new AbstractMap.SimpleImmutableEntry<>(startNode, heuristic(startNode, endNode)));

        int expanded = 0;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll().getKey();

            // Stale queue entry: the node was already expanded with a better distance
            if (!closed.add(current))
                continue;

            ++expanded;
            verbosity.print(EVerbosity.ALL, "A* expands node " + current.getId());

            if (current.equals(endNode)) {
                List<Node> path = reconstructPath(previousNodes, endNode);
                verbosity.print(EVerbosity.ESSENTIAL, "A* : path of " + path.size() + " nodes, "
                        + expanded + " nodes expanded");
                return path;
            }

            for (Node neighbour : current.getNodes()) {
                if (closed.contains(neighbour))
                    continue;

                float newDistance = distances.get(current) + current.getDist(neighbour);

                if (newDistance < distances.getOrDefault(neighbour, Float.MAX_VALUE)) {
                    distances.put(neighbour, newDistance);
                    previousNodes.put(neighbour, current);
                    openSet.offer(new AbstractMap.SimpleImmutableEntry<>(neighbour,
                            newDistance + heuristic(neighbour, endNode)));
                }
            }
        }

        verbosity.print(EVerbosity.ESSENTIAL, "A* : no path, " + expanded + " nodes expanded");
        return new ArrayList<>();
    }

    private static float heuristic(Node from, Node to) {
        return from.getPosition().distance(to.getPosition());
    }

    private static List<Node> reconstructPath(Map<Node, Node> previousNodes, Node endNode) {
        LinkedList<Node> path = new LinkedList<>();
        Node currentNode = endNode;

        while (currentNode != null) {
            path.addFirst(currentNode);
            currentNode = previousNodes.get(currentNode);
        }

        return new ArrayList<>(path);
    }
}
