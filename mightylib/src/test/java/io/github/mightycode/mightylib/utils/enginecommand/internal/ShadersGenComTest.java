package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.shader.IShaderService;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.enginecommand.CommandOwner;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class ShadersGenComTest {
    // The defines of a service, without shaders.json
    private static final class FakeShaders implements IShaderService {
        private SortedMap<String, String> defines = new TreeMap<>();
        private final JSONObject declarations = new JSONObject("{"
                + "\"plain\": {\"files\": [\"v.glsl\", \"f.glsl\"], \"tags\": [\"2D\"]},"
                + "\"tinted\": {\"files\": [\"v.glsl\", \"f.glsl\"], \"tags\": [\"2D\"], \"defines\": {\"TINT\": \"1\"}}}");

        @Override
        public JSONObject declarations() {
            return declarations;
        }

        @Override
        public String folder() {
            return "shaders/";
        }

        @Override
        public String prelude(Map<String, String> entryDefines) {
            return "#version 330 core\n";
        }

        @Override
        public void setDefine(String name, String value) {
            checkDefine(name, value);
            TreeMap<String, String> changed = new TreeMap<>(defines);
            changed.put(name, value);
            defines = changed;
        }

        @Override
        public void removeDefine(String name) {
            TreeMap<String, String> changed = new TreeMap<>(defines);
            changed.remove(name);
            defines = changed;
        }

        @Override
        public Map<String, String> defines() {
            return Collections.unmodifiableSortedMap(defines);
        }
    }

    private final FakeShaders shaders = new FakeShaders();
    private final Commands commands = newCommands();

    private Commands newCommands() {
        // Without the general commands: this one reads the service of the test
        Commands created = new Commands(ECommandLevel.DEV, ECommandLevel.DEV);
        created.register(new ShadersGenCom(() -> shaders), CommandOwner.Plugin(Identifier.ENGINE_NAMESPACE));
        return created;
    }

    @Test
    void listGivesTheVersionTheDefinesAndTheShaders() {
        shaders.setDefine("DEMO", "");

        CommandResult result = commands.execute("shaders");

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("#version 330 core, 1 global define(s)\n  #define DEMO\n2 declared shader(s)\n  plain\n"
                + "  tinted (defines: TINT)", result.getMessage());
        JSONObject data = result.getData();
        assertEquals("#version 330 core", data.getString("version"));
        assertEquals("", data.getJSONObject("defines").getString("DEMO"));
        assertEquals("1", data.getJSONArray("shaders").getJSONObject(1).getJSONObject("defines").getString("TINT"));
    }

    @Test
    void defineWithOrWithoutValue() {
        assertTrue(commands.execute("shaders define FLAG").isSuccess());
        CommandResult result = commands.execute("shaders define TINT vec3(0.2, 0.6, 1.0)");

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(Map.of("FLAG", "", "TINT", "vec3(0.2, 0.6, 1.0)"), shaders.defines());
        assertFalse(commands.execute("shaders define TINT vec3(0.2, 0.6, 1.0)").getData().getBoolean("changed"));
    }

    @Test
    void invalidNameIsAnError() {
        CommandResult result = commands.execute("shaders define GL_X 1");

        assertFalse(result.isSuccess());
        assertTrue(shaders.defines().isEmpty());
    }

    @Test
    void undefineOnlyTakesAGlobalDefine() {
        shaders.setDefine("A", "1");

        assertFalse(commands.execute("shaders undefine B").isSuccess());
        assertTrue(commands.execute("shaders undefine A").isSuccess());
        assertTrue(shaders.defines().isEmpty());
    }

    // The names of the defines set are proposed
    @Test
    void completionProposesTheDefinesSet() {
        shaders.setDefine("BETA", "");
        shaders.setDefine("ALPHA", "");

        assertEquals(List.of("define", "undefine"), commands.complete("shaders ").getCandidates());
        assertEquals(List.of("ALPHA", "BETA"), commands.complete("shaders undefine ").getCandidates());
        assertEquals(List.of("ALPHA", "BETA"), commands.complete("shaders define ").getCandidates());
    }
}
