package MightyLibrary.mightylib.graphics.game;

import MightyLibrary.mightylib.resources.map.Tilemap;

public class FullTileMapRenderer {
    private final TileMapRenderer forTileMapRenderer;
    private final TileMapRenderer backTileMapRenderer;


    public FullTileMapRenderer(String shader, boolean frequentlyUpdate) {
        forTileMapRenderer = new TileMapRenderer(shader, frequentlyUpdate, true);
        backTileMapRenderer = new TileMapRenderer(shader, frequentlyUpdate, false);
    }

    public void setTileMap(Tilemap tilemap){
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
