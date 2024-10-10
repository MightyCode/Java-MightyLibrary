package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.SingleSourceDataType;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TileMap extends SingleSourceDataType {
    public static class TileChange {
        private TilePosition position;

        private int from;
        private int to;

        public TileChange setPosition(TilePosition position){
            this.position = position;
            return this;
        }

        public TileChange setFrom(int from){
            this.from = from;
            return this;
        }

        public TileChange setTo(int to){
            this.to = to;
            return this;
        }

        public TilePosition getPosition() { return position; }

        public int getFrom() { return from; }
        public int getTo() { return to; }
    }

    // Correctly loaded serves to detect if save tile changes
    private final List<TileChange> tileChanges;

    private final Vector2i mapSize;
    private final TileSetAtlas tileSetAtlas;

    private TileLayer[] layers;

    private String[] layersCategories;
    private final HashMap<String, Integer> layersCategoriesFrom;
    private final HashMap<String, Integer> layersCategoriesTo;

    public TileMap(String dataName, String path) {
        this(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);
    }

    public TileMap(TYPE_SET_UP typeSetUp, String dataName, String path) {
        super(typeSetUp, dataName, path);
        this.mapSize = new Vector2i();
        this.tileSetAtlas = new TileSetAtlas();

        this.tileChanges = new ArrayList<>();

        layersCategoriesFrom = new HashMap<>();
        layersCategoriesTo = new HashMap<>();

        unload();
    }


    public void addTileset(TileSet tileset, int startId) {
        addDependency(tileset);
        tileSetAtlas.addTileSet(tileset, startId);
    }

    public void setInformation(Vector2i mapSize, TileLayer[] layers, String[] layersCategories, int[] layersCategoriesBeginIndex) {
        this.mapSize.set(mapSize);
        this.layers = layers.clone();

        if (layersCategories == null || layersCategories.length == 0){
            this.layersCategories = new String[]{TileLayer.DEFAULT_CATEGORY_NAME};
            this.layersCategoriesFrom.put(TileLayer.DEFAULT_CATEGORY_NAME, 0);
            this.layersCategoriesTo.put(TileLayer.DEFAULT_CATEGORY_NAME, layers.length - 1);
        } else {
            this.layersCategories = layersCategories.clone();

            assert layersCategories.length == layersCategoriesBeginIndex.length;

            for (int i = 0; i < layersCategories.length; ++i)
                if (i < layersCategories.length - 1) {
                    layersCategoriesFrom.put(layersCategories[i], layersCategoriesBeginIndex[i]);
                    layersCategoriesTo.put(layersCategories[i], layersCategoriesBeginIndex[i + 1] - 1);
                } else {
                    layersCategoriesFrom.put(layersCategories[i], layersCategoriesBeginIndex[i]);
                    layersCategoriesTo.put(layersCategories[i], layers.length - 1);
                }
        }
    }


    public Vector2i getMapSize() { return new Vector2i(mapSize); }

    public int mapHeight() { return mapSize.y; }

    public int mapWidth() { return mapSize.x; }

    public TileSetAtlas tileSetAtlas() { return tileSetAtlas; }

    public String[] getLayersCategories() { return layersCategories; }

    public int getLayerCategoryTo(String category) { return layersCategoriesTo.get(category); }
    public int getLayerCategoryFrom(String category) { return layersCategoriesFrom.get(category); }


    public void dispose(){
        tileChanges.clear();
    }

    public void setTileType(int layerNumber, int x, int y, int type){
        setTile(layerNumber, x, y, type);
    }

    private void setTile(int layerNumber, int x, int y, int type) {
        int previousType = layers[layerNumber].getTile(x, y);
        layers[layerNumber].setTileType(x, y, type);

        if (isLoaded()) {

            if (previousType != type)
                tileChanges.add(
                        new TileChange()
                                .setPosition(new TilePosition(x, y, layerNumber))
                                .setFrom(previousType)
                                .setTo(type));
        }
    }

    public boolean hasBeenUpdated(){
        return !tileChanges.isEmpty();
    }

    public Collection<TileChange> getTileChanges(){
        return tileChanges;
    }

    public int getTileType(String category, int layerNumber, int x, int y) {
        int from = layersCategoriesFrom.get(category);
        int to = layersCategoriesTo.get(category);

        int range = to - from;

        if (layerNumber > range)
            return -1;

        return layers[layerNumber + from].getTile(x, y);
    }


    public int numberOfNonEmptyTile(String category){
        int tileNumber = 0;

        int from = layersCategoriesFrom.get(category);
        int to = layersCategoriesTo.get(category);

        for (int layer = from; layer <= to; ++layer){
            for (int y = 0; y < mapSize.y; ++y){
                for (int x = 0; x < mapSize.x; ++x){
                    if (layers[layer].getTile(x, y) != -1)
                        ++tileNumber;
                }
            }
        }
        return tileNumber;
    }

    @Override
    protected boolean internLoad() {
        tileSetAtlas.transferTileSetTextureToAtlas();

        return layers != null && mapSize.x != 0 && mapSize.y != 0;
    }

    @Override
    public void internUnload() {
        layers = null;
        tileSetAtlas.clear();
        mapSize.x = 0;
        mapSize.y = 0;

        layersCategoriesFrom.clear();
        layersCategoriesTo.clear();
    }
}
