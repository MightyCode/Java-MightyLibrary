package MightyLibrary.mightylib.algorithms.graph;

/**
 * Class that represent, in a context of a reduced graph, a sub node
 */
public class NodeSubIntersection extends NodeContainer<Node, NodeIntersection> {

    // Neighbour can be null;
    public NodeSubIntersection(Node node, NodeIntersection parent1, NodeIntersection parent2) {
        super(node);

        super.add(parent1);
        super.add(parent2);
    }
    public NodeIntersection getParent1(){
        return get(0);
    }

    public NodeIntersection getParent2(){
        return get(1);
    }

    @Override
    public boolean add(NodeIntersection node){
        return false;
    }
}
