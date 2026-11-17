package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.graphics.shader.ShaderPreprocessor;
import io.github.mightycode.mightylib.graphics.text.FontFile;
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

    // The names listed by the batch of the engine for a type of data
    private static List<String> batchFiles(String type) {
        JSONObject batch = FileMethods.readJSONObject(engine, "batchs/" + EngineResources.ENGINE_BATCH + ".batch");
        List<String> names = new ArrayList<>();
        JSONArray entries = batch.getJSONArray("batchs");
        for (int i = 0; i < entries.length(); ++i) {
            JSONObject entry = entries.getJSONObject(i);
            if (!entry.getString("type").equals(type))
                continue;

            assertTrue(entry.getJSONArray("regex").isEmpty(), "the names are listed: the game keeps its own regex");
            for (Object file : entry.getJSONArray("files"))
                names.add((String) file);
        }

        return names;
    }

    @Test
    void theBatchListsExactlyTheShaders() {
        assertEquals(EngineResources.shaders(), batchFiles("ShaderData"));
    }

    @Test
    void theBatchLoadsTheTwoRegularFontsOnly() {
        assertEquals(List.of(EngineResources.DEFAULT_FONT, EngineResources.CONSOLE_FONT),
                EngineResources.loadedFonts());
        assertEquals(EngineResources.loadedFonts(), batchFiles("TextureData"));
        assertEquals(EngineResources.loadedFonts(), batchFiles("FontFace"));
    }

    @Test
    void everyFontIsDeclaredWithItsAtlas() {
        JSONObject fonts = FileMethods.readJSONObject(engine, "textures/fonts/fonts.json").getJSONObject("fonts");
        JSONObject atlases = FileMethods.readJSONObject(engine, "textures/textures.json")
                .getJSONObject("textures").getJSONObject("fonts");
        assertEquals(new HashSet<>(EngineResources.fonts()), fonts.keySet());
        assertEquals(new HashSet<>(EngineResources.fonts()), atlases.keySet());
        assertTrue(EngineResources.fonts().containsAll(EngineResources.loadedFonts()));

        for (String name : EngineResources.fonts()) {
            JSONObject font = fonts.getJSONObject(name);
            assertEquals(name, font.getString("texture"), name + ": the atlas has the name of the font");

            String info = "textures/fonts/" + font.getString("info");
            String atlas = "textures/fonts/" + atlases.getJSONObject(name).getString("file");
            assertTrue(engine.size(atlas) > 0, name + ": missing atlas " + atlas);

            // One page, the file of the atlas; a character of the interface and the arrows of the console
            FontFile file = new FontFile(info);
            file.load(engine);
            String page = FileMethods.readString(engine, info).lines()
                    .filter(line -> line.startsWith("page ")).findFirst().orElse("");
            assertTrue(page.contains("file=\"" + atlas.substring("textures/fonts/".length()) + "\""), name + ": " + page);
            assertEquals(1, FileMethods.readString(engine, info).lines().filter(l -> l.startsWith("page ")).count(),
                    name + ": FontFile reads one page");
            assertTrue(file.getCharacter('A').getxAdvance() > 0, name);
            for (int arrow = 0x2190; arrow <= 0x2195; ++arrow)
                assertTrue(file.getCharacter(arrow).getWidth() > 0, name + ": missing arrow " + arrow);
        }
    }

    // The cursor and the selection of the console count on one advance for every character of the monospace font
    @Test
    void theConsoleFontIsMonospace() {
        for (String name : List.of(EngineResources.CONSOLE_FONT, EngineResources.CONSOLE_FONT_BOLD,
                EngineResources.CONSOLE_FONT_ITALIC, EngineResources.CONSOLE_FONT_BOLD_ITALIC)) {
            FontFile file = new FontFile("textures/fonts/" + name + ".fnt");
            file.load(engine);
            double advance = file.getCharacter('a').getxAdvance();
            for (int c = 33; c < 127; ++c)
                assertEquals(advance, file.getCharacter(c).getxAdvance(), name + ": " + (char) c);
        }
    }

    @Test
    void theLicenseOfTheFontsIsNextToTheAtlas() {
        assertTrue(readIfFile("textures/fonts/OFL.txt").contains("SIL OPEN FONT LICENSE Version 1.1"));
    }
}
