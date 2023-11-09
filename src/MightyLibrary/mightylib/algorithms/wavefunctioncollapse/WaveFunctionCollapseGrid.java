package MightyLibrary.mightylib.algorithms.wavefunctioncollapse;

import MightyLibrary.mightylib.resources.map.TileLayer;
import MightyLibrary.mightylib.resources.map.TileMap;
import MightyLibrary.mightylib.resources.map.TileSet;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WaveFunctionCollapseGrid {
    private final TileMap map;
    private final Vector2i mapSize;

    private final WaveCollapseRule rule;

    private WaveCollapseRule.Rule initialRule;

    private List<Integer>[][] availableTiles;

    private Random random;

    private boolean finished = false;

    public WaveFunctionCollapseGrid(WaveCollapseRule rule, TileMap map, TileSet set, Vector2i mapSize) {
        this.rule = rule;
        this.map = map;
        this.mapSize = mapSize;

        TileLayer layer = new TileLayer(mapSize);

        TileLayer[] layers = new TileLayer[1];
        layers[0] = layer;

        map.init(set, mapSize, layers, 1);

        availableTiles = new ArrayList[mapSize.y][mapSize.x];
        for (int y = 0; y < mapSize.y; ++y) {
            for (int x = 0; x < mapSize.x; ++x) {
                availableTiles[y][x] = new ArrayList<>();
            }
        }

        random = new Random();

        init();
    }

    private void init(){
        initialRule = rule.getInitialRule();

        Set<Integer> listOfTile = initialRule.getAvailableTiles(WaveCollapseRule.Rule.INITIAl_POS);
        // Add all possible tiles to each cell
        for (int y = 0; y < mapSize.y; ++y) {
            for (int x = 0; x < mapSize.x; ++x) {
                // Copy the list of tile
                availableTiles[y][x].clear();
                availableTiles[y][x].addAll(listOfTile);
            }
        }
    }

    public void reset(){
        for (int x = 0; x < mapSize.x; ++x) {
            for (int y = 0; y < mapSize.y; ++y) {
                map.setTileType(0, x, y, -1);
            }
        }

        init();

        finished = false;
    }

    public void forceTileType(int x, int y, int tileId){
        setTileAt(x, y, tileId);
    }

    private void setTileAt(int x, int y, int tileId){
        map.setTileType(0, x, y, tileId);

        if (tileId != -1) {
            availableTiles[y][x].clear();

            WaveCollapseRule.Rule ruleToApply = rule.getRule(tileId);

            for (Vector2i direction : ruleToApply.getDirections()) {
                // Only keep for the direction tile, the given list

                Set<Integer> availableTile = ruleToApply.getAvailableTiles(direction);
                /*System.out.print("Chosen tile (" + ruleToApply.getId() + ")
                    (" + direction.x + "," + direction.y + ") :");
                for (Integer tile : availableTile) {
                    System.out.print(tile + ", ");
                }
                System.out.println();*/

                // Check if direction is in the map

                Vector2i newPos = new Vector2i(x, y).add(direction, new Vector2i());

                if (newPos.x < 0 || newPos.x >= mapSize.x || newPos.y < 0 || newPos.y >= mapSize.y)
                    continue;

                if (availableTiles[newPos.y][newPos.x].size() > 0) {
                    availableTiles[newPos.y][newPos.x].retainAll(availableTile);
                    /*System.out.println("new size "
                            + availableTiles[newPos.y][newPos.x].size());*/

                }
            }

        }
    }

    public Vector2i selectMinimumIn(List<Integer>[][] availableTiles, Random random){
        List<Vector2i> positions = new ArrayList<>();
        int minSize = Integer.MAX_VALUE;

        for (int y = 0; y < availableTiles.length; ++y) {
            for (int x = 0; x < availableTiles[y].length; ++x) {
                if (availableTiles[y][x].size() > 0){
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

    public void waveFunctionAlgorithm() {
        Vector2i currentPosition = selectMinimumIn(availableTiles, random);

        while (currentPosition != null){
            int chosenTile = -1;

            if (availableTiles[currentPosition.y][currentPosition.x].size() > 0)
                chosenTile = availableTiles[currentPosition.y][currentPosition.x].get(
                        random.nextInt(availableTiles[currentPosition.y][currentPosition.x].size()));

            setTileAt(currentPosition.x, currentPosition.y, chosenTile);

            // Select the minimum available point by its list size, if there is no available point, return null
            currentPosition = selectMinimumIn(availableTiles, random);
        }

        finished = true;
    }

    // Do n step of wave function algorithm
    public void waveFunctionAlgorithmStep(int n) {
        Vector2i currentPosition = selectMinimumIn(availableTiles, random);

        for (int i = 0; i < n && currentPosition != null; ++i){
            int chosenTile = -1;

            if (availableTiles[currentPosition.y][currentPosition.x].size() > 0)
                chosenTile = availableTiles[currentPosition.y][currentPosition.x].get(
                        random.nextInt(availableTiles[currentPosition.y][currentPosition.x].size()));

            setTileAt(currentPosition.x, currentPosition.y, chosenTile);

            // Select the minimum available point by its list size, if there is no available point, return null
            currentPosition = selectMinimumIn(availableTiles, random);
        }

        if (currentPosition == null)
            finished = true;
    }

    public boolean isFinished(){
        return finished;
    }
}
