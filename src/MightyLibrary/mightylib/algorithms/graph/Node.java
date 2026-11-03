package MightyLibrary.mightylib.algorithms.graph;

import MightyLibrary.mightylib.utils.math.MightyMath;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.HashMap;

/**
 * Node can only be connected to object of class that contain Node
 */
public class Node extends PositionListNode<Node> {
    // Geographic position
    protected Vector2f position;
    // If we want to override the distance to a node by a given value
    private final HashMap<Node, Float> overrideDistances;

    public Node(long id, float x, float y){
        super(id);

        overrideDistances = new HashMap<>();
        position = new Vector2f(x, y);
    }

    public float getDist(Node n){
        if (overrideDistances.containsKey(n))
            return overrideDistances.get(n);

        return getPosition().distance(n.getPosition());
    }

    /**
     * Convert the position inside the geographic boundaries to the screen position
     * @param boundaries geographic boundaries
     * @param rendererDest screen boundaries
     * @return converted value
     */
    @Override
    public Vector2f getPositionInBoundaries(Vector4f boundaries, Vector4f rendererDest){
        return new Vector2f(
                MightyMath.Mapf(position.x, boundaries.x, boundaries.z, rendererDest.x, rendererDest.z),

                MightyMath.Mapf(position.y, boundaries.y, boundaries.w, rendererDest.w, rendererDest.y)
        );
    }

    /**
     * Reimplementation of add method with distances
     * @param neighbour new neighbour node
     * @param distance fixed distance to the new node
     */
    public void add(Node neighbour, float distance){
        super.add(neighbour);

        overrideDistances.put(neighbour, distance);
    }

    @Override
    public Node copy() {
        return new Node(getId(), position.x, position.y);
    }

    @Override
    public Vector2f getPosition() {
        return new Vector2f(position);
    }
}
