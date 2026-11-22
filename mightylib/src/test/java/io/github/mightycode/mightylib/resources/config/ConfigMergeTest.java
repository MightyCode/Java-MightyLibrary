package io.github.mightycode.mightylib.resources.config;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// The deep merge of the layers of a configuration and the dotted keys
class ConfigMergeTest {
    @Test
    void objectsMergeOtherValuesReplace() {
        JSONObject low = new JSONObject("{\"a\": 1, \"o\": {\"x\": 1, \"y\": 2}, \"r\": {\"k\": 1}, \"l\": [1, 2]}");
        JSONObject high = new JSONObject("{\"o\": {\"y\": 3, \"z\": 4}, \"r\": 5, \"l\": [3]}");

        JSONObject merged = ConfigData.Merge(low, high);
        assertEquals(1, merged.getInt("a"));
        assertEquals(1, merged.getJSONObject("o").getInt("x"));
        assertEquals(3, merged.getJSONObject("o").getInt("y"));
        assertEquals(4, merged.getJSONObject("o").getInt("z"));
        assertEquals(5, merged.getInt("r"));
        assertEquals(1, merged.getJSONArray("l").length());

        // The layers are not changed, the result shares nothing with them
        assertEquals(2, low.getJSONObject("o").getInt("y"));
        merged.getJSONObject("o").put("x", 9);
        assertEquals(1, low.getJSONObject("o").getInt("x"));
    }

    @Test
    void dottedKeys() {
        JSONObject object = new JSONObject("{\"a\": {\"b\": {\"c\": true}}, \"n\": null}");
        assertEquals(true, ConfigData.Find(object, "a.b.c"));
        assertNull(ConfigData.Find(object, "a.x.c"));
        assertNull(ConfigData.Find(object, "n"));
        assertThrows(IllegalArgumentException.class, () -> ConfigData.Find(object, ""));
        assertThrows(IllegalArgumentException.class, () -> ConfigData.Find(object, "a."));
    }

    @Test
    void pathOfAConfiguration() {
        assertEquals("configs/mightylib/display.json", ConfigData.PathOf("mightylib/display"));
    }

    @Test
    void aRootIsNotAConfigurationFile() {
        Path root = Path.of("").toAbsolutePath().getRoot();
        assertThrows(IOException.class, () -> ConfigData.WriteAtomically(root, "{}"));
    }
}
