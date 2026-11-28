package io.github.mightycode.mightylib.resources.map;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.utils.Logger;
import org.joml.Vector2i;

import java.util.HashMap;

public final class TileMap extends SingleSourceDataType {
    // Increased at each tile changed and at each reload (main thread): readers compare it to the version they used
    private int tilesVersion;
    // Tile changes made by the code since the map was loaded or reloaded (a tile changed twice counts twice): lost by
    // a reload, which reports them
    private int tileChanges;

    private final Vector2i mapSize;
    private final TileSetAtlas tileSetAtlas;

    private TileLayer[] layers;

    private String[] layersCategories;
    private HashMap<String, Integer> layersCategoriesFrom;
    private HashMap<String, Integer> layersCategoriesTo;

    public TileMap(String dataName, String path) {
        this(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);
    }

    public TileMap(TYPE_SET_UP typeSetUp, String dataName, String path) {
        super(typeSetUp, dataName, path);
        this.mapSize = new Vector2i();
        this.tileSetAtlas = new TileSetAtlas(dataName);

        layersCategoriesFrom = new HashMap<>();
        layersCategoriesTo = new HashMap<>();

        unload();
    }

    // The texture of the atlas (shared with TileMapRenderer), sent once the map is loaded (its atlas built)
    @Override
    public Class<? extends GLElement> glElementType() {
        return Texture.class;
    }

    @Override
    public String glElementName() {
        return tileSetAtlas.getTextureAtlas().getDataName();
    }

    @Override
    public Texture createGLElement() {
        Texture texture = new Texture(tileSetAtlas.getTextureAtlas());
        // The atlas is not managed by Resources: a failed map must not keep a loading screen waiting
        texture.addResourceDependency(this);
        return texture;
    }

    @Override
    protected DataType createReloadCopy() {
        return new TileMap(getTypeSetUp(), getDataName(), path());
    }

    // The atlas objects stay the same (used by the renderers): only their content is exchanged.
    // The file wins: the changes made by the code to the previous version are lost, and reported.
    @Override
    protected void swapContent(DataType copy) {
        TileMap other = (TileMap) copy;

        int width = mapSize.x, height = mapSize.y;
        mapSize.set(other.mapSize);
        other.mapSize.set(width, height);

        tileSetAtlas.replaceContentWith(other.tileSetAtlas);

        TileLayer[] otherLayers = other.layers;
        other.layers = layers;
        layers = otherLayers;

        String[] otherCategories = other.layersCategories;
        other.layersCategories = layersCategories;
        layersCategories = otherCategories;

        HashMap<String, Integer> otherFrom = other.layersCategoriesFrom, otherTo = other.layersCategoriesTo;
        other.layersCategoriesFrom = layersCategoriesFrom;
        other.layersCategoriesTo = layersCategoriesTo;
        layersCategoriesFrom = otherFrom;
        layersCategoriesTo = otherTo;

        if (tileChanges > 0)
            Logger.Warn(Logger.CATEGORY_RESOURCES, getDataName() + " reloaded: " + tileChanges
                    + " tile changes made by the code are lost");
        tileChanges = 0;
        ++tilesVersion;
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


    public void setTileType(int layerNumber, int x, int y, int type){
        setTile(layerNumber, x, y, type);
    }

    private void setTile(int layerNumber, int x, int y, int type) {
        int previousType = layers[layerNumber].getTile(x, y);
        layers[layerNumber].setTileType(x, y, type);

        if (isLoaded() && previousType != type) {
            ++tileChanges;
            ++tilesVersion;
        }
    }

    /**
     * Increased at each tile changed (setTileType with another value, once loaded) and at each reload: a reader
     * keeps the version it built with and builds again when it differs, nothing to clear. See also getVersion (the
     * content read from the file, increased by the load and the reload only).
     */
    public int tilesVersion() {
        return tilesVersion;
    }

    // Tile changes made by the code since the load or the last reload (a reload loses them)
    int tileChanges() {
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
        return layers != null && mapSize.x != 0 && mapSize.y != 0 && tileSetAtlas.build(resources());
    }

    @Override
    public void internUnload() {
        layers = null;
        tileChanges = 0;
        tileSetAtlas.clear();
        mapSize.x = 0;
        mapSize.y = 0;

        layersCategoriesFrom.clear();
        layersCategoriesTo.clear();
    }
}
