package MightyLibrary.mightylib.algorithms.wavefunctioncollapse;

import MightyLibrary.mightylib.resources.map.TileLayer;
import MightyLibrary.mightylib.resources.map.TileMap;
import MightyLibrary.mightylib.resources.map.TileSet;
import org.joml.Vector2i;

import java.util.*;

public class WaveFunctionCollapseGrid {
    private final TileMap map;
    private final Vector2i mapSize;

    private final WaveCollapseRule rule;

    private WaveCollapseRule.Rule initialRule;

    private List<Integer>[][] availableTiles;

    private Random random;

    private boolean finished = false;
    private Map<Integer, List<Vector2i>> minimumPossibleTilePositions;

    public WaveFunctionCollapseGrid(WaveCollapseRule rule, TileMap map, TileSet set, Vector2i mapSize) {
        this.rule = rule;
        this.map = map;
        this.mapSize = mapSize;

        TileLayer layer = new TileLayer(mapSize);

        TileLayer[] layers = new TileLayer[1];
        layers[0] = layer;

        map.addTileset(set, 0);
        map.init(mapSize, layers, 1);

        availableTiles = new ArrayList[mapSize.y][mapSize.x];
        for (int y = 0; y < mapSize.y; ++y) {
            for (int x = 0; x < mapSize.x; ++x) {
                availableTiles[y][x] = new ArrayList<>();
            }
        }

        random = new Random();

        minimumPossibleTilePositions = new TreeMap<>();

        init();
    }

    private void init(){
        initialRule = rule.getInitialRule();

        Set<Integer> listOfTile = initialRule.getAvailableIds(WaveCollapseRule.Rule.INITIAl_POS);
        minimumPossibleTilePositions.clear();
        List<Vector2i> positions = new ArrayList<>();

        // Add all possible tiles to each cell
        for (int y = 0; y < mapSize.y; ++y) {
            for (int x = 0; x < mapSize.x; ++x) {
                // Copy the list of tile
                availableTiles[y][x].clear();
                availableTiles[y][x].addAll(listOfTile);
                positions.add(new Vector2i(x, y));
            }
        }

        minimumPossibleTilePositions.put(listOfTile.size(), positions);
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

    private void printMinimumPossibleTilePositions(){
        System.out.println("Minimum possible tile positions : ");
        for (Map.Entry<Integer, List<Vector2i>> entry : minimumPossibleTilePositions.entrySet()){
            System.out.print("Key : " + entry.getKey());
            for (Vector2i pos : entry.getValue()){
                System.out.print("(" + pos.x + ", " + pos.y + ")");
            }
            System.out.println();
        }
    }

    private void printMatricesOfAvailableSize(){
        System.out.println("Matrices of available size : ");
        for (int y = 0; y < mapSize.y; ++y) {
            for (int x = 0; x < mapSize.x; ++x) {
                System.out.print(availableTiles[y][x].size() + " ");
            }
            System.out.println();
        }
    }

    private void setTileAt(int x, int y, int tileId){
        map.setTileType(0, x, y, tileId);

        if (tileId != -1) {
            availableTiles[y][x].clear();

            WaveCollapseRule.Rule ruleToApply = rule.getRule(tileId);

            for (Vector2i direction : ruleToApply.getDirections()) {
                // Only keep for the direction tile the corresponding list
                Set<Integer> availableTile = ruleToApply.getAvailableIds(direction);

                Vector2i newPos = new Vector2i(x, y).add(direction);

                if (newPos.x < 0 || newPos.x >= mapSize.x || newPos.y < 0 || newPos.y >= mapSize.y)
                    continue;

                if (availableTiles[newPos.y][newPos.x].size() > 0) {
                    int oldSize = availableTiles[newPos.y][newPos.x].size();

                    availableTiles[newPos.y][newPos.x].retainAll(availableTile);

                    int newSize = availableTiles[newPos.y][newPos.x].size();

                    if (oldSize != newSize) {
                        // For all element in map with old size, remove the position if vector x and y match
                        List<Vector2i> positions = minimumPossibleTilePositions.get(oldSize);
                        /*System.out.println("New Position " + newPos.x + ", "
                                + newPos.y + " : " + oldSize + " -> " + newSize);*/

                        for (int i = 0; i < positions.size(); ++i) {
                            Vector2i pos = positions.get(i);

                            if (pos.x == newPos.x && pos.y == newPos.y) {
                                positions.remove(i);
                                break;
                            }
                        }

                        if (positions.size() == 0)
                            minimumPossibleTilePositions.remove(oldSize);

                        // Add the new position to the new size list
                        if (!minimumPossibleTilePositions.containsKey(newSize))
                            minimumPossibleTilePositions.put(newSize, new ArrayList<>());

                        minimumPossibleTilePositions.get(newSize).add(newPos);

                        //System.out.println("New size list " + minimumPossibleTilePositions.get(newSize).size());
                    }
                }
            }
        }
    }

    private Vector2i getNextTileToCompute(){
        // Return a tile from the minimum key of the map

        if (minimumPossibleTilePositions.size() == 0)
            return null;

        int minKey = minimumPossibleTilePositions.keySet().iterator().next();
        List<Vector2i> positions = minimumPossibleTilePositions.get(minKey);
        if (positions.size() == 0)
            return null;

        // Take and remove
        int index = random.nextInt(positions.size());
        Vector2i pos = positions.get(index);
        positions.remove(index);

        if (positions.size() == 0)
            minimumPossibleTilePositions.remove(minKey);

        return pos;
    }

    public void waveFunctionAlgorithm() {
        Vector2i currentPosition = getNextTileToCompute();

        while (currentPosition != null){
            int chosenTile = -1;

            if (availableTiles[currentPosition.y][currentPosition.x].size() > 0)
                chosenTile = availableTiles[currentPosition.y][currentPosition.x].get(
                        random.nextInt(availableTiles[currentPosition.y][currentPosition.x].size()));

            setTileAt(currentPosition.x, currentPosition.y, chosenTile);

            // Select the minimum available point by its list size, if there is no available point, return null
            currentPosition = getNextTileToCompute();
        }

        finished = true;
    }

    // Do n step of wave function algorithm
    public void waveFunctionAlgorithmStep(int n) {
        //printMatricesOfAvailableSize();
        //printMinimumPossibleTilePositions();
        Vector2i currentPosition = getNextTileToCompute();
        if (currentPosition == null) {
            finished = true;
            return;
        }
        //printMinimumPossibleTilePositions();

        System.out.println("Chosen position : (" + currentPosition.x + ", " + currentPosition.y + ")");

        for (int i = 0; i < n && currentPosition != null; ++i){
            int chosenTile = -1;

            if (availableTiles[currentPosition.y][currentPosition.x].size() > 0)
                chosenTile = availableTiles[currentPosition.y][currentPosition.x].get(
                        random.nextInt(availableTiles[currentPosition.y][currentPosition.x].size()));

            setTileAt(currentPosition.x, currentPosition.y, chosenTile);
        }
    }

    public boolean isFinished(){
        return finished;
    }
}
