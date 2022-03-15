package MightyLibrary.mightylib.graphics.game;

public class Tilelayer {
    private final Tilemap tilemap;
    private final int[][] tiles;

    public Tilelayer(Tilemap tilemap){
        this.tilemap = tilemap;

        tiles = new int[tilemap.mapHeight()][tilemap.mapWidth()];
    }


    public void setTileType(int x, int y, int type){
        tiles[y][x] = type;
    }
}
