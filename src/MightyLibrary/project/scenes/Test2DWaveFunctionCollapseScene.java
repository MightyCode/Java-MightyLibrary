package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.algorithms.wavefunctioncollapse.WaveFunctionCollapseGrid;
import MightyLibrary.mightylib.graphics.game.FullTileMapRenderer;
import MightyLibrary.mightylib.resources.data.JSONFile;
import MightyLibrary.mightylib.resources.map.TileLayer;
import MightyLibrary.mightylib.resources.map.TileMap;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.resources.map.TileSet;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.scenes.cameracomponents.DraggingCameraComponent;
import MightyLibrary.mightylib.scenes.cameracomponents.MovingCameraComponent;
import MightyLibrary.mightylib.scenes.cameracomponents.ZoomingCameraComponent;
import MightyLibrary.mightylib.algorithms.wavefunctioncollapse.WaveCollapseRule;
import MightyLibrary.mightylib.utils.Timer;
import MightyLibrary.project.main.ActionId;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Test2DWaveFunctionCollapseScene extends Scene {
    private TileMap map;
    private FullTileMapRenderer mapRenderer;

    private Vector2i mapSize;

    private DraggingCameraComponent draggingSceneComponent;
    private MovingCameraComponent movingSceneComponent;
    private ZoomingCameraComponent zoomingSceneComponent;

    private WaveFunctionCollapseGrid waveFunctionCollapseGrid;
    private boolean immediateConstruction;
    private Timer timer;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        main3DCamera.setPos(new Vector3f(0, 0, 0));

        setClearColor(52, 189, 235, 1f);
        //setClearColor(0, 0, 0, 1f);

        /// RENDERERS ///
        map = new TileMap("waveCollapseTest", "none");
        mapSize = new Vector2i(100, 100);

        mapRenderer = new FullTileMapRenderer("texture2D", true);

        WaveCollapseRule rule = new WaveCollapseRule(resources.getResource(JSONFile.class, "waveCollapseTest"));
        rule.printRules();

        TileSet set = resources.getResource(TileSet.class, rule.getRecommendedTileset());
        waveFunctionCollapseGrid = new WaveFunctionCollapseGrid(rule, map, set, mapSize);
        waveFunctionCollapseGrid.waveFunctionAlgorithmStep(1);

        mapRenderer.setTileMap(map);

        draggingSceneComponent = new DraggingCameraComponent();
        draggingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(), main2DCamera);
        draggingSceneComponent.initActionId(ActionId.RIGHT_CLICK);

        movingSceneComponent = new MovingCameraComponent();
        movingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(), main2DCamera);
        movingSceneComponent.initActionIds(
                new MovingCameraComponent.Inputs()
                        .setMoveLeft(ActionId.MOVE_LEFT_2D)
                        .setMoveRight(ActionId.MOVE_RIGHT_2D)
                        .setMoveDown(ActionId.MOVE_DOWN_2D)
                        .setMoveUp(ActionId.MOVE_UP_2D)
                        .setQuickSpeed(ActionId.SHIFT)
        );

        zoomingSceneComponent = new ZoomingCameraComponent();
        zoomingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(),
                main2DCamera, mainContext.getWindow().getInfo().getSizeRef());
        zoomingSceneComponent.initActionId(ActionId.SHIFT);

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

        movingSceneComponent.update();
        draggingSceneComponent.update();
        zoomingSceneComponent.update();

        mapRenderer.update();

        mapRenderer.getBackTileMapRenderer().setScale(new Vector3f(4f, 4f, 1f));

        map.dispose();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        mapRenderer.drawBackLayers();
        mapRenderer.drawForLayers();

        mapRenderer.getForTileMapRenderer().setPosition(new Vector3f(0, 0, 0));
        mapRenderer.getBackTileMapRenderer().setPosition(new Vector3f(0, 0, 0));

        super.setAndDisplayRealScene();
    }


    public void unload() {
        super.unload();

        mapRenderer.unload();
    }

    public Vector2i selectMinimumIn(List<Integer>[][] availableTiles, Random random){
        List<Vector2i> positions = new ArrayList<>();
        int minSize = Integer.MAX_VALUE;

        for (int y = 0; y < availableTiles.length; ++y) {
            for (int x = 0; x < availableTiles[y].length; ++x) {
                if (availableTiles[y][x] != null){
                    if (availableTiles[y][x].size() < minSize) {
                        positions.clear();
                        minSize = availableTiles[y][x].size();
                        positions.add(new Vector2i(x, y));
                    } else if (availableTiles[y][x].size() == minSize){
                        positions.add(new Vector2i(x, y));
                    }
                }
            }
        }

        if (positions.size() == 0)
            return null;

        return positions.get(random.nextInt(positions.size()));
    }

    public void waveFunctionAlgorithm(TileMap map, TileSet set, WaveCollapseRule rule, Vector2i mapSize) {
        TileLayer layer = new TileLayer(mapSize);

        WaveCollapseRule.Rule initialRule = rule.getInitialRule();

        Set<Integer> listOfTile = initialRule.getAvailableIds(WaveCollapseRule.Rule.INITIAl_POS);

        List<Integer>[][] availableTiles = new ArrayList[mapSize.y][mapSize.x];
        // Add all possible tiles to each cell
        for (int y = 0; y < mapSize.y; ++y) {
            for (int x = 0; x < mapSize.x; ++x) {
                // Copy the list of tile
                availableTiles[y][x] = new ArrayList<>(listOfTile);
            }
        }

        Random random = new Random();

        Vector2i currentPosition = selectMinimumIn(availableTiles, random);

        while (currentPosition != null){
            int chosenTile = -1;

            if (availableTiles[currentPosition.y][currentPosition.x].size() > 0)
                chosenTile = availableTiles[currentPosition.y][currentPosition.x].get(
                        random.nextInt(availableTiles[currentPosition.y][currentPosition.x].size()));

            layer.setTileType(currentPosition.x, currentPosition.y, chosenTile);

            // Destroy the list
            availableTiles[currentPosition.y][currentPosition.x] = null;

            if (chosenTile != -1) {
                WaveCollapseRule.Rule ruleToApply = rule.getRule(chosenTile);

                for (Vector2i direction : ruleToApply.getDirections()) {
                    // Only keep for the direction tile, the given list

                    Set<Integer> availableTile = ruleToApply.getAvailableIds(direction);
                    /*System.out.print("Chosen tile (" + ruleToApply.getId() + ")
                        (" + direction.x + "," + direction.y + ") :");
                    for (Integer tile : availableTile) {
                        System.out.print(tile + ", ");
                    }
                    System.out.println();*/

                    // Check if direction is in the map

                    Vector2i newPos = currentPosition.add(direction, new Vector2i());

                    if (newPos.x < 0 || newPos.x >= mapSize.x || newPos.y < 0 || newPos.y >= mapSize.y)
                        continue;

                    if (availableTiles[newPos.y][newPos.x] != null) {
                        availableTiles[newPos.y][newPos.x].retainAll(availableTile);
                    }
                }
            }

            // Select the minimum available point by its list size, if there is no available point, return null
            currentPosition = selectMinimumIn(availableTiles, random);
        }

        TileLayer[] layers = new TileLayer[1];
        layers[0] = layer;

        map.init(set, mapSize, layers, 1);
    }
}
