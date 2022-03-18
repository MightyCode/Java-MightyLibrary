package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.Resources;
import org.joml.Vector2i;

public class TileSet extends DataType {
    private String texture;

    private final Vector2i tileSize;
    private final Vector2i tileNumber;

    private boolean useRotation, useFlip;

    public TileSet(String dataName, String path) {
        super(dataName, path);

        tileSize = new Vector2i();
        tileNumber = new Vector2i();

        texture = "error";

        reset();
    }


    public void setTileParameters(boolean useRotation, boolean useFlip){
        this.useRotation = useRotation;
        this.useFlip = useFlip;
    }


    public void setTileSize(String texture, Vector2i tileSize){
        this.tileSize.x = tileSize.x;
        this.tileSize.y = tileSize.y;

        this.texture = texture;

        Texture text = Resources.getInstance().getResource(Texture.class, texture);

        tileNumber.x = text.getWidth() / tileSize.x;
        tileNumber.y = text.getHeight() / tileSize.y;
    }

    public Vector2i numberOfTile() {
        return new Vector2i(tileNumber);
    }

    public String texture(){ return texture; }

    public Vector2i tileSize() { return new Vector2i(tileSize); }

    public int getConvertedId(int id){
        if ((!useRotation && !useFlip) || id < 0)
            return id;

        if (useRotation && useFlip)
            return id / 16;

        return id / 4;
    }

    /**
     * Get the rotation information extracting via the id
     *
     * @param id id of the tile
     *
     * @return 0 : no rotation, 1 : 90° to the right, 2 : 180°, 3 90° to the left
     */
    public int getTileRotation(int id){
        if (!useRotation || id < 0)
            return 0;

        if (useFlip)
            return id % 16 % 4;

        return id % 4;
    }

    /**
     * Get the flip information extracting via the id
     *
     * @param id id of the tile
     *
     * @return 0 : no flip, 1 : 90° horizontal flip , 2 : 180° vertical flip, 3 90° both flips
     */
    public int getTileFlip(int id){
        if (!useFlip || id < 0)
            return 0;

        if (useRotation)
            return id % 16 / 4;

        return id % 4;
    }

    private void reset(){
        useRotation = false;
        useFlip = false;
        texture = "error";

        tileNumber.x = 0;
        tileNumber.y = 0;
    }

    @Override
    public boolean unload() {
        reset();

        return true;
    }
}
