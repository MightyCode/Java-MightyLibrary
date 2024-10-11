package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.algorithms.wavefunctioncollapse.WaveFunctionCollapseGrid;
import MightyLibrary.mightylib.graphics.game.LayersTileMapRenderer;
import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.map.TileLayer;
import MightyLibrary.mightylib.resources.map.TileMap;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.resources.map.TileSet;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.scenes.camera.cameraComponents.DebugInfoCamera2D;
import MightyLibrary.mightylib.scenes.camera.cameraComponents.DraggingCameraComponent;
import MightyLibrary.mightylib.scenes.camera.cameraComponents.MovingCameraComponent;
import MightyLibrary.mightylib.scenes.camera.cameraComponents.ZoomingCameraComponent;
import MightyLibrary.mightylib.algorithms.wavefunctioncollapse.WaveCollapseRules;
import MightyLibrary.mightylib.utils.Timer;
import MightyLibrary.project.main.ActionId;
import MightyLibrary.project.scenes.loadingScenes.LoadingSceneImplementation;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class Test2DWaveFunctionCollapseScene extends Scene {
    private TileMap map;
    private LayersTileMapRenderer mapRenderer;
    private Vector2i mapSize;
    private WaveFunctionCollapseGrid waveFunctionCollapseGrid;
    private boolean immediateConstruction;
    private Timer timer;

    private Camera2D hudCamera;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{
                "map-assets", "wavecollapse"
        };
    }

    public void init(String[] args) {
        super.init(args, new LoadingSceneImplementation());
    }

    public void launch(String[] args) {
        super.launch(args);
        /// SCENE INFORMATION ///

        hudCamera = main2DCamera.copy();

        setClearColor(52, 189, 235, 1f);
        //setClearColor(0, 0, 0, 1f);

        /// RENDERERS ///

        mapSize = new Vector2i(100, 100);

        mapRenderer = new LayersTileMapRenderer("texture2D", true);

        //rule.printRules();

        WaveCollapseRules rule = resources.getResource(WaveCollapseRules.class, "waveCollapseTest");

        map = new TileMap(DataType.TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT, "waveCollapseTest", "none");
        map.addTileset(resources.getResource(TileSet.class, rule.getRecommendedTileset()), 0);
        map.addDependency(rule);
        map.setPreloaded();

        waveFunctionCollapseGrid = new WaveFunctionCollapseGrid(rule, map, mapSize);
        resources.addPreLoadedResource(map);

        waveFunctionCollapseGrid.waveFunctionAlgorithmStep(1);
        System.out.println(map.tileSetAtlas().getTextureAtlas().isLoaded());
        mapRenderer.setTileMap(map);

        DraggingCameraComponent draggingSceneComponent = new DraggingCameraComponent();
        draggingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(), main2DCamera);
        draggingSceneComponent.initActionId(ActionId.RIGHT_CLICK);

        addUpdatable(draggingSceneComponent);

        MovingCameraComponent movingSceneComponent = new MovingCameraComponent();
        movingSceneComponent.init(mainContext.getInputManager(), main2DCamera);
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
        zoomingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(),
                main2DCamera, mainContext.getWindow().getInfo().getSizeRef());
        zoomingSceneComponent.initActionId(ActionId.SHIFT);

        addUpdatable(zoomingSceneComponent);

        addUpdatableAndDisplayable(
                new DebugInfoCamera2D(hudCamera).init(main2DCamera, new Vector2f(5, 5))
                        .addInfo("position").addInfo("rotation").addInfo("zoom")
        );

        timer = new Timer();
        timer.start(0.33f);
        immediateConstruction = false;
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();

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

        map.dispose();
    }


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
        resources.deleteResource(map);
    }
}
