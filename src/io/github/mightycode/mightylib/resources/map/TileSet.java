package io.github.mightycode.mightylib.resources.map;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.joml.Vector2i;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TileSet extends SingleSourceDataType {
    private String texture;
    private TextureData textureData;
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

    private Map<Integer, TileAnimation> animations;

    public TileSet(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);

        tileSize = new Vector2i();
        tileNumber = new Vector2i();

        texture = "error";

        animations = new HashMap<>();

        unload();
    }

    @Override
    protected DataType createReloadCopy() {
        return new TileSet(dataName, path);
    }

    @Override
    protected void swapContent(DataType copy) {
        TileSet other = (TileSet) copy;

        String otherTexture = other.texture;
        other.texture = texture;
        texture = otherTexture;

        TextureData otherTextureData = other.textureData;
        other.textureData = textureData;
        textureData = otherTextureData;

        swap(tileSize, other.tileSize);
        swap(tileNumber, other.tileNumber);

        boolean otherRotation = other.useRotation, otherFlip = other.useFlip;
        other.useRotation = useRotation;
        other.useFlip = useFlip;
        useRotation = otherRotation;
        useFlip = otherFlip;

        Map<Integer, TileAnimation> otherAnimations = other.animations;
        other.animations = animations;
        animations = otherAnimations;
    }

    private static void swap(Vector2i first, Vector2i second) {
        int x = first.x, y = first.y;
        first.set(second);
        second.set(x, y);
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

        // Unknown texture: the fallback one. The number of tiles is known once the texture is loaded (internLoad)
        textureData = Resources.getInstance().getResourceOrFallback(TextureData.class, texture);
        addDependency(textureData);
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

    // The texture data used (the fallback one for an unknown name), placed in the atlas of the maps
    TextureData textureData() { return textureData; }

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

    @Override
    protected boolean internLoad() {
        if (textureData == null || tileSize.x <= 0 || tileSize.y <= 0)
            return false;

        // A failed texture is replaced by the fallback: at least one tile, so the map can still be drawn
        TextureData usable = Resources.getInstance().usable(TextureData.class, textureData);
        tileNumber.x = Math.max(1, usable.getWidth() / tileSize.x);
        tileNumber.y = Math.max(1, usable.getHeight() / tileSize.y);
        return true;
    }


    @Override
    protected void internUnload() {
        useRotation = false;
        useFlip = false;
        texture = "error";
        textureData = null;

        tileNumber.x = 0;
        tileNumber.y = 0;
    }
}
