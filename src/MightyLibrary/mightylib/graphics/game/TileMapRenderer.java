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

    private final Vector2f leftUpPosition;

    private EDirection reference;

    private TileMap tilemap;
    private final boolean isForLayer;


    public TileMapRenderer(String shaderName, boolean willChange, boolean isForLayer) {
        super(shaderName, true, true);

        // Null let renderer's old setting for text
        this.reference = EDirection.LeftUp;

        this.leftUpPosition = new Vector2f(0, 0);

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
        this.computeTileInformations();

        return this;
    }

    public EDirection reference() {
        return this.reference;
    }


    public void setTilemap(TileMap tilemap){
        this.tilemap = tilemap;

        switchToTextureMode(tilemap.tileset().texture());

        computeTileInformations();
    }

    public void computeTileInformations(){
        leftUpPosition.x = position.x;
        leftUpPosition.y = position.y;

        Vector2i mapSize = tilemap.getMapSize().mul(tilemap.tileset().tileSize());

        switch(this.reference){
            case None:
            case Up:
            case Down:
                leftUpPosition.x = (int)(-mapSize.x * 0.5f);
                break;
            case RightDown:
            case Right:
            case RightUp:
                leftUpPosition.x = -mapSize.x;
                break;
        }

        switch(this.reference){
            case None:
            case Left:
            case Right:
                leftUpPosition.y = (int)(mapSize.y * 0.5f);
                break;
            case LeftDown:
            case Down:
            case RightDown:
                leftUpPosition.y = -mapSize.y;
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

        Vector2i tileSize = tilemap.tileset().tileSize();

        for (int layer = 0; layer < ((isForLayer) ? tilemap.forlayerNumber() : tilemap.backlayerNumber()); ++layer) {
            for (int y = 0; y < tilemap.mapHeight(); ++y) {
                for (int x = 0; x < tilemap.mapWidth(); ++x) {
                    tileType = tilemap.getTileType(isForLayer, layer, x, y);
                    if (tileType <= 0)
                        continue;

                    tileType -= 1;

                    indices[tileCount * SIZE_INDICES] = tileCount * NUMBER_INDICES;
                    indices[tileCount * SIZE_INDICES + 1] = tileCount * NUMBER_INDICES + 1;
                    indices[tileCount * SIZE_INDICES + 2] = tileCount * NUMBER_INDICES + 2;
                    indices[tileCount * SIZE_INDICES + 3] = tileCount * NUMBER_INDICES + 2;
                    indices[tileCount * SIZE_INDICES + 4] = tileCount * NUMBER_INDICES;
                    indices[tileCount * SIZE_INDICES + 5] = tileCount * NUMBER_INDICES + 3;

                    temp.x = x * tilemap.tileset().tileSize().x;
                    temp.y = (x + 1) * tilemap.tileset().tileSize().x - (leftUpPosition.x - position().x);
                    temp.z = y * tilemap.tileset().tileSize().y;
                    temp.w = (y + 1) * tilemap.tileset().tileSize().y - (leftUpPosition.y - position().y);

                    position[tileCount * SIZE_COORDINATES] = temp.x;
                    position[tileCount * SIZE_COORDINATES + 1] = temp.z;

                    position[tileCount * SIZE_COORDINATES + 2] = temp.x;
                    position[tileCount * SIZE_COORDINATES + 3] = temp.w;

                    position[tileCount * SIZE_COORDINATES + 4] = temp.y;
                    position[tileCount * SIZE_COORDINATES + 5] = temp.w;

                    position[tileCount * SIZE_COORDINATES + 6] = temp.y;
                    position[tileCount * SIZE_COORDINATES + 7] = temp.z;


                    tilePosition.x = (tileType * tileSize.x) % texture.getWidth() / tilemap.tileset().tileSize().x;
                    tilePosition.y = (tileType * tileSize.y) / texture.getWidth();

                    temp.x = (tilePosition.x * 1.0f * tileSize.x) / texture.getWidth();
                    temp.y = ((tilePosition.x + 1.0f) * tileSize.x) / texture.getWidth();
                    temp.z = (tilePosition.y * 1.0f * tileSize.y) / texture.getHeight();
                    temp.w = ((tilePosition.y + 1.0f) * tileSize.y) / texture.getHeight();

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

