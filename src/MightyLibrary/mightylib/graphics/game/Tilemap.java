package MightyLibrary.mightylib.graphics.game;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import org.joml.Vector2i;

import java.util.ArrayList;

public class Tilemap extends DataType {

    private final Vector2i mapSize;
    private final ArrayList<Tilelayer> forlayers;
    private final ArrayList<Tilelayer> backlayers;

    public Tilemap(String dataName, String path) {
        super(EDataType.TileMap, dataName, path);

        forlayers = new ArrayList<>();
        backlayers = new ArrayList<>();

        this.mapSize = new Vector2i();
    }


    @Override
    public boolean load() {
        return false;
    }



    public Tilelayer addForLayer(){
        Tilelayer forLayer = new Tilelayer(this);
        forlayers.add(forLayer);

        return forLayer;
    }

    public Tilelayer addBackLayer(){
        Tilelayer backlayer = new Tilelayer(this);
        backlayers.add(backlayer);

        return backlayer;
    }


    public Vector2i getMapSize() { return new Vector2i(mapSize); }

    public int mapHeight() { return mapSize.y; }

    public int mapWidth() { return mapSize.x; }

    @Override
    public boolean unload() {
        return false;
    }
}
