package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.game.FullTileMapRenderer;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.physics.tweenings.type.FloatTweening;
import MightyLibrary.mightylib.resources.data.JSONFile;
import MightyLibrary.mightylib.resources.map.TileLayer;
import MightyLibrary.mightylib.resources.map.TileMap;
import MightyLibrary.mightylib.graphics.renderer._2D.Animation2DRenderer;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import MightyLibrary.mightylib.resources.animation.Animator;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.resources.map.TileSet;
import MightyLibrary.mightylib.scenes.Camera2D;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.utils.math.Color4f;
import MightyLibrary.mightylib.utils.math.EDirection;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.Vector2fTweening;
import MightyLibrary.mightylib.utils.math.MightyMath;
import MightyLibrary.mightylib.utils.math.WaveCollapseRule;
import MightyLibrary.project.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.*;

public class Test2dWaveFunctionCollapseScene extends Scene {
    private TileMap map;
    private FullTileMapRenderer mapRenderer;

    private Camera2D testCamera;

    private Vector2i mapSize;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        main3DCamera.setPos(new Vector3f(0, 0, 0));

        setClearColor(52, 189, 235, 1f);
        //setClearColor(0, 0, 0, 1f);

        /// RENDERERS ///

        WaveCollapseRule rule = new WaveCollapseRule(resources.getResource(JSONFile.class, "waveCollapseTest"));
        TileSet set = resources.getResource(TileSet.class, rule.getRecommendedTileset());

        map = new TileMap("waveCollapseTest", "none");
        mapSize = new Vector2i(30, 30);
        waveFunctionAlgorithm(map, set, rule, mapSize);

        mapRenderer = new FullTileMapRenderer("texture2D", false);
        mapRenderer.setTileMap(map);

        testCamera = new Camera2D(mainContext.getWindow().getInfo(), new Vector2f(0, 0));
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();
        if (inputManager.inputPressed(ActionId.TAB)){
            WaveCollapseRule rule = new WaveCollapseRule(resources.getResource(JSONFile.class, "waveCollapseTest"));
            TileSet set = resources.getResource(TileSet.class, rule.getRecommendedTileset());
            waveFunctionAlgorithm(map, set, rule, mapSize);
        }

        map.setTileType(0, 0, 0, -1);

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

        Set<Integer> listOfTile = initialRule.getAvailableTiles(WaveCollapseRule.Rule.INITIAl_POS);

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

                    Set<Integer> availableTile = ruleToApply.getAvailableTiles(direction);
                    /*System.out.print("Chosen tile (" + ruleToApply.getId() + ") (" + direction.x + "," + direction.y + ") :");
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
                        /*System.out.println("new size "
                                + availableTiles[newPos.y][newPos.x].size());*/

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
