package io.github.mightycode.mightylib.resources.map;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.resources.texture.TextureDataAtlas;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class TileSetAtlas {
    // Content of the map, built with it and never registered in Resources (its textures are the map's dependencies)
    private final TextureDataAtlas textureAtlas;
    private List<TileSet> tilesets;
    private List<Integer> startIds;

    TileSetAtlas(String mapName) {
        textureAtlas = new TextureDataAtlas(DataType.TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT, "tilemap:" + mapName, null);
        tilesets = new ArrayList<>();
        startIds = new ArrayList<>();
    }

    // Reload of the map: takes the content of the new atlas, the same objects stay in use (renderers, GL texture)
    void replaceContentWith(TileSetAtlas newer) {
        List<TileSet> newerTilesets = newer.tilesets;
        newer.tilesets = tilesets;
        tilesets = newerTilesets;

        List<Integer> newerStartIds = newer.startIds;
        newer.startIds = startIds;
        startIds = newerStartIds;

        textureAtlas.replaceContentWith(newer.textureAtlas);
    }

    void addTileSet(TileSet tileset, int startId) {
        tilesets.add(tileset);
        startIds.add(startId);
    }

    // The tilesets are loaded (dependencies of the map), so are their textures or their fallback
    boolean build() {
        for (TileSet tileset : tilesets)
            textureAtlas.addTexture(tileset.textureData());

        return textureAtlas.build();
    }

    public int getTileSetIndexRelatedTo(int tileId){
        if (tileId == -1)
            return -1;

        int i;
        int endId;

        for (i = 0; i < tilesets.size(); i++){
            endId = startIds.get(i) + tilesets.get(i).getTotalNumberTile();

            if (tileId < endId)
                break;
        }

        if (i == tilesets.size())
            return -1;

        if (startIds.get(i) > tileId || tileId > startIds.get(i) + this.tilesets.get(i).getTotalNumberTile())
            return -1;

        return i;
    }

    public TileSet getTileSet(int index){
        return tilesets.get(index);
    }

    public Vector2i getTileSetPosition(int index) {
        // By the data added to the atlas: the fallback one for an unknown texture name
        return textureAtlas.getTexturePosition(tilesets.get(index).textureData().getDataName());
    }

    public int getStartId(int index){
        return startIds.get(index);
    }
    public Vector2i getTileSetSize(int index){
        return tilesets.get(index).tilesNumberAxis();
    }

    public Vector2i getTileSize(){
        return tilesets.get(0).tileSize();
    }

    public TextureDataAtlas getTextureAtlas(){
        return textureAtlas;
    }

    public int getNumberTileset(){
        return tilesets.size();
    }

    public void clear() {
        tilesets.clear();
        startIds.clear();

        textureAtlas.clear();
    }
}
