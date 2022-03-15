package MightyLibrary.mightylib.resources.map;

public class Tilelayer {
    private final Tilemap tilemap;
    private final int[][] tiles;

    public Tilelayer(Tilemap tilemap){
        this.tilemap = tilemap;

        tiles = new int[tilemap.mapHeight()][tilemap.mapWidth()];
    }


    void setTileType(int x, int y, int type){
        tiles[y][x] = type;
    }

    public int getTile(int x, int y) {
        return tiles[y][x];
    }
}
