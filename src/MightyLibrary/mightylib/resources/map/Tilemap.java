package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.Resources;
import org.joml.Vector2i;

public class Tilemap extends DataType {
    private final Vector2i mapSize;
    private Tilelayer[] layers;

    private Tileset tileset;

    private int endBackLayer;

    public Tilemap(String dataName, String path) {
        super(EDataType.TileMap, dataName, path);

        this.mapSize = new Vector2i();

        endBackLayer = 0;
    }


    @Override
    public boolean load() {
        String data = FileMethods.readFileAsString(path);
        String[] parts = data.split("\n");
        int index = 0;
        tileset = Resources.getInstance().getResource(Tileset.class, parts[index++]);

        String[] mSize = parts[index++].trim().split(" ");
        mapSize.x = Integer.parseInt(mSize[0]);
        mapSize.y = Integer.parseInt(mSize[1]);

        String[] layerNumber = parts[index++].trim().split(" ");
        mapSize.x = Integer.parseInt(mSize[0]);
        mapSize.y = Integer.parseInt(mSize[1]);


        int numberOfBack = Integer.parseInt(layerNumber[0]);
        int numberOfFor = Integer.parseInt(layerNumber[1]);

        endBackLayer = numberOfBack;

        layers = new Tilelayer[numberOfBack + numberOfFor];

        for (int layer = 0; layer < numberOfBack; ++layer){
            layers[layer] = new Tilelayer(this);

            for (int y = 0; y < mapSize.y; ++y){
                String[] Xs = parts[index++].trim().split(" ");

                for (int x = 0; x < mapSize.x; ++x){
                    layers[layer].setTileType(x, y, Integer.parseInt(Xs[x]));
                }
            }
        }




        return true;
    }


    public Vector2i getMapSize() { return new Vector2i(mapSize); }

    public int mapHeight() { return mapSize.y; }

    public int mapWidth() { return mapSize.x; }

    public Tileset tileset() { return tileset; }


    public int forlayerNumber () { return layers.length - endBackLayer;  }

    public int backlayerNumber () { return endBackLayer;  }

    public int getTileType(boolean isFor, int layerNumber, int x, int y){
        if (isFor){
            if (layerNumber >= forlayerNumber())
                return -1;

            return layers[layerNumber + endBackLayer].getTile(x, y);
        }

        if (layerNumber >= endBackLayer)
            return -1;

        return layers[layerNumber].getTile(x, y);
    }


    public int numberOfNonEmptyTile(){
        int tileNumber = 0;

        for (int layer = 0; layer < layers.length; ++layer){
            for (int y = 0; y < mapSize.y; ++y){
                for (int x = 0; x < mapSize.x; ++x){
                    boolean isFor = layer >= endBackLayer;

                    if (getTileType(isFor, (isFor) ? layer - endBackLayer: layer, x, y) != 0)
                        ++tileNumber;
                }
            }
        }

        return tileNumber;
    }


    @Override
    public boolean unload() {
        return true;
    }
}
