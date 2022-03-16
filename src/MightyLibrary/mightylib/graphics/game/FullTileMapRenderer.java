package MightyLibrary.mightylib.graphics.game;

import MightyLibrary.mightylib.resources.map.TileMap;
import org.joml.Vector3f;

public class FullTileMapRenderer {
    private final TileMapRenderer forTileMapRenderer;
    private final TileMapRenderer backTileMapRenderer;


    public FullTileMapRenderer(String shader, boolean frequentlyUpdate) {
        forTileMapRenderer = new TileMapRenderer(shader, frequentlyUpdate, true);
        backTileMapRenderer = new TileMapRenderer(shader, frequentlyUpdate, false);

        forTileMapRenderer.setPosition(new Vector3f(100, 100, 0));
        forTileMapRenderer.setPosition(new Vector3f(100, 100, 0));
    }

    public void setTileMap(TileMap tilemap){
        forTileMapRenderer.setTilemap(tilemap);
        backTileMapRenderer.setTilemap(tilemap);
    }

    public void drawForLayers(){
        forTileMapRenderer.display();
    }

    public void drawBackLayers(){
        backTileMapRenderer.display();
    }

    public void unload(){
        forTileMapRenderer.unload();
        backTileMapRenderer.unload();
    }
}
