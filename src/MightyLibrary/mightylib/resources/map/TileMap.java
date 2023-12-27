package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import org.joml.Vector2i;

public class TileMap extends DataType {
    private final Vector2i mapSize;
    private final TileSetAtlas tileSetAtlas;

    private TileLayer[] layers;

    private int endBackLayer;
    private boolean updated;

    public TileMap(String dataName, String path) {
        super(dataName, path);
        this.mapSize = new Vector2i();
        this.tileSetAtlas = new TileSetAtlas();

        reset();

        updated = false;
    }

    public void addTileset(TileSet tileset, int startId){
        tileSetAtlas.addTileSet(tileset, startId);
    }

    public void init(Vector2i mapSize, TileLayer[] layers, int endBackLayer) {
        this.mapSize.set(mapSize);
        this.layers = layers.clone();
        this.endBackLayer = endBackLayer;

        updated = true;
    }


    public Vector2i getMapSize() { return new Vector2i(mapSize); }

    public int mapHeight() { return mapSize.y; }

    public int mapWidth() { return mapSize.x; }

    public TileSetAtlas tileSetAtlas() { return tileSetAtlas; }

    public int forLayerNumber() { return layers.length - endBackLayer; }

    public int backLayerNumber() { return endBackLayer; }

    public void dispose(){
        updated = false;
    }


    public void setTileType(int layerNumber, int x, int y, int type){
        setTile(layerNumber, x, y, type);
    }

    private void setTile(int layerNumber, int x, int y, int type){
        layers[layerNumber].setTileType(x, y, type);
        updated = true;
    }

    public boolean hasBeenUpdated(){
        return updated;
    }

    public int getTileType(boolean isFor, int layerNumber, int x, int y){
        if (isFor){
            if (layerNumber >= forLayerNumber())
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
        tileSetAtlas.clear();
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
