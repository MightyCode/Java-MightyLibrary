package io.github.mightycode.mightylib.algorithms.wavefunctioncollapse;

import io.github.mightycode.mightylib.resources.data.JSONFile;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Hot reload: the rules stay usable while their new version is computed apart
class WaveCollapseRulesTest {
    private static WaveCollapseRules load(String rulesJson) {
        JSONFile json = new JSONFile(JSONFile.TYPE_SET_UP.THREAD_CONTEXT, "rules.wavecollapse", "rules.wavecollapse");
        json.setJsonObject(new JSONObject(rulesJson));

        WaveCollapseRules rules = new WaveCollapseRules("rules");
        rules.setData(json);
        assertTrue(rules.internLoad());
        return rules;
    }

    private static String rules(String tileset, int tiles) {
        StringBuilder json = new StringBuilder("{\"info\":{\"tileset\":\"" + tileset + "\",\"type\":\"list\"},"
                + "\"initial\":{\"all\":true}");
        for (int i = 0; i < tiles; ++i)
            json.append(",\"").append(i).append("\":{\"list\":[]}");
        return json.append('}').toString();
    }

    @Test
    void reloadCopyExchangesTheContent() {
        WaveCollapseRules current = load(rules("old", 2));
        WaveCollapseRules copy = (WaveCollapseRules) current.createReloadCopy();
        assertNotNull(copy);

        WaveCollapseRules newVersion = load(rules("new", 3));
        current.swapContent(newVersion);

        assertEquals("new", current.getRecommendedTileset());
        assertEquals(3, current.getNumberRuledTiles());
        assertNotNull(current.getRule(2));
        assertEquals(3, current.getInitialRule().getAvailableIds(WaveCollapseRules.Rule.INITIAL_POS).size());
        assertEquals("old", newVersion.getRecommendedTileset(), "the copy gets the previous version");
    }

    // Computed twice (reload in place): the number of tiles is not added up
    @Test
    void loadedTwiceCountsTheTilesOnce() {
        WaveCollapseRules rules = load(rules("tiles", 4));
        assertTrue(rules.internLoad());
        assertEquals(4, rules.getNumberRuledTiles());
    }
}
