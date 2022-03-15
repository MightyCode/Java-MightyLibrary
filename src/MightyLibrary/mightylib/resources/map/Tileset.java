package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;

public class Tileset extends DataType {
    public String texture;

    public Tileset(String dataName, String path) {
        super(EDataType.TileSet, dataName, path);
    }

    @Override
    public boolean load() {
        return false;
    }

    @Override
    public boolean unload() {
        return false;
    }
}
