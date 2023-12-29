package MightyLibrary.mightylib.graphics.game;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.resources.map.*;
import MightyLibrary.mightylib.resources.texture.TextureAtlas;
import MightyLibrary.mightylib.utils.math.EDirection;
import org.joml.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TileMapRenderer extends Renderer {
    private static class AnimatedTile {
        private float countTime;
        private int currentFrame;
        private int oldTileId;
        private final List<TilePosition> listConcernedTiles;

        private final TileSet.TileAnimation animation;

        public AnimatedTile(TileSet.TileAnimation animation) {
            this.animation = animation;
            listConcernedTiles = new ArrayList<>();

            countTime = 0;
            currentFrame = 0;
            oldTileId = -1;
        }

        public void addConcernedTile(TilePosition tile){
            listConcernedTiles.add(tile);
        }

        public void addConcernedTiles(Collection<TilePosition> tiles){
            listConcernedTiles.addAll(tiles);
        }

        public Collection<TilePosition> getConcernedTiles(){
            return listConcernedTiles;
        }

        public TileSet.TileAnimation getAnimationData(){
            return animation;
        }

        public void update(){
            countTime += GameTime.DeltaTime();
            oldTileId = animation.getTileId(currentFrame);

            while (countTime > animation.getTime(currentFrame)) {
                countTime -= animation.getTime(currentFrame);
                ++currentFrame;

                while (currentFrame < 0)
                    currentFrame += animation.getFrameCount();

                while(currentFrame >= animation.getFrameCount())
                    currentFrame -= animation.getFrameCount();
            }
        }

        public boolean hasBeenUpdated(){
            return oldTileId != animation.getTileId(currentFrame);
        }

        public int getCurrentTileId(){
            return animation.getTileId(currentFrame);
        }
    }

    private static final int NUMBER_INDICES = 4;
    private static final int SIZE_INDICES = 6;
    private static final int SIZE_COORDINATES = 8;
    private final int positionIndex;
    private final int textureIndex;
    private final Vector2f leftUpPosition;
    private EDirection reference;
    private TileMap tilemap;
    private TileSetAtlas tileSetAtlas;
    private TextureAtlas texture;
    private final String layerCategory;
    private final List<AnimatedTile> animatedTiles;

    private Vector2i tileSize;

    private Vector2i realMapSize;

    int[] tilesIndices = null;
    float[] tilesTexturePosition = null;
    float[] tilesPosition = null;
    int[] tilesCount = null;

    public TileMapRenderer(String shaderName, boolean willChange, String layerCategory) {
        super(shaderName, true);

        // Null let renderer's old setting for text
        this.reference = EDirection.LeftUp;

        this.leftUpPosition = new Vector2f(0, 0);

        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(new int[0]);

        positionIndex = shape.addVboFloat(new float[0],
                2, (willChange) ? Shape.DYNAMIC_STORE : Shape.STATIC_STORE);
        textureIndex = shape.addVboFloat(new float[0],
                2,  (willChange) ? Shape.DYNAMIC_STORE : Shape.STATIC_STORE);

        animatedTiles = new ArrayList<>();

        this.layerCategory = layerCategory;
    }

    public TileMapRenderer setReference(EDirection reference){
        if (reference == null)
            return this;

        this.reference = reference;
        this.loadTileMapInformation();

        return this;
    }

    public EDirection reference() {
        return this.reference;
    }

    public void setTilemap(TileMap tilemap) {
        reset();
        this.tilemap = tilemap;
        this.tileSetAtlas = tilemap.tileSetAtlas();
        this.texture = tileSetAtlas.getTextureAtlas();

        setMainTextureChannel(tileSetAtlas.getTextureAtlas());

        loadGeneralInformation();

        loadTileMapInformation();
    }

    public TileMap getTileMap(){
        return tilemap;
    }

    private void loadGeneralInformation() {
        animatedTiles.clear();
        for (int i = 0; i <  tileSetAtlas.getNumberTileset(); ++i){
            TileSet tileSet = tileSetAtlas.getTileSet(i);

            for (TileSet.TileAnimation animation : tileSet.getAnimations()) {
                animatedTiles.add(new AnimatedTile(animation));
            }
        }

        tileSize = tileSetAtlas.getTileSize();
        realMapSize = tilemap.getMapSize().mul(tileSize, new Vector2i());
    }

    private void loadTileMapInformation() {
        leftUpPosition.x = position.x;
        leftUpPosition.y = position.y;

        switch(this.reference){
            case None:
            case Up:
            case Down:
                leftUpPosition.x = (int)(-realMapSize.x * 0.5f);
                break;
            case RightDown:
            case Right:
            case RightUp:
                leftUpPosition.x = -realMapSize.x;
                break;
        }

        switch(this.reference){
            case None:
            case Left:
            case Right:
                leftUpPosition.y = (int)(realMapSize.y * 0.5f);
                break;
            case LeftDown:
            case Down:
            case RightDown:
                leftUpPosition.y = -realMapSize.y;
                break;
        }

        int tileNumber = tilemap.numberOfNonEmptyTile(layerCategory);
        int tileCount = 0;

        tilesIndices = new int[tileNumber * 6];
        tilesCount = new int[tileNumber];
        tilesTexturePosition = new float[tileNumber * 8];
        tilesPosition = new float[tileNumber * 8];

        int rawTileType;
        int tileType;

        int layerIndexFrom = tilemap.getLayerCategoryFrom(layerCategory);
        int layerIndexTo = tilemap.getLayerCategoryTo(layerCategory);

        for (int layer = layerIndexFrom; layer <= layerIndexTo; ++layer) {
            for (int y = 0; y < tilemap.mapHeight(); ++y) {
                for (int x = 0; x < tilemap.mapWidth(); ++x) {
                    rawTileType = tilemap.getTileType(layerCategory, layer, x, y);

                    if (rawTileType < 0)
                        continue;

                    int tileSetIndex = tileSetAtlas.getTileSetIndexRelatedTo(rawTileType);

                    if (tileSetIndex == -1)
                        continue;

                    rawTileType -= tileSetAtlas.getStartId(tileSetIndex);

                    TileSet tileSet = tileSetAtlas.getTileSet(tileSetIndex);

                    for (AnimatedTile animatedTile : animatedTiles) {
                        //System.out.println(animatedTile.getAnimationData().getReferenceId() + " " + rawTileType);
                        if (animatedTile.getAnimationData().getReferenceId() == rawTileType) {
                            //System.out.println("changed to " + animatedTile.getCurrentTileId());
                            rawTileType = animatedTile.getCurrentTileId();
                            animatedTile.addConcernedTile(new TilePosition(x, y, layer));
                        }
                    }

                    tileType = tileSet.getConvertedId(rawTileType);

                    setTilesIndices(tilesIndices, tileCount);

                    Vector4f rect = getTilePosition(x, y);

                    setTilePosition(tilesPosition, tileCount, rect);

                    Vector2i tileTexturePosition = getTilePosition(tileType, tileSet.tilesNumberAxis());

                    Vector4f tileTextureRect = getTileTextureRect(tileTexturePosition, tileSetIndex);

                    setTexturePosition(tilesTexturePosition, tileCount * SIZE_COORDINATES,
                            tileTextureRect,
                            tileSet.getTileRotation(rawTileType),
                            tileSet.getTileFlip(rawTileType));

                    ++tileCount;
                }
            }
        }

        shape.setEbo(tilesIndices);
        shape.updateVbo(tilesTexturePosition, textureIndex);
        shape.updateVbo(tilesPosition, positionIndex);
    }

    public void update(){
        boolean needUpdate = tilemap.hasBeenUpdated();

        for (AnimatedTile animatedTile : animatedTiles) {
            if (needUpdate)
                break;

            animatedTile.update();

            if (animatedTile.hasBeenUpdated()) {
                needUpdate = true;
                break;
            }
        }

        if (needUpdate)
            loadTileMapInformation();
    }

    private void setTilesIndices(int[] tilesIndices, int tileCount) {
        tilesIndices[tileCount * SIZE_INDICES] = tileCount * NUMBER_INDICES;
        tilesIndices[tileCount * SIZE_INDICES + 1] = tileCount * NUMBER_INDICES + 1;
        tilesIndices[tileCount * SIZE_INDICES + 2] = tileCount * NUMBER_INDICES + 2;
        tilesIndices[tileCount * SIZE_INDICES + 3] = tileCount * NUMBER_INDICES + 2;
        tilesIndices[tileCount * SIZE_INDICES + 4] = tileCount * NUMBER_INDICES;
        tilesIndices[tileCount * SIZE_INDICES + 5] = tileCount * NUMBER_INDICES + 3;
    }

    // X, Y from (x, z) -> X, Y to (y, w)
    private Vector4f getTilePosition(int x, int y) {
        return new Vector4f(
                x * tileSize.x,
                (x + 1) * tileSize.x - (leftUpPosition.x - position().x),
                y * tileSize.y,
                (y + 1) * tileSize.y - (leftUpPosition.y - position().y)
        );
    }

    private void setTilePosition(float[] tilesPosition, int tileCount, Vector4f rect) {
        tilesPosition[tileCount * SIZE_COORDINATES] = rect.x;
        tilesPosition[tileCount * SIZE_COORDINATES + 1] = rect.z;

        tilesPosition[tileCount * SIZE_COORDINATES + 2] = rect.x;
        tilesPosition[tileCount * SIZE_COORDINATES + 3] = rect.w;

        tilesPosition[tileCount * SIZE_COORDINATES + 4] = rect.y;
        tilesPosition[tileCount * SIZE_COORDINATES + 5] = rect.w;

        tilesPosition[tileCount * SIZE_COORDINATES + 6] = rect.y;
        tilesPosition[tileCount * SIZE_COORDINATES + 7] = rect.z;
    }

    private Vector2i getTilePosition(int tileType, Vector2i numberTile) {
        return new Vector2i(
                (tileType) % numberTile.x,
                (tileType) / numberTile.x
        );
    }

    private Vector4f getTileTextureRect(Vector2i tileTexturePosition, int tileSetIndex) {
        Vector2i tileSetPosition = tileSetAtlas.getTileSetPosition(tileSetIndex);

        return new Vector4f(
                (tileTexturePosition.x * 1.0f * tileSize.x + tileSetPosition.x) / texture.getWidth(),
                ((tileTexturePosition.x + 1.0f) * tileSize.x + tileSetPosition.x) / texture.getWidth(),
                (tileTexturePosition.y * 1.0f * tileSize.y + tileSetPosition.y) / texture.getHeight(),
                ((tileTexturePosition.y + 1.0f) * tileSize.y + tileSetPosition.y) / texture.getHeight()
        );
    }

    private void setTexturePosition(float[] texturePosition, int index, Vector4f originalPosition,
                                    int rotation, int flip) {
        float temp;

        if (flip == 1 || flip == 3){
             temp = originalPosition.x;
             originalPosition.x = originalPosition.y;
             originalPosition.y = temp;
        }

        if (flip == 2 || flip == 3){
            temp = originalPosition.z;
            originalPosition.z = originalPosition.w;
            originalPosition.w = temp;
        }

        switch (rotation) {
            default:
            case 0 :
                texturePosition[index++] = originalPosition.x;
                texturePosition[index++] = originalPosition.z;

                texturePosition[index++] = originalPosition.x;
                texturePosition[index++] = originalPosition.w;

                texturePosition[index++] = originalPosition.y;
                texturePosition[index++] = originalPosition.w;

                texturePosition[index++] = originalPosition.y;
                texturePosition[index++] = originalPosition.z;
            break;

            case 1:
                texturePosition[index++] = originalPosition.x;
                texturePosition[index++] = originalPosition.w;

                texturePosition[index++] = originalPosition.y;
                texturePosition[index++] = originalPosition.w;

                texturePosition[index++] = originalPosition.y;
                texturePosition[index++] = originalPosition.z;

                texturePosition[index++] = originalPosition.x;
                texturePosition[index++] = originalPosition.z;
            break;

            case 2:
                texturePosition[index++] = originalPosition.y;
                texturePosition[index++] = originalPosition.w;

                texturePosition[index++] = originalPosition.y;
                texturePosition[index++] = originalPosition.z;

                texturePosition[index++] = originalPosition.x;
                texturePosition[index++] = originalPosition.z;

                texturePosition[index++] = originalPosition.x;
                texturePosition[index++] = originalPosition.w;
            break;

            case 3:
                texturePosition[index++] = originalPosition.y;
                texturePosition[index++] = originalPosition.z;

                texturePosition[index++] = originalPosition.x;
                texturePosition[index++] = originalPosition.z;

                texturePosition[index++] = originalPosition.x;
                texturePosition[index++] = originalPosition.w;

                texturePosition[index++] = originalPosition.y;
                texturePosition[index++] = originalPosition.w;
        }
    }

    public void reset() {
        this.tilemap = null;
        this.tileSetAtlas = null;
        shape.setEbo(new int[0]);
        shape.updateVbo(new float[0], positionIndex);
        shape.updateVbo(new float[0], textureIndex);

        animatedTiles.clear();
    }
}