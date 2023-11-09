package MightyLibrary.mightylib.utils.math;

import MightyLibrary.mightylib.resources.data.JSONFile;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class WaveCollapseRule {
    public static class Rule {
        public static Vector2i INITIAl_POS = new Vector2i(0, 0);

        private final int id;

        private final List<Vector2i> directions;
        private final List<Set<Integer>> availableTiles;

        public Rule(int id){
            this.id = id;
            this.availableTiles = new ArrayList<>();
            this.directions = new ArrayList<>();
        }

        public void addDirection(Vector2i direction){
            if (getDirectionIndex(direction) != -1)
                return;

            directions.add(direction);
            availableTiles.add(new HashSet<>());
        }

        private int getDirectionIndex(Vector2i pos){
            for (int i = 0; i < directions.size(); ++i){
                Vector2i direction = directions.get(i);

                if (direction.x == pos.x && direction.y == pos.y)
                    return i;
            }

            return -1;
        }

        public void addAvailableTile(Vector2i pos, int tileId) {
            availableTiles.get(getDirectionIndex(pos)).add(tileId);
        }


        public void addAvailableTiles(Vector2i pos, Set<Integer> tilesId){
            availableTiles.get(getDirectionIndex(pos)).addAll(tilesId);
        }

        public Set<Integer> getAvailableTiles(Vector2i pos){
            return availableTiles.get(getDirectionIndex(pos));
        }

        public int getId() {
            return id;
        }

        public List<Vector2i> getDirections() {
            return directions;
        }
    }

    public WaveCollapseRule(JSONFile jsonFile) {
        init(jsonFile);
    }

    private String recommendedTileset;
    private int numberRuledTiles;

    private Map<Integer, Rule> rules;

    private Rule initialRule;

    public Set<Integer> makeAvailableList(JSONObject list) {
        Set<Integer> available = new HashSet<>();

        if (list.keySet().contains("all")) {
            available.addAll(rules.keySet());
        }

        if (list.keySet().contains("list")){
            JSONArray array = list.getJSONArray("list");
            for (int i = 0; i < array.length(); ++i){
                available.add(array.getInt(i));
            }
        }

        if (list.keySet().contains("range")){
            throw new RuntimeException ("Range not implemented yet");
        }

        return available;
    }

    public void init(JSONFile jsonFile){
        JSONObject root = jsonFile.getObject();
        JSONObject info = root.getJSONObject("info");

        recommendedTileset = info.getString("tileset");

        rules = new TreeMap<>(Comparator.comparingInt(o -> o));
        initialRule = new Rule(-1);

        for (String key : root.keySet()){
            try {
                int id = Integer.parseInt(key);
                Rule rule = new Rule(id);
                ++numberRuledTiles;

                rules.put(id, rule);
            } catch (NumberFormatException ignored) {}
        }

        initialRule.addDirection(Rule.INITIAl_POS);
        initialRule.addAvailableTiles(Rule.INITIAl_POS, makeAvailableList(root.getJSONObject("initial")));

        // Add all rules
        for (String key : root.keySet()){
            try {
                int id = Integer.parseInt(key);

                // Add all directions
                JSONArray dependencyList = root.getJSONObject(key).getJSONArray("list");

                for (int i = 0; i < dependencyList.length(); ++i){
                    JSONObject dependency = dependencyList.getJSONObject(i);
                    List<Vector2i> directions = new ArrayList<>();

                    if (dependency.keySet().contains("directions")){
                        JSONArray directionsArray = dependency.getJSONArray("directions");

                        for (int j = 0; j < directionsArray.length(); ++j){
                            JSONArray directionObject = directionsArray.getJSONArray(j);

                            directions.add(new Vector2i(directionObject.getInt(0), directionObject.getInt(1)));
                        }
                    } else if (dependency.keySet().contains("direction")){
                        JSONArray directionObject = dependency.getJSONArray("direction");

                        directions.add(new Vector2i(directionObject.getInt(0), directionObject.getInt(1)));
                    }

                    Set<Integer> available = makeAvailableList(dependency.getJSONObject("available"));

                    for (Vector2i directionsPos : directions){
                        Rule rule = rules.get(id);
                        /*System.out.print("Chosen tile ("+ id + ") (" + directionsPos.x + "," + directionsPos.y + ") :");
                        for (Integer tile : available){
                            System.out.print(tile + ", ");
                        }
                        System.out.println();*/


                        rule.addDirection(directionsPos);
                        rule.addAvailableTiles(directionsPos, available);
                    }
                }

            } catch (NumberFormatException ignored) {}
        }
    }


    public String getRecommendedTileset () { return recommendedTileset; }
    public int getNumberRuledTiles () { return numberRuledTiles; }

    public Rule getInitialRule () { return initialRule; }

    public Rule getRule(int chosenTile) {
        return rules.get(chosenTile);
    }
}
