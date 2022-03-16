package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import org.joml.Vector2i;

public class Tileset extends DataType {
    private String texture;
    private Vector2i tileSize;

    public Tileset(String dataName, String path) {
        super(EDataType.TileSet, dataName, path);

        tileSize = new Vector2i();
    }


    public void setTexture(String texture){
        this.texture = texture;
    }


    public void setTileSize(Vector2i tileSize){
        this.tileSize.x = tileSize.x;
        this.tileSize.y = tileSize.y;
    }

    @Override
    public boolean unload() {
        return true;
    }

    public String texture(){ return texture; }

    public Vector2i tileSize() { return new Vector2i(tileSize); }
}
