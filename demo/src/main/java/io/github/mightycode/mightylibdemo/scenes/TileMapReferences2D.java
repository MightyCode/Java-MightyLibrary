package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.game.LayersTileMapRenderer;
import io.github.mightycode.mightylib.graphics.game.TileMapRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

/**
 * The references of a tile map (TileMapRenderer.setReference): the same map drawn nine times, its renderer placed at
 * a point marked by a cross, one reference each, named above. The cross falls on the corner, the middle of the side or
 * the center the reference names, and every map keeps its size. A reference scene of the image comparisons.
 */
public class TileMapReferences2D extends Scene {
    // In the order of the grid: rows from the top, columns from the left
    private static final EDirection[] REFERENCES = {
            EDirection.LeftUp, EDirection.Up, EDirection.RightUp,
            EDirection.Left, EDirection.None, EDirection.Right,
            EDirection.LeftDown, EDirection.Down, EDirection.RightDown};
    // Size of a cell of the grid, in sizes of the map: the map reaches one size from its point at most
    private static final float CELL = 2.5f;
    private static final Color4f CROSS_COLOR = new Color4f(1f, 0.25f, 0.2f, 1f);

    private LayersTileMapRenderer[] maps;
    private Text[] names;
    private final Vector2f[] points = new Vector2f[REFERENCES.length];
    private float crossSize;
    private Text help;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{"map-assets"};
    }

    @Override
    public void launch(String[] args) {
        Camera2D hudCamera = camera2D().copy();
        DemoCamera.free(this);
        setClearColor(30, 34, 44, 1f);

        TileMap tileMap = resources().getResource(TileMap.class, "map");
        Vector2i tile = tileMap.tileSetAtlas().getTileSize();
        Vector2f mapSize = new Vector2f(tileMap.mapWidth() * tile.x, tileMap.mapHeight() * tile.y);
        crossSize = Math.min(mapSize.x, mapSize.y) * 0.25f;

        maps = new LayersTileMapRenderer[REFERENCES.length];
        names = new Text[REFERENCES.length];
        for (int i = 0; i < REFERENCES.length; ++i) {
            int column = i % 3, row = i / 3;
            points[i] = new Vector2f((column + 0.5f) * CELL * mapSize.x, (row + 0.5f) * CELL * mapSize.y);

            maps[i] = new LayersTileMapRenderer("texture2D", false);
            maps[i].setTileMap(tileMap);
            for (String category : maps[i].getCategories()) {
                TileMapRenderer renderer = maps[i].getTileMapRenderer(category);
                renderer.setReference(REFERENCES[i]);
                renderer.setPosition(new Vector3f(points[i], 0));
            }

            names[i] = new Text();
            names[i].setFontSize(Math.max(8, mapSize.y * 0.2f))
                    .setReference(EDirection.Up)
                    .setColor(new Color4f(1, 1, 1, 1))
                    .setPosition(new Vector2f(points[i].x, row * CELL * mapSize.y + mapSize.y * 0.05f))
                    .setText(REFERENCES[i].name());
        }

        // The whole grid on the screen
        Vector2i screen = context().getWindow().getInfo().getVirtualSizeCopy();
        float gridWidth = 3 * CELL * mapSize.x, gridHeight = 3 * CELL * mapSize.y;
        float zoom = Math.min(screen.x / gridWidth, screen.y / gridHeight) * 0.9f;
        camera2D().setZoomLevel(zoom);
        camera2D().setPos(new Vector2f(gridWidth * 0.5f - screen.x * 0.5f / zoom,
                gridHeight * 0.5f - screen.y * 0.5f / zoom));

        help = new Text();
        help.setFontSize(16).setReference(EDirection.LeftDown).setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(10, screen.y - 10))
                .setText("ZQSD/arrows, right drag : move   wheel : zoom   the cross is the position of each map");
        help.setReferenceCamera(hudCamera);
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        // One frame each: asked again at each frame
        for (Vector2f point : points)
            debugDraw().cross(point.x, point.y, crossSize, CROSS_COLOR);
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        for (LayersTileMapRenderer map : maps)
            map.display();
        for (Text name : names)
            name.display();
        help.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (maps == null)
            return;

        for (LayersTileMapRenderer map : maps)
            map.unload();
        for (Text name : names)
            name.unload();
        help.unload();
    }
}
