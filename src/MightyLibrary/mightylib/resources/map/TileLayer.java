package MightyLibrary.mightylib.resources.map;

import org.joml.Vector2i;

public class TileLayer {
    public static String DEFAULT_CATEGORY_NAME = "Default";
    private final Vector2i mapSize;
    private final int[][] tiles;

    public TileLayer(Vector2i mapSize){
        this.mapSize = new Vector2i(mapSize);

        tiles = new int[mapSize.y][mapSize.x];
    }

    public void setTileType(int x, int y, int type){
        tiles[y][x] = type;
    }

    public int getTile(int x, int y) {
        return tiles[y][x];
    }

    public Vector2i mapSize(){
        return new Vector2i(mapSize);
    }
}
