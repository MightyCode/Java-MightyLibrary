package MightyLibrary.mightylib.algorithms.graph;

/**
 * Class that represent, in a context of a reduced graph, an intersection Node
 */
public class NodeIntersection extends NodeContainer<Node, NodeIntersectionList> {
    public NodeIntersection(Node node) {
        super(node);
    }

    public float getDist(NodeIntersection intersection){
        return getById(intersection.getId()).dist();
    }
}
