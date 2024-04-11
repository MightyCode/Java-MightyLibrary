package MightyLibrary.mightylib.algorithms.graph;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class GraphUtil {
    /**
     * Be sure that the majority (> 50 %) of nodes are connected with each others
     * @param graph The graph
     */
    public static Graph eliminateNotConnectedNodes(Graph graph){
        int numberNode = graph.size();

        Set<Node> allSeenNodes = new HashSet<>();

        for (Node node : graph.getNodes()){
            if (allSeenNodes.contains(node))
                continue;

            Set<Node> connectedNodes = new HashSet<>();
            Queue<Node> nodeToCheck = new LinkedList<>();
            nodeToCheck.add(node);

            while (!nodeToCheck.isEmpty()){
                Node current = nodeToCheck.poll();

                for (Node neighbour : current.getNodes()){
                    if (connectedNodes.add(neighbour)) {
                        nodeToCheck.add(neighbour);
                        allSeenNodes.add(neighbour);
                    }
                }
            }

            if (connectedNodes.size() > numberNode * 0.5f){
                Graph result = new Graph();
                result.addAll(connectedNodes);
                return result;
            }
        }

        return null;
    }
}
