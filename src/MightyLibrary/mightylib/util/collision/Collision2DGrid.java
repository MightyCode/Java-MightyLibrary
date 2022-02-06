package MightyLibrary.mightylib.util.collision;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.*;

public class Collision2DGrid {
    private final Set<Collision2D> allCollisions;

    private final Vector2f mapSize;
    private final Vector2f cellsSize;

    private final Vector2i cellsNumber;

    private Set<Collision2D>[][] constructedArea;

    private final Set<Collision2D> returnGrid;

    public Collision2DGrid(){
        allCollisions = new HashSet<>();
        mapSize = new Vector2f();
        cellsSize = new Vector2f();
        cellsNumber = new Vector2i();
        returnGrid = new HashSet<>();
    }

    public void init(Vector2f mapSize, Vector2f cellsSize, Set<Collision2D> collisions){
        clear();

        this.allCollisions.addAll(collisions);
        this.mapSize.x = mapSize.x;
        this.mapSize.y = mapSize.y;

        this.cellsSize.x = cellsSize.x;
        this.cellsSize.y = cellsSize.y;

        this.cellsNumber.x = (int)Math.ceil(mapSize.x / cellsSize.x);
        this.cellsNumber.y = (int)Math.ceil(mapSize.y / cellsSize.y);

        System.out.println("--Initialize collision grid, size(" + mapSize.x + "," + mapSize.y + "), cellNumber (" + cellsNumber.x + "," + cellsNumber.y + ")");

        construct();
    }

    private void construct(){
        this.returnGrid.clear();
        this.constructedArea = new HashSet[cellsNumber.y][cellsNumber.x];

        Vector2f cellSize = new Vector2f();
        CollisionRectangle tmp = new CollisionRectangle(0, 0, 0, 0);

        for (int y = 0; y < cellsNumber.y; ++y){
            for (int x = 0; x < cellsNumber.x; ++x){
                this.constructedArea[y][x] = new HashSet<>();

                setTmpAreaSize(cellSize, x, y);

                tmp.setPosition(x * cellSize.x, y * cellSize.y);
                tmp.setSize(cellSize.x, cellSize.y);

                for (Collision2D collision : allCollisions){
                    if (collision.isColliding(tmp)){
                        constructedArea[y][x].add(collision);
                    }
                }
            }
        }
    }

    private void setTmpAreaSize(Vector2f cellSize, int x, int y){
        cellSize.x = this.cellsSize.x;
        cellSize.y = this.cellsSize.y;

        if (x == cellsNumber.x - 1){
            cellSize.x = mapSize.x - (cellsNumber.x - 1) * cellsSize.x;
        }

        if (y == cellsNumber.y - 1){
            cellSize.y = mapSize.y - (cellsNumber.y - 1) * cellsSize.y;
        }
    }

    public void clear(){
        allCollisions.clear();
        constructedArea = new HashSet[0][0];
    }

    public Set<Collision2D> getCollisionNear(Collision2D a){
        CollisionRectangle tmp = a.bounds();

        Vector2i from = new Vector2i((int)(tmp.x() / cellsSize.x), (int)(tmp.y() / cellsSize.y));
        Vector2i to = new Vector2i((int)(tmp.oppX() / cellsSize.x), (int)(tmp.oppY() / cellsSize.y));

        if (to.x < 0 || from.x >= cellsNumber.x || to.y < 0 || from.y >= cellsNumber.y)
            return new HashSet<>();

        if (from.x < 0)
            from.x = 0;
        if (from.y < 0)
            from.y = 0;
        if (to.x >= cellsNumber.x)
            to.x = cellsNumber.x - 1;
        if (to.y >= cellsNumber.y)
            to.y = cellsNumber.y - 1;

       returnGrid.clear();

        for (int y = from.y; y <= to.y; ++y){
            for (int x = from.x; x <= to.x; ++x){
                returnGrid.addAll(constructedArea[y][x]);
            }
        }

        return returnGrid;
    }
}
