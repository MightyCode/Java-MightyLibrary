package MightyLibrary.mightylib.algorithms.graph;

import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Class that represent a node that have a position in space
 * @param <T>
 */
public abstract class PositionListNode<T extends ListNode<?>> extends ListNode<T> {

    public PositionListNode(long id) {
        super(id);
    }

    public abstract Vector2f getPosition();

    public abstract Vector2f getPositionInBoundaries(Vector4f boundaries, Vector4f rendererDest);

    public abstract PositionListNode<T> copy();
}
