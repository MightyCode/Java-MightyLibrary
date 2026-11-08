package io.github.mightycode.mightylib.resources.map;

import io.github.mightycode.mightylib.resources.*;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Map;

public class TileMapLoader extends ResourceLoader {

    @Override
    public Class<? extends DataType> getType() {
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
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof TileMap))
            return;

        TileMap tileMap = (TileMap) dataType;

        String path = tileMap.path();
        String data = FileMethods.readFileAsString(path);
        String[] parts = data.split("\n");
        int lineIndex = 0;

        Vector2i layerSize = null;
        int numberOfBackLayers = 0, numberOfForLayers = 0;

        TileLayer[] layers = null;

        ArrayList<String> layersCategories = new ArrayList<>();
        ArrayList<Integer> layersCategoriesBeginIndex = new ArrayList<>();

        while (lineIndex < parts.length) {
            String[] linePart = parts[lineIndex].trim().split(" ");

            ++lineIndex;
            int lineNumber = lineIndex;
            if (linePart.length == 0)
                continue;

            if (linePart[0].equals("tileset")) {
                String tilesetName = TextFormat.word(linePart, 1, path, lineNumber, "tileset name");
                TileSet tileset = Resources.getInstance().getResource(TileSet.class, tilesetName);
                int startId = 0;

                if (linePart.length >= 3)
                    startId = TextFormat.integer(linePart, 2, path, lineNumber, "first tile id") - 1;

                tileMap.addTileset(tileset, startId);
            } else if (linePart[0].equals("layerSize")) {
                layerSize = new Vector2i(
                        TextFormat.integer(linePart, 1, path, lineNumber, "map width"),
                        TextFormat.integer(linePart, 2, path, lineNumber, "map height"));
                if (layerSize.x < 0 || layerSize.y < 0)
                    throw TextFormat.error(path, lineNumber, "the map size can't be negative");
            } else if (linePart[0].equals("layerNumber")) {
                numberOfBackLayers = TextFormat.integer(linePart, 1, path, lineNumber, "number of back layers");
                numberOfForLayers = TextFormat.integer(linePart, 2, path, lineNumber, "number of front layers");
                if (numberOfBackLayers < 0 || numberOfForLayers < 0)
                    throw TextFormat.error(path, lineNumber, "the number of layers can't be negative");

                layers = new TileLayer[numberOfBackLayers + numberOfForLayers];
            } else if (linePart[0].equals("layerCategory")) {
                layersCategories.add(TextFormat.word(linePart, 1, path, lineNumber, "category name"));
                if (linePart.length >= 3)
                    layersCategoriesBeginIndex.add(TextFormat.integer(linePart, 2, path, lineNumber, "category first layer"));
                else
                    layersCategoriesBeginIndex.add(0);

            } else if (linePart[0].equals("layer")) {
                int layerNumber = TextFormat.integer(linePart, 1, path, lineNumber, "layer number");

                if (layers == null || layerSize == null)
                    throw TextFormat.error(path, lineNumber,
                            "the map size and the number of layers must come before the layers");
                if (layerNumber < 0 || layerNumber >= layers.length)
                    throw TextFormat.error(path, lineNumber, "layer " + layerNumber
                            + " is out of range (" + layers.length + " layers)");

                layers[layerNumber] = new TileLayer(layerSize);

                for (int y = 0; y < layerSize.y; ++y) {
                    if (lineIndex >= parts.length)
                        throw TextFormat.error(path, parts.length, "layer " + layerNumber
                                + " needs " + layerSize.y + " rows, found " + y);

                    String[] Xs = parts[lineIndex].trim().split(" ");
                    if (Xs.length < layerSize.x)
                        throw TextFormat.error(path, lineIndex + 1, "row needs " + layerSize.x
                                + " tiles, found " + Xs.length);

                    for (int x = 0; x < layerSize.x; ++x) {
                        layers[layerNumber].setTileType(x, y,
                                TextFormat.integer(Xs, x, path, lineIndex + 1, "tile id") - 1);
                    }

                    lineIndex += 1;
                }
            }
        }

        if (layers == null || layerSize == null)
            throw new ResourceException("bad format in " + path + ": missing layerSize or layerNumber");

        tileMap.setInformation(layerSize, layers,
                layersCategories.toArray(new String[0]),
                layersCategoriesBeginIndex.stream().mapToInt(i -> i).toArray());
    }
}

