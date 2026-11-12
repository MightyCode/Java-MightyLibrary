package io.github.mightycode.mightylib.graphics.game;

import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.resources.map.*;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.*;

import java.util.ArrayList;
import java.util.List;

public class TileMapRenderer extends MeshRenderer {
    // Tile of the map using an animation: where it is in the buffers, to update only its texture position
    private static class AnimatedTileInstance {
        private final int bufferIndex;
        private final int tileSetIndex;

        public AnimatedTileInstance(int bufferIndex, int tileSetIndex) {
            this.bufferIndex = bufferIndex;
            this.tileSetIndex = tileSetIndex;
        }
    }

    private static class AnimatedTile {
        private float countTime;
        private int currentFrame;
        private int oldTileId;
        private final List<AnimatedTileInstance> concernedTiles;

        private final TileSet.TileAnimation animation;

        public AnimatedTile(TileSet.TileAnimation animation) {
            this.animation = animation;
            concernedTiles = new ArrayList<>();

            countTime = 0;
            currentFrame = 0;
            oldTileId = -1;
        }

        public void addConcernedTile(AnimatedTileInstance tile){
            concernedTiles.add(tile);
        }

        public void clearConcernedTiles(){
            concernedTiles.clear();
        }

        public List<AnimatedTileInstance> getConcernedTiles(){
            return concernedTiles;
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
    // Version of the map the tiles were built with
    private int tileMapVersion;
    // Texture of the atlas, and the tiles per axis of each tileset when the tiles were built
    private Texture atlasTexture;
    private Vector2i[] tileSetsNumber;
    private TileSetAtlas tileSetAtlas;
    private final String layerCategory;
    private final List<AnimatedTile> animatedTiles;

    private Vector2i tileSize;

    private Vector2i realMapSize;

    int[] tilesIndices = null;
    float[] tilesTexturePosition = null;
    float[] tilesPosition = null;

    public TileMapRenderer(String shaderName, boolean willChange, String layerCategory) {
        super(shaderName, true);

        // Null let renderer's old setting for text
        this.reference = EDirection.LeftUp;

        this.leftUpPosition = new Vector2f(0, 0);

        mesh.setEboStorage(Mesh.STATIC_STORE);
        mesh.setEbo(new int[0]);

        positionIndex = mesh.addVboFloat(new float[0],
                2, (willChange) ? Mesh.DYNAMIC_STORE : Mesh.STATIC_STORE);
        textureIndex = mesh.addVboFloat(new float[0],
                2,  (willChange) ? Mesh.DYNAMIC_STORE : Mesh.STATIC_STORE);

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
        this.tileMapVersion = tilemap.getVersion();
        this.tileSetAtlas = tilemap.tileSetAtlas();
        // The same element as the one a batch listing the map sends during the loading
        atlasTexture = glResources.addElementOrReturnIfPresent(Texture.class, tilemap.createGLElement(), tilemap.glElementName());
        setMainTextureChannel(atlasTexture);

        loadGeneralInformation();

        loadTileMapInformation();
    }

    public TileMap getTileMap(){
        return tilemap;
    }

    private void loadGeneralInformation() {
        animatedTiles.clear();
        tileSetsNumber = new Vector2i[tileSetAtlas.getNumberTileset()];
        for (int i = 0; i <  tileSetAtlas.getNumberTileset(); ++i){
            TileSet tileSet = tileSetAtlas.getTileSet(i);
            // Kept: a reloaded tileset changes it before the atlas texture that matches it is shown
            tileSetsNumber[i] = tileSet.tilesNumberAxis();

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
        tilesTexturePosition = new float[tileNumber * 8];
        tilesPosition = new float[tileNumber * 8];

        int rawTileType;
        int tileType;

        int layerIndexFrom = tilemap.getLayerCategoryFrom(layerCategory);
        int layerIndexTo = tilemap.getLayerCategoryTo(layerCategory);

        // Rebuilt from scratch below, otherwise the lists would grow at each reload
        for (AnimatedTile animatedTile : animatedTiles)
            animatedTile.clearConcernedTiles();

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
                            animatedTile.addConcernedTile(new AnimatedTileInstance(tileCount, tileSetIndex));
                        }
                    }

                    tileType = tileSet.getConvertedId(rawTileType);

                    setTilesIndices(tilesIndices, tileCount);

                    Vector4f rect = getTilePosition(x, y);

                    setTilePosition(tilesPosition, tileCount, rect);

                    Vector2i tileTexturePosition = getTilePosition(tileType, tileSetsNumber[tileSetIndex]);

                    Vector4f tileTextureRect = getTileTextureRect(tileTexturePosition, tileSetIndex);

                    setTexturePosition(tilesTexturePosition, tileCount * SIZE_COORDINATES,
                            tileTextureRect,
                            tileSet.getTileRotation(rawTileType),
                            tileSet.getTileFlip(rawTileType));

                    ++tileCount;
                }
            }
        }

        mesh.setEbo(tilesIndices);
        mesh.updateVbo(tilesTexturePosition, textureIndex);
        mesh.updateVbo(tilesPosition, positionIndex);
    }

    public void update(){
        // Reloaded: built again from the new version (same atlas objects), once the atlas texture shows it.
        // Meanwhile the previous tiles stay with the previous texture, the animations wait.
        if (tilemap.getVersion() != tileMapVersion) {
            if (atlasTexture.isLoaded() && !atlasTexture.isUpToDate())
                return;

            tileMapVersion = tilemap.getVersion();
            loadGeneralInformation();
            loadTileMapInformation();
        }

        boolean animationChanged = false;

        // Every animation must advance each tick, otherwise they drift apart
        for (AnimatedTile animatedTile : animatedTiles) {
            animatedTile.update();

            if (animatedTile.hasBeenUpdated()) {
                updateAnimatedTileTexture(animatedTile);
                animationChanged = true;
            }
        }

        // A map change needs a full rebuild, an animation only changes texture positions
        if (tilemap.hasBeenUpdated())
            loadTileMapInformation();
        else if (animationChanged)
            mesh.updateVbo(tilesTexturePosition, textureIndex);
    }

    private void updateAnimatedTileTexture(AnimatedTile animatedTile) {
        int currentTileId = animatedTile.getCurrentTileId();

        for (AnimatedTileInstance tile : animatedTile.getConcernedTiles()) {
            TileSet tileSet = tileSetAtlas.getTileSet(tile.tileSetIndex);

            Vector2i tileTexturePosition = getTilePosition(tileSet.getConvertedId(currentTileId), tileSetsNumber[tile.tileSetIndex]);
            Vector4f tileTextureRect = getTileTextureRect(tileTexturePosition, tile.tileSetIndex);

            setTexturePosition(tilesTexturePosition, tile.bufferIndex * SIZE_COORDINATES,
                    tileTextureRect,
                    tileSet.getTileRotation(currentTileId),
                    tileSet.getTileFlip(currentTileId));
        }
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
                (tileTexturePosition.x * 1.0f * tileSize.x + tileSetPosition.x) / atlasTexture.getWidth(),
                ((tileTexturePosition.x + 1.0f) * tileSize.x + tileSetPosition.x) / atlasTexture.getWidth(),
                (tileTexturePosition.y * 1.0f * tileSize.y + tileSetPosition.y) / atlasTexture.getHeight(),
                ((tileTexturePosition.y + 1.0f) * tileSize.y + tileSetPosition.y) / atlasTexture.getHeight()
        );
    }

    // Texture coordinates of the 4 corners of a tile (8 floats from index). flip: 1 horizontal, 2 vertical, 3 both
    // (originalPosition is flipped in place); rotation: quarter turns, 0 to 3
    static void setTexturePosition(float[] texturePosition, int index, Vector4f originalPosition,
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

        // Corners in drawing order: (x, z), (x, w), (y, w), (y, z); each quarter turn starts one corner later.
        // Any other rotation value is drawn without rotation
        int first = rotation >= 1 && rotation <= 3 ? rotation : 0;
        for (int corner = 0; corner < 4; ++corner) {
            int source = (first + corner) % 4;
            texturePosition[index + 2 * corner] = source < 2 ? originalPosition.x : originalPosition.y;
            texturePosition[index + 2 * corner + 1] = source == 0 || source == 3 ? originalPosition.z : originalPosition.w;
        }
    }

    public void reset() {
        this.tilemap = null;
        this.tileSetAtlas = null;
        mesh.setEbo(new int[0]);
        mesh.updateVbo(new float[0], positionIndex);
        mesh.updateVbo(new float[0], textureIndex);

        animatedTiles.clear();
    }
}