package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.*;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Map;

public class TileMapLoader extends ResourceLoader {

    @Override
    public Class<?> getType() {
        return TileMap.class;
    }

    @Override
    public String getResourceNameType() {
        return "TileMap";
    }

    @Override
    public void create(Map<String, DataType> data){
        exploreResourcesFile(data, Resources.FOLDER + "tilemap");
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new TileMap(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".tilemap"))
            return getFileName(path);

        return null;
    }

    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof TileMap))
            return;

        TileMap tileMap = (TileMap) dataType;

        String data = FileMethods.readFileAsString(tileMap.getPath());
        String[] parts = data.split("\n");
        //new Vector2i(Integer.parseInt(mSize[0]), Integer.parseInt(mSize[1]));
        int lineIndex = 0;

        Vector2i layerSize = null;
        int numberOfBackLayers = 0, numberOfForLayers = 0;

        TileLayer[] layers = null;

        ArrayList<String> layersCategories = new ArrayList<>();
        ArrayList<Integer> layersCategoriesBeginIndex = new ArrayList<>();

        while (lineIndex < parts.length) {
            String[] linePart = parts[lineIndex].trim().split(" ");

            ++lineIndex;
            if (linePart.length == 0)
                continue;

            if (linePart[0].equals("tileset")) {
                TileSet tileset = Resources.getInstance().getResource(TileSet.class, linePart[1]);
                int startId = 0;

                if (linePart.length >= 3)
                    startId = Integer.parseInt(linePart[2]) - 1;

                tileMap.addTileset(tileset, startId);
            } else if (linePart[0].equals("layerSize")) {
                layerSize = new Vector2i(Integer.parseInt(linePart[1]), Integer.parseInt(linePart[2]));
            } else if (linePart[0].equals("layerNumber")) {
                numberOfBackLayers = Integer.parseInt(linePart[1]);
                numberOfForLayers = Integer.parseInt(linePart[2]);

                layers = new TileLayer[numberOfBackLayers + numberOfForLayers];
            } else if (linePart[0].equals("layerCategory")) {
                layersCategories.add(linePart[1]);
                if (linePart.length >= 3)
                    layersCategoriesBeginIndex.add(Integer.parseInt(linePart[2]));
                else
                    layersCategoriesBeginIndex.add(0);

            } else if (linePart[0].equals("layer")) {
                int layerNumber = Integer.parseInt(linePart[1]);

                if (numberOfBackLayers == -1 || layerSize == null)
                    throw new RuntimeException("Tilemap file is not correctly formated, please define first map size"
                        + " number of layers");

                assert layers != null;
                layers[layerNumber] = new TileLayer(layerSize);

                for (int y = 0; y < layerSize.y; ++y) {
                    String[] Xs = parts[lineIndex].trim().split(" ");

                    for (int x = 0; x < layerSize.x; ++x) {
                        layers[layerNumber].setTileType(x, y, Integer.parseInt(Xs[x]) - 1);
                    }

                    lineIndex += 1;
                }
            }
        }

        assert layers != null;
        tileMap.init(layerSize, layers,
                layersCategories.toArray(new String[0]),
                layersCategoriesBeginIndex.stream().mapToInt(i -> i).toArray());
        tileMap.setCorrectlyLoaded();
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        TileMap tileMap = new TileMap(resourceName, resourcePath);
        load(tileMap);
        data.put(resourceName, tileMap);
    }
}

