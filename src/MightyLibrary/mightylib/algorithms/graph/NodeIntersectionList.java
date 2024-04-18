package MightyLibrary.mightylib.algorithms.graph;

/**
 * Class that also represent, in a context of a reduced graph, a connection between two intersection
 * and its sub nodes that create the path between them.
 */
public class NodeIntersectionList extends ListNode<NodeSubIntersection> {
    private final NodeIntersection from;
    private final NodeIntersection to;
    public NodeIntersectionList(NodeIntersection from, NodeIntersection to) {
        super(to.getId());
        this.from = from;
        this.to = to;
    }

    public NodeIntersection getReferenceNodeFrom(){
        return from;
    }

    public NodeIntersection getReferenceNodeTo(){
        return to;
    }

    public float dist(){
        if (size() == 0)
            return from.getReferenceNode().getDist(to.getReferenceNode());

        float result = from.getReferenceNode().getDist(get(0).getReferenceNode());
        result += to.getReferenceNode().getDist(get(size() - 1).getReferenceNode());

        for (int i = 0; i < size() - 1; ++i)
            result += get(i).getReferenceNode().getDist(get(i + 1).getReferenceNode());

        return result;
    }
}
