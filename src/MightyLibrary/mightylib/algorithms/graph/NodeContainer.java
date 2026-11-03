package MightyLibrary.mightylib.algorithms.graph;

import org.joml.Vector2f;
import org.joml.Vector4f;

public class NodeContainer <K extends PositionListNode<?>, T extends ListNode<?>>
        extends PositionListNode<T> {
    private final K node;

    public NodeContainer(K node) {
        super(node.getId());

        this.node = node;
    }

    @Override
    public long getId(){
        return node.getId();
    }

    public K getReferenceNode(){
        return node;
    }

    @Override
    public Vector2f getPosition() {
        return node.getPosition();
    }

    @Override
    public Vector2f getPositionInBoundaries(Vector4f boundaries, Vector4f rendererDest) {
        return node.getPositionInBoundaries(boundaries, rendererDest);
    }

    public NodeContainer<K, T> copy(){
        return new NodeContainer<>(node);
    }
}
