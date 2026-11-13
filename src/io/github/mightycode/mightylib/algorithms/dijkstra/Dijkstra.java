package io.github.mightycode.mightylib.algorithms.dijkstra;

import io.github.mightycode.mightylib.algorithms.EVerbosity;
import io.github.mightycode.mightylib.algorithms.graph.*;

import java.util.*;

public class Dijkstra {
    public static List<Node> findShortestPathReduced(ReducedGraph reducedGraph, Graph correspondingGraph,
                                                     Node startNode, Node endNode){
        return findShortestPathReduced(reducedGraph, correspondingGraph, startNode, endNode, EVerbosity.NONE);
    }

    public static List<Node> findShortestPathReduced(ReducedGraph reducedGraph, Graph correspondingGraph,
                                                     Node startNode, Node endNode, EVerbosity verbosity){
        if (startNode == endNode)
            return new ArrayList<>();

        Node startIntersection = null, endIntersection = null;

        ReducedGraph.ReducedGraphSubNode reducedGraphSubNode = reducedGraph.getSubNodeGraph();

        boolean startAddedManually = false, endAddedManually = false;

        // If start Node is intersection
        if (correspondingGraph.containsId(startNode.getId()))
            startIntersection = correspondingGraph.getById(startNode.getId());
        // If not we add the node as an intersection
        else if (reducedGraph.getSubNodeGraph().containsId(startNode.getId())){
            startIntersection = findAndAdd(startNode, reducedGraph, correspondingGraph);
            startAddedManually = true;
        }

        // If end node is intersection
        if (correspondingGraph.containsId(endNode.getId()))
            endIntersection = correspondingGraph.getById(endNode.getId());
        // If not we add the node as an intersection
        else if (reducedGraph.getSubNodeGraph().containsId(endNode.getId())){
            endIntersection = findAndAdd(endNode, reducedGraph, correspondingGraph);
            endAddedManually = true;
        }

        // Debug case
        if (startIntersection == null || endIntersection == null){
            return new ArrayList<>();
        }

        List<Node> intersectionPath = findShortestPath(correspondingGraph, startIntersection, endIntersection);

        List<Node> result = new ArrayList<>();

        int start = 0;
        int end = intersectionPath.size() - 1;
        if (reducedGraph.containsId(startIntersection.getId())) {
            result.add(reducedGraph.getById(intersectionPath.get(0).getId()).getReferenceNode());
        } else {
            start = 1;
            NodeSubIntersection startSubNode = reducedGraphSubNode.getById(startNode.getId());
            NodeIntersection otherInter;
            NodeIntersectionList goal;

            if (startSubNode.getParent1().getId() == intersectionPath.get(1).getId()) {
                otherInter = startSubNode.getParent2();
                goal = otherInter.getById(startSubNode.getParent1().getId());
            } else {
                otherInter = startSubNode.getParent1();
                goal = otherInter.getById(startSubNode.getParent2().getId());
            }

            Long[] idsArray = goal.getIds().toArray(new Long[0]);

            boolean shouldBegin = false;
            for (Long aLong : idsArray) {
                if (aLong == startNode.getId())
                    shouldBegin = true;

                if (shouldBegin)
                    result.add(goal.getById(aLong).getReferenceNode());
            }

            result.add(goal.getReferenceNodeTo().getReferenceNode());
        }

        if (!reducedGraph.containsId(endIntersection.getId()))
            end = intersectionPath.size() - 2;

        Node current = intersectionPath.get(start), next;
        NodeIntersection currentIntersection;
        NodeIntersectionList nextIntersection;
        // ]0, size - 1[
        // don't include current, include next
        for (int i = start; i < end; ++i){
            next = intersectionPath.get(i + 1);

            currentIntersection = reducedGraph.getById(current.getId());

            if (verbosity.allows(EVerbosity.ALL)) {
                for (Long id : currentIntersection.getIds())
                    verbosity.print(EVerbosity.ALL, "Dijkstra reduced, link of " + current.getId() + " : " + id);
            }

            verbosity.print(EVerbosity.ALL, "Dijkstra reduced, next : " + next.getId());

            nextIntersection = currentIntersection.getById(next.getId());

            for (Long id : nextIntersection.getIds()){
                result.add(reducedGraphSubNode.getById(id).getReferenceNode());
            }

            result.add(nextIntersection.getReferenceNodeTo().getReferenceNode());

            current = next;
        }

        if (!reducedGraph.containsId(endIntersection.getId())){
            NodeSubIntersection endSubNode = reducedGraphSubNode.getById(endNode.getId());
            NodeIntersection otherInter;
            NodeIntersectionList goal;

            if (endSubNode.getParent1().getId() == intersectionPath.get(intersectionPath.size() - 2).getId()) {
                otherInter = endSubNode.getParent1();
                goal = otherInter.getById(endSubNode.getParent2().getId());
            } else {
                otherInter = endSubNode.getParent2();
                goal = otherInter.getById(endSubNode.getParent1().getId());
            }

            Long[] idsArray = goal.getIds().toArray(new Long[0]);

            boolean shouldContinue = true;
            for (int i = 0; shouldContinue && i < idsArray.length; ++i) {
                if (idsArray[i] == endNode.getId())
                    shouldContinue = false;

                result.add(goal.getById(idsArray[i]).getReferenceNode());
            }
        }

        if (startAddedManually) {
            for (Node neighbour : startIntersection.getNodes()){
                neighbour.remove(startIntersection);
            }

            correspondingGraph.remove(startIntersection);
        }

        if (endAddedManually){
            for (Node neighbour : endIntersection.getNodes()){
                neighbour.remove(endIntersection);
            }

            correspondingGraph.remove(endIntersection);
        }

        verbosity.print(EVerbosity.ESSENTIAL, "Dijkstra reduced : path of " + result.size() + " nodes, "
                + intersectionPath.size() + " intersections");

        return result;
    }

    public static Node findAndAdd(Node reference, ReducedGraph reducedGraph, Graph correspondingGraph){
        NodeSubIntersection endSubNode = reducedGraph.getSubNodeGraph().getById(reference.getId());

        Node result = new Node(reference.getId(), reference.getPosition().x, reference.getPosition().y);

        Node temp = correspondingGraph.getById(endSubNode.getParent1().getId());

        result.add(temp);
        temp.add(result);

        temp = correspondingGraph.getById(endSubNode.getParent2().getId());

        result.add(temp);
        temp.add(result);

        correspondingGraph.add(result);

        return result;
    }

    public static List<Node> findShortestPath(Graph graph, Node startNode, Node endNode) {
        return findShortestPath(graph, startNode, endNode, EVerbosity.NONE);
    }

    public static List<Node> findShortestPath(Graph graph, Node startNode, Node endNode, EVerbosity verbosity) {
        Map<Node, Float> distances = new HashMap<>();
        Map<Node, Node> previousNodes = new HashMap<>();
        // Entries keep the distance they were queued with, so the ordering never changes under the queue
        PriorityQueue<Map.Entry<Node, Float>> priorityQueue = new PriorityQueue<>(Map.Entry.comparingByValue());
        Set<Node> visited = new HashSet<>();

        for (Node node : graph.getNodes()) {
            distances.put(node, Float.MAX_VALUE);
            previousNodes.put(node, null);
        }

        distances.put(startNode, 0f);
        priorityQueue.offer(new AbstractMap.SimpleImmutableEntry<>(startNode, 0f));

        while (!priorityQueue.isEmpty()) {
            Node currentNode = priorityQueue.poll().getKey();

            // Stale queue entry: the node was already expanded with a better distance
            if (visited.contains(currentNode))
                continue;

            if (currentNode.equals(endNode)) {
                List<Node> path = reconstructPath(previousNodes, endNode);
                verbosity.print(EVerbosity.ESSENTIAL, "Dijkstra : path of " + path.size() + " nodes, "
                        + visited.size() + " nodes expanded");
                return path;
            }

            visited.add(currentNode);
            verbosity.print(EVerbosity.ALL, "Dijkstra expands node " + currentNode.getId());

            for (Node neighbor : currentNode.getNodes()) {
                if (visited.contains(neighbor))
                    continue;

                float newDistance = distances.get(currentNode) + currentNode.getDist(neighbor);

                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    previousNodes.put(neighbor, currentNode);
                    priorityQueue.offer(new AbstractMap.SimpleImmutableEntry<>(neighbor, newDistance));
                }
            }
        }

        verbosity.print(EVerbosity.ESSENTIAL, "Dijkstra : no path, " + visited.size() + " nodes expanded");
        return new ArrayList<>();
    }

    private static List<Node> reconstructPath(Map<Node, Node> previousNodes, Node endNode) {
        List<Node> path = new ArrayList<>();
        Node currentNode = endNode;

        while (currentNode != null) {
            path.add(0, currentNode);
            currentNode = previousNodes.get(currentNode);
        }

        return path;
    }
}