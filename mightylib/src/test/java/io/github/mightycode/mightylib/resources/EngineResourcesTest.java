package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.graphics.shader.ShaderPreprocessor;
import io.github.mightycode.mightylib.resources.source.ClasspathSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The content of the source of the engine (classpath assets/mightylib/): every name of EngineResources is there
class EngineResourcesTest {
    private static final String SHADERS_FOLDER = "shaders/3_30/";

    private static ResourceSources engine;

    @BeforeAll
    static void openEngineSource() {
        engine = ResourceSources.of(new ClasspathSource(EngineResourcesTest.class.getClassLoader(), "assets/mightylib/"));
    }

    @AfterAll
    static void closeEngineSource() {
        engine.close();
    }

    private static JSONObject declaredShaders() {
        return FileMethods.readJSONObject(engine, SHADERS_FOLDER + ShaderManager.LIST_SHADER_FILE_NAME)
                .getJSONObject("shaders");
    }

    private static String readIfFile(String path) {
        try {
            return engine.size(path) < 0 ? null : FileMethods.readString(engine, path);
        } catch (IllegalArgumentException e) {
            // Out of the source by its ".."
            return null;
        }
    }

    @Test
    void everyShaderIsDeclaredWithItsFiles() {
        JSONObject shaders = declaredShaders();
        assertEquals(new HashSet<>(EngineResources.shaders()), shaders.keySet(),
                "shaders.json of the engine declares exactly the shaders of EngineResources");

        for (String name : EngineResources.shaders()) {
            JSONArray files = shaders.getJSONObject(name).getJSONArray("files");
            assertFalse(files.isEmpty(), name);
            for (int i = 0; i < files.length(); ++i) {
                String path = SHADERS_FOLDER + files.getString(i);
                assertTrue(engine.size(path) >= 0, name + ": missing file " + path);
            }
        }
    }

    // A game without shaders of its own still compiles those of the engine: their includes are in the engine
    @Test
    void everyIncludeResolvesInTheEngineAlone() {
        JSONObject shaders = declaredShaders();
        for (String name : EngineResources.shaders()) {
            JSONArray files = shaders.getJSONObject(name).getJSONArray("files");
            for (int i = 0; i < files.length(); ++i) {
                String path = SHADERS_FOLDER + files.getString(i);
                ShaderPreprocessor preprocessor = new ShaderPreprocessor(EngineResourcesTest::readIfFile, SHADERS_FOLDER, 0);
                assertDoesNotThrow(() -> preprocessor.process(path), name + ": " + path);
            }
        }
    }

    @Test
    void theBatchListsExactlyTheShaders() {
        JSONObject batch = FileMethods.readJSONObject(engine, "batchs/" + EngineResources.ENGINE_BATCH + ".batch");

        List<String> shaders = new ArrayList<>();
        JSONArray entries = batch.getJSONArray("batchs");
        for (int i = 0; i < entries.length(); ++i) {
            JSONObject entry = entries.getJSONObject(i);
            if (!entry.getString("type").equals("ShaderData"))
                continue;

            assertTrue(entry.getJSONArray("regex").isEmpty(), "the names are listed: the game keeps its own regex");
            for (Object file : entry.getJSONArray("files"))
                shaders.add((String) file);
        }

        assertEquals(EngineResources.shaders(), shaders);
    }
}
