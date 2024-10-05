package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.resources.texture.TextureData;
import MightyLibrary.mightylib.resources.texture.TextureDataAtlas;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class TileSetAtlas {
    private final TextureDataAtlas textureAtlas;
    private final List<TileSet> tilesets;
    private final List<Integer> startIds;

    public TileSetAtlas() {
        textureAtlas = new TextureDataAtlas("atlas");
        tilesets = new ArrayList<>();
        startIds = new ArrayList<>();
    }

    void addTileSet(TileSet tileset, int startId) {
        tilesets.add(tileset);
        startIds.add(startId);

        textureAtlas.addTexture(
                Resources.getInstance().getResource(TextureData.class, tileset.texture()));
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

    public Vector2i getTileSetPosition(int index){
        return textureAtlas.getTexturePosition(
                tilesets.get(index).texture()
        );
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
