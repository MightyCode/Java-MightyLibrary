package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.SingleSourceDataType;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.TextureData;
import org.joml.Vector2i;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TileSet extends SingleSourceDataType {
    private String texture;
    private final Vector2i tileSize;
    private final Vector2i tileNumber;
    private boolean useRotation, useFlip;

    public static class TileAnimation {
        int refId;
        int[] ids;
        float[] times;

        public TileAnimation(int tileId, int frames){
            refId = tileId;
            ids = new int[frames];
            times = new float[frames];
        }

        public int getReferenceId(){
            return refId;
        }

        public float getTime(int frame){
            return times[frame];
        }

        public int getTileId(int frame){
            return ids[frame];
        }

        public int getFrameCount(){
            return ids.length;
        }
    }

    public Map<Integer, TileAnimation> animations;

    public TileSet(String dataName, String path) {
        super(dataName, path);

        tileSize = new Vector2i();
        tileNumber = new Vector2i();

        texture = "error";

        animations = new HashMap<>();

        reset();
    }

    public void setTileParameters(boolean useRotation, boolean useFlip){
        this.useRotation = useRotation;
        this.useFlip = useFlip;
    }

    public void addAnimation(int tileId, TileAnimation animation){
        animations.put(tileId, animation);
    }

    public TileAnimation getAnimation(int tileId){
        if (!animations.containsKey(tileId))
            return null;

        return animations.get(tileId);
    }

    public Collection<TileAnimation> getAnimations(){
        return animations.values();
    }

    public void setTileSize(String texture, Vector2i tileSize){
        this.tileSize.x = tileSize.x;
        this.tileSize.y = tileSize.y;

        this.texture = texture;

        TextureData text = Resources.getInstance().getResource(TextureData.class, texture);

        tileNumber.x = text.getWidth() / tileSize.x;
        tileNumber.y = text.getHeight() / tileSize.y;

        checkLoaded();
    }

    public Vector2i tilesNumberAxis() {
        return new Vector2i(tileNumber);
    }

    public int getTotalNumberTile(){
        int total = tileNumber.x * tileNumber.y;
        if (useRotation)
            total *= 4;

        if (useFlip)
            total *= 4;

        return total;
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
     * @return 0 : no rotation, 1 : 90° to the left, 2 : 180°, 3 90° to the right
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

        checkLoaded();
    }

    private void checkLoaded(){
        correctlyLoaded = tileNumber.x != 0 && tileNumber.y != 0;
        if (!correctlyLoaded)
            return;

        correctlyLoaded = !texture.equals("error");
    }


    @Override
    public void unload() {
        reset();
    }
}
