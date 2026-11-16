package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.algorithms.wavefunctioncollapse.WaveFunctionCollapseGrid;
import io.github.mightycode.mightylib.graphics.game.LayersTileMapRenderer;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.map.TileLayer;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.resources.map.TileSet;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.DebugInfoCamera2D;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.DraggingCameraComponent;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.MovingCameraComponent;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.ZoomingCameraComponent;
import io.github.mightycode.mightylib.algorithms.wavefunctioncollapse.WaveCollapseRules;
import io.github.mightycode.mightylib.utils.Timer;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class WaveFunctionCollapse2D extends Scene {
    private TileMap map;
    private LayersTileMapRenderer mapRenderer;
    private WaveFunctionCollapseGrid waveFunctionCollapseGrid;
    private boolean immediateConstruction;
    private Timer timer;


    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{
                "map-assets", "wavecollapse"
        };
    }

    @Override
    public void launch(String[] args) {
        /// SCENE INFORMATION ///

        Camera2D hudCamera = camera2D().copy();

        setClearColor(52, 189, 235, 1f);
        //setClearColor(0, 0, 0, 1f);

        /// RENDERERS ///

        Vector2i mapSize = new Vector2i(100, 100);

        mapRenderer = new LayersTileMapRenderer("texture2D", true);

        //rule.printRules();

        WaveCollapseRules rule = resources().getResource(WaveCollapseRules.class, "waveCollapseTest");

        map = new TileMap(DataType.TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT, "waveCollapseTest", "none");
        map.addTileset(resources().getResource(TileSet.class, rule.getRecommendedTileset()), 0);
        map.addDependency(rule);
        map.setPreloaded();

        waveFunctionCollapseGrid = new WaveFunctionCollapseGrid(rule, map, mapSize);
        resources().addPreLoadedResource(map);

        waveFunctionCollapseGrid.waveFunctionAlgorithmStep(1);
        System.out.println(map.tileSetAtlas().getTextureAtlas().isLoaded());
        mapRenderer.setTileMap(map);

        DraggingCameraComponent draggingSceneComponent = new DraggingCameraComponent();
        draggingSceneComponent.init(context().getInputManager(), context().getMouseManager(), camera2D());
        draggingSceneComponent.initActionId(ActionId.RIGHT_CLICK);

        addUpdatable(draggingSceneComponent);

        MovingCameraComponent movingSceneComponent = new MovingCameraComponent();
        movingSceneComponent.init(context().getInputManager(), camera2D());
        movingSceneComponent.initActionIds(
                new MovingCameraComponent.Inputs()
                        .setMoveLeft(ActionId.MOVE_LEFT_2D)
                        .setMoveRight(ActionId.MOVE_RIGHT_2D)
                        .setMoveDown(ActionId.MOVE_DOWN_2D)
                        .setMoveUp(ActionId.MOVE_UP_2D)
                        .setQuickSpeed(ActionId.SHIFT)
        );

        addUpdatable(movingSceneComponent);

        ZoomingCameraComponent zoomingSceneComponent = new ZoomingCameraComponent();
        zoomingSceneComponent.init(context().getInputManager(), context().getMouseManager(),
                camera2D(), context().getWindow().getInfo().getSizeRef());
        zoomingSceneComponent.initActionId(ActionId.SHIFT);

        addUpdatable(zoomingSceneComponent);

        addUpdatableAndDisplayable(
                new DebugInfoCamera2D(hudCamera).init(camera2D(), new Vector2f(5, 5))
                        .addInfo("position").addInfo("rotation").addInfo("zoom")
        );

        timer = new Timer();
        timer.start(0.33f);
        immediateConstruction = false;
    }


    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});

        IInputManager inputManager = context().getInputManager();

        if (inputManager.inputPressed(ActionId.TAB)){
            waveFunctionCollapseGrid.reset();
        }

        if (inputManager.inputPressed(ActionId.ENTER))
            immediateConstruction = !immediateConstruction;

        if (!waveFunctionCollapseGrid.isFinished()) {
            if (immediateConstruction) {
                waveFunctionCollapseGrid.waveFunctionAlgorithm();
            } else {
                timer.update();
                if (timer.isFinished()) {
                    waveFunctionCollapseGrid.waveFunctionAlgorithmStep(1);
                    timer.resetStart();
                }
            }
        }

        mapRenderer.update();

        //mapRenderer.getBackTileMapRenderer().setScale(new Vector3f(4f, 4f, 1f));

        map.clearTileChanges();
    }


    @Override
    public void display() {
        super.setVirtualScene();
        clear();

        mapRenderer.display();

        if (mapRenderer.containsCategory("Foreground"))
            mapRenderer.getTileMapRenderer("Foreground").setPosition(new Vector3f(0, 0, 0));

        mapRenderer.getTileMapRenderer(TileLayer.DEFAULT_CATEGORY_NAME).setPosition(new Vector3f(0, 0, 0));

        super.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        mapRenderer.unload();
        resources().deleteResource(map);
    }
}
