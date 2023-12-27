package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.resources.texture.TextureAtlas;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class TileSetAtlas {
    private final TextureAtlas textureAtlas;
    private final List<TileSet> tilesets;
    private final List<Integer> startIds;

    public TileSetAtlas(){
        textureAtlas = new TextureAtlas("atlas");
        tilesets = new ArrayList<>();
        startIds = new ArrayList<>();
    }

    void addTileSet(TileSet tileset, int startId){
        tilesets.add(tileset);
        startIds.add(startId);

        textureAtlas.addTexture(
                Resources.getInstance().getResource(Texture.class, tileset.texture()));
    }

    public int getTileSetAssociateTo(int tileId){
        for (int i = 0; i < tilesets.size(); i++) {
            if (tileId >= startIds.get(i)
                    && tileId < startIds.get(i) + tilesets.get(i).numberOfTile().x * tilesets.get(i).numberOfTile().y)
                return i;
        }

        return -1;
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
        return tilesets.get(index).numberOfTile();
    }

    public Vector2i getTileSize(){
        return tilesets.get(0).tileSize();
    }

    public TextureAtlas getTextureAtlas(){
        return textureAtlas;
    }

    public int getNumberTileset(){
        return tilesets.size();
    }

    public void clear(){
        tilesets.clear();
        startIds.clear();

        textureAtlas.clear();
    }
}
