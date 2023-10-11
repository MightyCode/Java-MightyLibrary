package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import org.joml.Vector2i;

public class TileMap extends DataType {
    private final Vector2i mapSize;
    private TileLayer[] layers;

    private TileSet tileset;

    private int endBackLayer;

    private boolean updated;

    public TileMap(String dataName, String path) {
        super(dataName, path);
        this.mapSize = new Vector2i();

        reset();

        updated = false;
    }

    public void init(TileSet tileset, Vector2i mapSize, TileLayer[] layers, int endBackLayer){
        this.tileset = tileset;
        this.mapSize.set(mapSize);
        this.layers = layers.clone();
        this.endBackLayer = endBackLayer;
    }


    public Vector2i getMapSize() { return new Vector2i(mapSize); }

    public int mapHeight() { return mapSize.y; }

    public int mapWidth() { return mapSize.x; }

    public TileSet tileSet() { return tileset; }

    public int forlayerNumber () { return layers.length - endBackLayer; }

    public int backlayerNumber () { return endBackLayer; }

    public void dispose(){
        updated = false;
    }


    public void setTileType(int layerNumber, int x, int y, int type){
        layers[layerNumber].setTileType(x, y, type);

        updated = true;
    }

    public boolean updated(){
        return updated;
    }

    public int getTileType(boolean isFor, int layerNumber, int x, int y){
        if (isFor){
            if (layerNumber >= forlayerNumber())
                return -1;

            return layers[layerNumber + endBackLayer].getTile(x, y);
        }

        if (layerNumber >= endBackLayer)
            return -1;

        return layers[layerNumber].getTile(x, y);
    }


    public int numberOfNonEmptyTile(){
        int tileNumber = 0;

        for (int layer = 0; layer < layers.length; ++layer){
            for (int y = 0; y < mapSize.y; ++y){
                for (int x = 0; x < mapSize.x; ++x){
                    boolean isFor = layer >= endBackLayer;

                    if (getTileType(isFor, (isFor) ? layer - endBackLayer: layer, x, y) >= 0)
                        ++tileNumber;
                }
            }
        }

        return tileNumber;
    }

    void setCorrectlyLoaded(){ correctlyLoaded = true; }

    private void reset(){
        layers = null;
        tileset = null;

        mapSize.x = 0;
        mapSize.y = 0;

        endBackLayer = 0;

        correctlyLoaded = false;
    }

    @Override
    public void unload() {
        reset();
    }
}
