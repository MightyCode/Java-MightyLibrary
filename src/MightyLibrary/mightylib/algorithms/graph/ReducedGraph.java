package MightyLibrary.mightylib.algorithms.graph;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Class that can only have intersection nodes
 */
public class ReducedGraph extends ListNode<NodeIntersection> {
    /**
     * Subclass that contains a reference to all sub intersection nodes.
     */
    public static class ReducedGraphSubNode extends ListNode<NodeSubIntersection>{
        public ReducedGraphSubNode() {
            super(-1);
        }
    }

    // Reference to the corresponding graph containing all sub nodes
    private final ReducedGraphSubNode subNodeGraph;

    private ReducedGraph() {
        super(-1);
        subNodeGraph = new ReducedGraphSubNode();
    }

    protected void init(Graph graph) {
        subNodeGraph.clear();

        Queue<Node> nodeToCheck = new LinkedList<>();
        List<Node> subNodes = new ArrayList<>();

        Node currentNode, nextNode, previousNode, tempNode;
        NodeIntersection currentIntersection, nextIntersection;
        NodeSubIntersection currentSubNode;

        for (Node node : graph.getNodes()) {
            if (node.size() != 2) {
                nodeToCheck.add(node);
                break;
            }
        }

        int total = 0;

        while (!nodeToCheck.isEmpty()) {
            currentNode = nodeToCheck.poll();

            if (containsId(currentNode.getId()))
                currentIntersection = getById(currentNode.getId());
            else {
                currentIntersection = new NodeIntersection(currentNode);
                add(currentIntersection);
            }

            for (Node neighbour : currentNode.getNodes()) {
                subNodes.clear();

                nextNode = neighbour;
                previousNode = currentNode;

                while (nextNode.size() == 2) {
                    if (!subNodes.contains(nextNode))
                        subNodes.add(nextNode);

                    tempNode = nextNode;

                    if (nextNode.get(0) == previousNode)
                        nextNode = nextNode.get(1);
                    else
                        nextNode = nextNode.get(0);

                    previousNode = tempNode;
                }

                // Next is the second intersection
                if (containsId(nextNode.getId()))
                    nextIntersection = getById(nextNode.getId());
                else {
                    nextIntersection = new NodeIntersection(nextNode);
                    add(nextIntersection);
                    nodeToCheck.add(nextNode);
                }

                NodeIntersectionList intersectionList = new NodeIntersectionList(currentIntersection, nextIntersection);

                for (Node subNode : subNodes){
                    if (subNodeGraph.containsId(subNode.getId()))
                        currentSubNode = subNodeGraph.getById(subNode.getId());
                    else {
                        currentSubNode = new NodeSubIntersection(
                                subNode,
                                currentIntersection, nextIntersection);
                        subNodeGraph.add(currentSubNode);
                    }

                    intersectionList.add(currentSubNode);

                    ++total;
                }

                currentIntersection.add(intersectionList);
            }
        }

        System.out.println((size() + total / 2) + " / " + graph.size());
    }

    /**
     * The class can only be constructed via that function.
     * @param graph referenced graph
     * @return constructed reduced graph.
     */
    public static ReducedGraph constructFrom(Graph graph) {
        ReducedGraph reducedGraph = new ReducedGraph();
        reducedGraph.init(graph);
        return reducedGraph;
    }

    public ReducedGraphSubNode getSubNodeGraph(){
        return subNodeGraph;
    }

    // Create a new graph using only intersection, will copy all nodes, should not use the original node
    // no object id of nodes will be different
    public Graph createCorresponding(){
        SortedMap<Long, Node> createdNode = new TreeMap<>();

        Graph graph = new Graph();
        Node model, correspondingModel, modelNeighbour, correspondingNeighbour;

        for (NodeIntersection intersection : getNodes()){
            model = intersection.getReferenceNode();

            if (createdNode.containsKey(model.getId())) {
                correspondingModel = createdNode.get(model.getId());
            } else {
                correspondingModel = model.copy();

                createdNode.put(model.getId(), correspondingModel);
            }

            for (NodeIntersectionList neighbour : intersection.getNodes()){
                modelNeighbour = neighbour.getReferenceNodeTo().getReferenceNode();

                if (createdNode.containsKey(modelNeighbour.getId())) {
                    correspondingNeighbour = createdNode.get(modelNeighbour.getId());
                } else {
                    correspondingNeighbour = modelNeighbour.copy();

                    createdNode.put(modelNeighbour.getId(), correspondingNeighbour);
                }

                correspondingModel.add(correspondingNeighbour, intersection.getDist(neighbour.getReferenceNodeTo()));
            }
        }

        graph.addAll(createdNode.values());

        return graph;
    }

    /**
     * Creates a graph that contains only the connection between intersection.
     * All the sub nodes will be eliminated.
     */
    public Graph createCorrespondingSubGraph(){
        SortedMap<Long, Node> createdNodes = new TreeMap<>();

        Graph graph = new Graph();
        Node previousNode, currentNode;
        for (NodeIntersection intersection : getNodes()){
            for (NodeIntersectionList neighbour : intersection.getNodes()){
                previousNode = null;
                if (intersection.getId() < neighbour.getId())
                    continue;

                for (Long id : neighbour.getIds()){
                    if (createdNodes.containsKey(id)){
                        currentNode = createdNodes.get(id);
                    } else {
                        currentNode = neighbour.getById(id).getReferenceNode().copy();
                        createdNodes.put(currentNode.getId(), currentNode);
                    }

                    if (previousNode != null){
                        previousNode.add(currentNode);
                        currentNode.add(previousNode);
                    }

                    previousNode = currentNode;
                }
            }
        }

        graph.addAll(createdNodes.values());

        return graph;
    }
}