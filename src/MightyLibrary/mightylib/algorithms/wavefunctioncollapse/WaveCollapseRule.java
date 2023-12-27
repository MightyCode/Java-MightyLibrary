package MightyLibrary.mightylib.algorithms.wavefunctioncollapse;

import MightyLibrary.mightylib.resources.data.JSONFile;
import MightyLibrary.mightylib.utils.math.Direction;
import org.joml.Vector2i;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class WaveCollapseRule {
    public static class Rule {
        public static Vector2i INITIAl_POS = new Vector2i(0, 0);
        private final int id;
        private final List<Vector2i> directions;
        
        // available tile as bits
        private final List<Set<Integer>> availableTiles;

        public Rule(int elementId){
            this.id = elementId;
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

        public void addAvailableId(Vector2i pos, int tileId) {
            availableTiles.get(getDirectionIndex(pos)).add(tileId);
        }


        public void addAvailableIds(Vector2i pos, Collection<Integer> tilesId){
            availableTiles.get(getDirectionIndex(pos)).addAll(tilesId);
        }

        public Set<Integer> getAvailableIds(Vector2i pos){
            return availableTiles.get(getDirectionIndex(pos));
        }

        public int getElementId() {
            return id;
        }

        public List<Vector2i> getDirections() {
            return directions;
        }

        public void printRule() {
            for (int i = 0; i < directions.size(); ++i){
                Vector2i direction = directions.get(i);
                Set<Integer> available = availableTiles.get(i);

                System.out.print("\tDirection " + direction.x + ", " + direction.y + " : ");
                for (Integer id : available){
                    System.out.print("  " + id);
                }
                System.out.println();
            }
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
        initialRule.addAvailableIds(Rule.INITIAl_POS, makeAvailableList(root.getJSONObject("initial")));

        if (info.getString("type").equals("list"))
            parseListIdTypeFile(root);
        else if (info.getString("type").equals("connections"))
            parseGridTypeFile(root);
        else
            throw new RuntimeException("Unknown type " + info.getString("type"));
    }

    public void parseListIdTypeFile(JSONObject root){

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
                        rule.addDirection(directionsPos);
                        rule.addAvailableIds(directionsPos, available);
                    }
                }

            } catch (NumberFormatException ignored) {}
        }
    }

    private static <T> T getValue(Map<Vector2i, T> map, Vector2i vec){
        for (Vector2i vec2 : map.keySet()){
            if (vec2.x == vec.x && vec.y == vec2.y)
                return map.get(vec2);
        }

        return null;
    }

    public void parseGridTypeFile(JSONObject root){
        Map<Vector2i, Map<Integer, Set<Integer>>> typedRules = new HashMap<>();

        for (Integer id : rules.keySet()) {
            Rule rule = rules.get(id);
            JSONObject jsonRule = root.getJSONObject(id.toString());
            JSONArray connections = jsonRule.getJSONArray("connections");

            for (int i = 0; i < connections.length(); ++i) {
                JSONObject connection = connections.getJSONObject(i);

                JSONArray directions = connection.getJSONArray("directions");
                JSONArray types = connection.getJSONArray("types");

                // Add self to all direction
                for (int j = 0; j < directions.length(); ++j) {
                    Vector2i direction = new Vector2i(directions.getJSONArray(j).getInt(0),
                            directions.getJSONArray(j).getInt(1));
                    rule.addDirection(direction);

                    Map<Integer, Set<Integer>> selfGroup = getValue(typedRules, direction);
                    if (selfGroup == null) {
                        selfGroup = new HashMap<>();
                    }

                    typedRules.put(direction, selfGroup);

                    for (int k = 0; k < types.length(); ++k) {
                        int groupType = types.getInt(k);

                        if (!selfGroup.containsKey(groupType))
                            selfGroup.put(groupType, new HashSet<>());

                        selfGroup.get(groupType).add(id);
                    }
                }

                // Check for all group, for all direction and add ids to each other rules.
                for (int j = 0; j < directions.length(); ++j) {
                    Vector2i direction = new Vector2i(directions.getJSONArray(j).getInt(0),
                            directions.getJSONArray(j).getInt(1));

                    Vector2i oppositeDirection = direction.mul(-1, new Vector2i());
                    Map<Integer, Set<Integer>> connectionGroupMap = getValue(typedRules, oppositeDirection);

                    for (int k = 0; k < types.length(); ++k) {
                        int groupType = types.getInt(k);

                        if (connectionGroupMap != null && connectionGroupMap.containsKey(groupType)) {
                            Set<Integer> oppositeListId = connectionGroupMap.get(groupType);

                            // Add other to self
                            rule.addAvailableIds(direction, oppositeListId);

                            // Add self to other
                            for (Integer oppositeId : oppositeListId) {
                                Rule oppositeRule = rules.get(oppositeId);
                                oppositeRule.addAvailableId(oppositeDirection, id);
                            }
                        }
                    }
                }
            }
        }
    }


    public String getRecommendedTileset () { return recommendedTileset; }
    public int getNumberRuledTiles () { return numberRuledTiles; }

    public Rule getInitialRule () { return initialRule; }

    public Rule getRule(int chosenTile) {
        return rules.get(chosenTile);
    }

    public void printRules(){
        System.out.println("Initial rule :");
        initialRule.printRule();

        for (Integer id : rules.keySet()){
            Rule rule = rules.get(id);
            System.out.println("Rule " + id + " :");
            rule.printRule();
        }
    }
}
