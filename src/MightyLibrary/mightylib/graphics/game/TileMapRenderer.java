package MightyLibrary.mightylib.graphics.game;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.map.TileMap;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;

public class TileMapRenderer extends Renderer {
    private static final int NUMBER_INDICES = 4;
    private static final int SIZE_INDICES = 6;
    private static final int SIZE_COORDINATES = 8;

    private final int positionIndex;
    private final int textureIndex;

    private final Vector2f rightLeftPosition;

    private EDirection reference;

    private TileMap tilemap;
    private final boolean isForLayer;


    public TileMapRenderer(String shaderName, boolean willChange, boolean isForLayer) {
        super(shaderName, true, true);

        // Null let renderer's ancien setting for text
        this.reference = EDirection.RightUp;

        this.rightLeftPosition = new Vector2f(0, 0);

        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(new int[0]);

        positionIndex = shape.addVbo(new float[0], 2, (willChange) ? Shape.DYNAMIC_STORE : Shape.STATIC_STORE);
        textureIndex = shape.addVbo(new float[0], 2,  (willChange) ? Shape.DYNAMIC_STORE : Shape.STATIC_STORE);

        this.isForLayer = isForLayer;
    }

    public TileMapRenderer setReference(EDirection reference){
        if (reference == null)
            return this;

        this.reference = reference;
        this.computeAllMap();

        return this;
    }

    public EDirection reference() {
        return this.reference;
    }


    public void setTilemap(TileMap tilemap){
        this.tilemap = tilemap;

        switchToTextureMode(tilemap.tileset().texture());

        computeAllMap();
    }

    public void computeAllMap(){

        rightLeftPosition.x = position.x;
        rightLeftPosition.y = position.y;

        Vector2i mapSize = tilemap.getMapSize().mul(tilemap.tileset().tileSize());

        switch(this.reference){
            case None:
            case Up:
            case Down:
                rightLeftPosition.x = (int)(-mapSize.x * 0.5f);
                break;
            case RightDown:
            case Right:
            case RightUp:
                rightLeftPosition.x = -mapSize.x;
                break;
        }

        switch(this.reference){
            case None:
            case Left:
            case Right:
                rightLeftPosition.y = (int)(rightLeftPosition.y * 0.5f);
                break;
            case LeftDown:
            case Down:
            case RightDown:
                rightLeftPosition.y = -mapSize.y;
                break;
        }

        int tileNumber = tilemap.numberOfNonEmptyTile();
        int tileCount = 0;

        int[] indices = new int[tileNumber * 6];
        float[] texturePosition = new float[tileNumber * 8];
        float[] position = new float[tileNumber * 8];

        int tileType;

        Texture texture = Resources.getInstance().getResource(Texture.class, tilemap.tileset().texture());
        Vector2i tilePosition = new Vector2i();
        Vector4f temp = new Vector4f();

        for (int layer = 0; layer < ((isForLayer) ? tilemap.forlayerNumber() : tilemap.backlayerNumber()); ++layer) {
            for (int y = 0; y < tilemap.mapHeight(); ++y) {
                for (int x = 0; x < tilemap.mapWidth(); ++x) {
                    tileType = tilemap.getTileType(isForLayer, layer, x, y);
                    if (tileType <= 0)
                        continue;

                    indices[tileCount * SIZE_INDICES] = tileCount * NUMBER_INDICES;
                    indices[tileCount * SIZE_INDICES + 1] = tileCount * NUMBER_INDICES + 1;
                    indices[tileCount * SIZE_INDICES + 2] = tileCount * NUMBER_INDICES + 2;
                    indices[tileCount * SIZE_INDICES + 3] = tileCount * NUMBER_INDICES + 2;
                    indices[tileCount * SIZE_INDICES + 4] = tileCount * NUMBER_INDICES;
                    indices[tileCount * SIZE_INDICES + 5] = tileCount * NUMBER_INDICES + 3;

                    temp.x = (float) (x * tilemap.tileset().tileSize().x) / texture.getWidth();
                    temp.y = (float) (y * tilemap.tileset().tileSize().y) / texture.getHeight();
                    temp.z = (float) ((x + 1) * tilemap.tileset().tileSize().x) / texture.getWidth();
                    temp.w = (float) ((y + 1) * tilemap.tileset().tileSize().y) / texture.getHeight();

                    position[tileCount * SIZE_COORDINATES] = temp.x;
                    position[tileCount * SIZE_COORDINATES + 1] = temp.z;

                    position[tileCount * SIZE_COORDINATES + 2] = temp.x;
                    position[tileCount * SIZE_COORDINATES + 3] = temp.w;

                    position[tileCount * SIZE_COORDINATES + 4] = temp.y;
                    position[tileCount * SIZE_COORDINATES + 5] = temp.w;

                    position[tileCount * SIZE_COORDINATES + 6] = temp.y;
                    position[tileCount * SIZE_COORDINATES + 7] = temp.z;


                    tilePosition.x = (tileType - 1) * tilemap.tileset().tileSize().x % texture.getWidth();
                    tilePosition.y = (int)((tileType - 1.0f) * tilemap.tileset().tileSize().y / texture.getHeight());

                    temp.x = (float) (tilePosition.x * tilemap.tileset().tileSize().x) / texture.getWidth();
                    temp.y = (float) (tilePosition.y * tilemap.tileset().tileSize().y) / texture.getHeight();
                    temp.z = (float) ((tilePosition.x + 1) * tilemap.tileset().tileSize().x) / texture.getWidth();
                    temp.w = (float) ((tilePosition.y + 1) * tilemap.tileset().tileSize().y) / texture.getHeight();
                    texturePosition[tileCount * SIZE_COORDINATES] = temp.x;
                    texturePosition[tileCount * SIZE_COORDINATES + 1] = temp.z;

                    texturePosition[tileCount * SIZE_COORDINATES + 2] = temp.x;
                    texturePosition[tileCount * SIZE_COORDINATES + 3] = temp.w;

                    texturePosition[tileCount * SIZE_COORDINATES + 4] = temp.y;
                    texturePosition[tileCount * SIZE_COORDINATES + 5] = temp.w;

                    texturePosition[tileCount * SIZE_COORDINATES + 6] = temp.y;
                    texturePosition[tileCount * SIZE_COORDINATES + 7] = temp.z;

                    ++tileCount;
                }
            }
        }


        shape.setEbo(indices);
        shape.updateVbo(texturePosition, textureIndex);
        shape.updateVbo(position, positionIndex);
    }
}

