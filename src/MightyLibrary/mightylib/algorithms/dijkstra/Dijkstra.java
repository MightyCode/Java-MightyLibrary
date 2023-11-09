package MightyLibrary.mightylib.algorithms.dijkstra;

import MightyLibrary.mightylib.algorithms.graph.*;

import java.util.*;

public class Dijkstra {
    public static List<Node> findShortestPathReduced(ReducedGraph reducedGraph, Graph correspondingGraph,
                                                     Node startNode, Node endNode){
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

            for (Long id : currentIntersection.getIds()){
                System.out.println(id);
            }

            System.out.println("Next : " + next.getId());

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
        Map<Node, Float> distances = new HashMap<>();
        Map<Node, Node> previousNodes = new HashMap<>();
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));
        Set<Node> visited = new HashSet<>();

        for (Node node : graph.getNodes()) {
            distances.put(node, Float.MAX_VALUE);
            previousNodes.put(node, null);
        }

        distances.put(startNode, 0f);
        priorityQueue.offer(startNode);

        while (!priorityQueue.isEmpty()) {
            Node currentNode = priorityQueue.poll();

            if (currentNode.equals(endNode))
                return reconstructPath(previousNodes, endNode);

            visited.add(currentNode);

            for (Node neighbor : currentNode.getNodes()) {
                if (visited.contains(neighbor))
                    continue;

                float newDistance = distances.get(currentNode) + currentNode.getDist(neighbor);

                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    previousNodes.put(neighbor, currentNode);
                    priorityQueue.offer(neighbor);
                }
            }
        }

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