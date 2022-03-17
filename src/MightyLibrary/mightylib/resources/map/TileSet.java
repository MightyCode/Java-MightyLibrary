package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import org.joml.Vector2i;

public class TileSet extends DataType {
    private String texture;
    private final Vector2i tileSize;

    public TileSet(String dataName, String path) {
        super(dataName, path);

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
