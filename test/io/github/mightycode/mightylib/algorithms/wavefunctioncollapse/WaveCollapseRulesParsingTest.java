package io.github.mightycode.mightylib.algorithms.wavefunctioncollapse;

import io.github.mightycode.mightylib.resources.data.JSONFile;
import org.joml.Vector2i;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// The two rule formats ("list" of neighbours and "connections" by side types) and the lists of available tiles
class WaveCollapseRulesParsingTest {
    private static final Vector2i RIGHT = new Vector2i(1, 0), LEFT = new Vector2i(-1, 0), UP = new Vector2i(0, 1);

    private static WaveCollapseRules rules(String json) {
        JSONFile file = new JSONFile(JSONFile.TYPE_SET_UP.THREAD_CONTEXT, "rules.wavecollapse", "rules.wavecollapse");
        file.setJsonObject(new JSONObject(json));

        WaveCollapseRules rules = new WaveCollapseRules("rules");
        rules.setData(file);
        return rules;
    }

    private static Set<Integer> set(Integer... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    @Test
    void listFormatGivesEachTileItsAvailableNeighboursPerDirection() {
        WaveCollapseRules rules = rules("{\"info\":{\"tileset\":\"ground\",\"type\":\"list\"},"
                + "\"initial\":{\"list\":[0,1]},"
                + "\"0\":{\"list\":[{\"direction\":[1,0],\"available\":{\"list\":[1]}},"
                + "{\"directions\":[[0,1],[0,-1]],\"available\":{\"all\":true}}]},"
                + "\"1\":{\"list\":[]},"
                + "\"2\":{\"list\":[{\"direction\":[-1,0],\"available\":{\"list\":[0,2]}}]}}");
        assertTrue(rules.internLoad());

        assertEquals("ground", rules.getRecommendedTileset());
        assertEquals(3, rules.getNumberRuledTiles());

        WaveCollapseRules.Rule zero = rules.getRule(0);
        assertEquals(0, zero.getElementId());
        assertEquals(3, zero.getDirections().size());
        assertEquals(set(1), zero.getAvailableIds(RIGHT));
        assertEquals(set(0, 1, 2), zero.getAvailableIds(UP));
        assertEquals(set(0, 1, 2), zero.getAvailableIds(new Vector2i(0, -1)));

        assertTrue(rules.getRule(1).getDirections().isEmpty());
        assertEquals(set(0, 2), rules.getRule(2).getAvailableIds(LEFT));
        assertNull(rules.getRule(7), "a tile without rule");
    }

    @Test
    void theInitialRuleListsTheTilesAllowedEverywhereAtTheStart() {
        WaveCollapseRules all = rules("{\"info\":{\"tileset\":\"t\",\"type\":\"list\"},\"initial\":{\"all\":true},"
                + "\"0\":{\"list\":[]},\"4\":{\"list\":[]}}");
        assertTrue(all.internLoad());
        assertEquals(-1, all.getInitialRule().getElementId());
        assertEquals(set(0, 4), all.getInitialRule().getAvailableIds(WaveCollapseRules.Rule.INITIAL_POS));

        WaveCollapseRules some = rules("{\"info\":{\"tileset\":\"t\",\"type\":\"list\"},\"initial\":{\"list\":[4]},"
                + "\"0\":{\"list\":[]},\"4\":{\"list\":[]}}");
        assertTrue(some.internLoad());
        assertEquals(set(4), some.getInitialRule().getAvailableIds(WaveCollapseRules.Rule.INITIAL_POS));
    }

    @Test
    void connectionsFormatLinksTheSidesThatHaveTheSameType() {
        // 0 shows type 1 on its right side, 1 shows type 1 on its left side, 2 connects to itself on both sides
        WaveCollapseRules rules = rules("{\"info\":{\"tileset\":\"t\",\"type\":\"connections\"},\"initial\":{\"all\":true},"
                + "\"0\":{\"connections\":[{\"directions\":[[1,0]],\"types\":[1]}]},"
                + "\"1\":{\"connections\":[{\"directions\":[[-1,0]],\"types\":[1]}]},"
                + "\"2\":{\"connections\":[{\"directions\":[[1,0],[-1,0]],\"types\":[5]}]}}");
        assertTrue(rules.internLoad());

        assertEquals(set(1), rules.getRule(0).getAvailableIds(RIGHT), "added by 1, which is seen after 0");
        assertEquals(set(0), rules.getRule(1).getAvailableIds(LEFT));
        assertEquals(set(2), rules.getRule(2).getAvailableIds(RIGHT));
        assertEquals(set(2), rules.getRule(2).getAvailableIds(LEFT));
    }

    @Test
    void aTileWithSeveralTypesOnASideAcceptsEveryMatchingNeighbour() {
        WaveCollapseRules rules = rules("{\"info\":{\"tileset\":\"t\",\"type\":\"connections\"},\"initial\":{\"all\":true},"
                + "\"0\":{\"connections\":[{\"directions\":[[1,0]],\"types\":[1,2]}]},"
                + "\"1\":{\"connections\":[{\"directions\":[[-1,0]],\"types\":[1]}]},"
                + "\"2\":{\"connections\":[{\"directions\":[[-1,0]],\"types\":[2]}]},"
                + "\"3\":{\"connections\":[{\"directions\":[[-1,0]],\"types\":[3]}]}}");
        assertTrue(rules.internLoad());

        assertEquals(set(1, 2), rules.getRule(0).getAvailableIds(RIGHT));
        assertEquals(set(0), rules.getRule(1).getAvailableIds(LEFT));
        assertTrue(rules.getRule(3).getAvailableIds(LEFT).isEmpty(), "nobody shows type 3 on its right");
    }

    @Test
    void anUnknownFormatIsRefused() {
        WaveCollapseRules rules = rules("{\"info\":{\"tileset\":\"t\",\"type\":\"magic\"},\"initial\":{\"all\":true}}");
        RuntimeException error = assertThrows(RuntimeException.class, rules::internLoad);
        assertTrue(error.getMessage().contains("magic"));
    }

    @Test
    void rangesAreNotSupportedYet() {
        WaveCollapseRules rules = rules("{\"info\":{\"tileset\":\"t\",\"type\":\"list\"},\"initial\":{\"range\":[0,3]}}");
        assertThrows(RuntimeException.class, rules::internLoad);
    }

    @Test
    void addingTwiceTheSameDirectionKeepsOneList() {
        WaveCollapseRules.Rule rule = new WaveCollapseRules.Rule(3);
        rule.addDirection(new Vector2i(1, 0));
        rule.addAvailableId(RIGHT, 5);
        rule.addDirection(new Vector2i(1, 0));
        rule.addAvailableId(RIGHT, 6);

        assertEquals(1, rule.getDirections().size());
        assertEquals(set(5, 6), rule.getAvailableIds(RIGHT));
    }

    @Test
    void unloadingEmptiesTheRules() {
        WaveCollapseRules rules = rules("{\"info\":{\"tileset\":\"t\",\"type\":\"list\"},\"initial\":{\"all\":true},"
                + "\"0\":{\"list\":[]}}");
        assertTrue(rules.internLoad());
        rules.internUnload();
        assertNull(rules.getInitialRule());
        assertNull(rules.getRule(0));
    }
}
