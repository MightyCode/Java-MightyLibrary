package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.*;
import org.joml.Vector2i;

import java.io.File;
import java.util.Map;
import java.util.Objects;

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
        create(data, "resources/tilemap");
    }

    private void create(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new TileMap(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                create(data, path + "/" + childPath);
            }
        }
    }

    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof TileMap))
            return;

        TileMap tileMap = (TileMap) dataType;

        String data = FileMethods.readFileAsString(tileMap.getPath());
        String[] parts = data.split("\n");
        int index = 0;
        TileSet tileset = Resources.getInstance().getResource(TileSet.class, parts[index++]);

        String[] mSize = parts[index++].trim().split(" ");
        Vector2i mapSize = new Vector2i(Integer.parseInt(mSize[0]), Integer.parseInt(mSize[1]));

        String[] layerNumber = parts[index++].trim().split(" ");

        int numberOfBack = Integer.parseInt(layerNumber[0]);
        int numberOfFor = Integer.parseInt(layerNumber[1]);

        TileLayer[] layers = new TileLayer[numberOfBack + numberOfFor];

        for (int layer = 0; layer < layers.length; ++layer){
            layers[layer] = new TileLayer(mapSize);

            for (int y = 0; y < mapSize.y; ++y){
                String[] Xs = parts[index++].trim().split(" ");

                for (int x = 0; x < mapSize.x; ++x){
                    layers[layer].setTileType(x, y, Integer.parseInt(Xs[x]) - 1);
                }
            }
        }

        tileMap.init(tileset, mapSize, layers, numberOfBack);

        tileMap.setCorrectlyLoaded();
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        // Todo
    }
}

