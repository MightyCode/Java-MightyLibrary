package MightyLibrary.mightylib.graphics.game;

import MightyLibrary.mightylib.resources.map.TileLayer;
import MightyLibrary.mightylib.resources.map.TileMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LayersTileMapRenderer {
    private final Map<String, TileMapRenderer> layerTileMapRenderer;
    private final String shader;
    private final boolean frequentlyUpdate;

    public LayersTileMapRenderer(String shader, boolean frequentlyUpdate) {
        layerTileMapRenderer = new HashMap<>();
        this.shader = shader;
        this.frequentlyUpdate = frequentlyUpdate;
    }

    public void setTileMap(TileMap tilemap) {
        unloadExistingRenderer();
        layerTileMapRenderer.clear();

        for (String category : tilemap.getLayersCategories()) {
            TileMapRenderer renderer = new TileMapRenderer(shader, frequentlyUpdate, category);
            renderer.setTilemap(tilemap);
            layerTileMapRenderer.put(category, renderer);
        }
    }

    public void update() {
        for (TileMapRenderer renderer : layerTileMapRenderer.values())
            renderer.update();
    }

    public void display(String category) {
        if (layerTileMapRenderer.containsKey(category)) {
            layerTileMapRenderer.get(category).display();
        }
    }

    public void display() {
        for (String key : layerTileMapRenderer.keySet())
            display(key);
    }

    public void displayDefault(){
        display(TileLayer.DEFAULT_CATEGORY_NAME);
    }
    public TileMapRenderer getTileMapRenderer(String category){
        return layerTileMapRenderer.get(category);
    }
    public Collection<String> getCategories(){
        return layerTileMapRenderer.keySet();
    }
    public boolean containsCategory(String category){
        return layerTileMapRenderer.containsKey(category);
    }
    public void unloadExistingRenderer() {
        for(TileMapRenderer renderer : layerTileMapRenderer.values())
            renderer.unload();
    }

    public void unload(){
        unloadExistingRenderer();
    }
}
