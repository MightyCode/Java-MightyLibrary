package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import MightyLibrary.mightylib.resources.FileMethods;
import org.joml.Vector2i;

public class Tileset extends DataType {
    private String texture;
    private Vector2i tileSize;

    public Tileset(String dataName, String path) {
        super(EDataType.TileSet, dataName, path);

        tileSize = new Vector2i();
    }

    @Override
    public boolean load() {
        String data = FileMethods.readFileAsString(path);
        String[] parts = data.split("\n");
        texture = parts[0];

        String[] sizePart = parts[1].trim().split(" ");
        tileSize.x = Integer.parseInt(sizePart[0]);
        tileSize.y = Integer.parseInt(sizePart[1]);

        return true;
    }

    @Override
    public boolean unload() {
        return true;
    }

    public String texture(){ return texture; }

    public Vector2i tileSize() { return new Vector2i(tileSize); }
}
