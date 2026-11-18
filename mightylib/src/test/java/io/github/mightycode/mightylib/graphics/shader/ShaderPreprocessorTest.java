package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShaderPreprocessorTest {
    private static final String FOLDER = "shaders/";
    private static final String PRELUDE = "#version 330 core\n#define A 1\n";
    // Start of every source: the prelude, then the main file from its line 1
    private static final String HEAD = PRELUDE + "#line 1 0\n";

    private final Map<String, String> files = new HashMap<>();

    private ShaderPreprocessor preprocessor() {
        return new ShaderPreprocessor(files::get, FOLDER, PRELUDE);
    }

    private String process(String path) {
        return preprocessor().process(path);
    }

    @Test
    void includeIsReplacedWithLineDirectives() {
        files.put("shaders/2D/a.glsl", "uniform int x;\n#include \"lib.glsl\"\nvoid main() {}\n");
        files.put("shaders/2D/lib.glsl", "int f() { return 1; }\n");

        ShaderPreprocessor preprocessor = preprocessor();
        String source = preprocessor.process("shaders/2D/a.glsl");

        // Numbers of the log: lines of the file, after the prelude
        assertEquals(HEAD + "uniform int x;\n#line 1 1\nint f() { return 1; }\n#line 3 0\nvoid main() {}\n",
                source);
        assertEquals(List.of("shaders/2D/a.glsl", "shaders/2D/lib.glsl"), preprocessor.files());
        assertEquals(List.of("shaders/2D/a.glsl", "shaders/2D/lib.glsl"), preprocessor.watchedFiles());
    }

    @Test
    void includeIsSearchedNextToTheFileThenInTheShadersFolder() {
        files.put("shaders/2D/a.glsl", "#include \"utils/colors.glsl\"\n#include \"near.glsl\"");
        files.put("shaders/2D/near.glsl", "near");
        files.put("shaders/near.glsl", "far");
        files.put("shaders/utils/colors.glsl", "colors");

        String source = process("shaders/2D/a.glsl");

        assertTrue(source.contains("colors\n"));
        assertTrue(source.contains("near\n"));
        assertFalse(source.contains("far"));
    }

    // An included file includes next to itself
    @Test
    void nestedIncludeIsRelativeToTheIncludingFile() {
        files.put("shaders/a.glsl", "#include \"utils/b.glsl\"");
        files.put("shaders/utils/b.glsl", "#include \"c.glsl\"\nb");
        files.put("shaders/utils/c.glsl", "c");

        ShaderPreprocessor preprocessor = preprocessor();
        String source = preprocessor.process("shaders/a.glsl");

        assertEquals(HEAD + "#line 1 1\n#line 1 2\nc\n#line 2 1\nb\n#line 2 0\n", source);
        assertEquals(List.of("shaders/a.glsl", "shaders/utils/b.glsl", "shaders/utils/c.glsl"),
                preprocessor.files());
    }

    @Test
    void fileIsIncludedOncePerSource() {
        files.put("shaders/a.glsl", "#include \"lib.glsl\"\n#include \"b.glsl\"\n#include \"lib.glsl\"\nend");
        files.put("shaders/b.glsl", "#include \"lib.glsl\"\nb");
        files.put("shaders/lib.glsl", "lib");

        String source = process("shaders/a.glsl");

        assertEquals(1, source.split("\nlib\n", -1).length - 1);
        assertTrue(source.contains("// #include \"lib.glsl\" (already included)\nb"));
        assertTrue(source.endsWith("// #include \"lib.glsl\" (already included)\nend"));

        // Each source starts again: the same file is included in the next one
        assertTrue(process("shaders/b.glsl").contains("lib\n"));
    }

    @Test
    void cycleIsRefused() {
        files.put("shaders/a.glsl", "#include \"b.glsl\"");
        files.put("shaders/b.glsl", "#include \"c.glsl\"");
        files.put("shaders/c.glsl", "#include \"b.glsl\"");

        ResourceException error = assertThrows(ResourceException.class, () -> process("shaders/a.glsl"));
        assertTrue(error.getMessage().contains("shaders/b.glsl -> shaders/c.glsl -> shaders/b.glsl"),
                error.getMessage());
    }

    // The source fails (fallback) instead of being compiled without its include; both paths are watched
    @Test
    void missingIncludeFailsAndIsWatched() {
        files.put("shaders/2D/a.glsl", "#include \"missing.glsl\"");

        ShaderPreprocessor preprocessor = preprocessor();
        ResourceException error = assertThrows(ResourceException.class,
                () -> preprocessor.process("shaders/2D/a.glsl"));
        assertTrue(error.getMessage().contains("missing.glsl"));
        assertTrue(error.getMessage().contains("shaders/2D/a.glsl:1"));
        assertEquals(List.of("shaders/2D/a.glsl", "shaders/2D/missing.glsl", "shaders/missing.glsl"),
                preprocessor.watchedFiles());
    }

    @Test
    void missingMainFileFailsAndIsWatched() {
        ShaderPreprocessor preprocessor = preprocessor();
        assertThrows(ResourceException.class, () -> preprocessor.process("shaders/none.glsl"));
        assertEquals(List.of("shaders/none.glsl"), preprocessor.watchedFiles());
    }

    // The engine writes the #version (prelude): a file that gives one is refused, main file or include
    @Test
    void versionInAFileIsRefused() {
        files.put("shaders/a.glsl", "#version 330 core\nvoid main() {}");
        ResourceException error = assertThrows(ResourceException.class, () -> process("shaders/a.glsl"));
        assertTrue(error.getMessage().contains("shaders/a.glsl:1"), error.getMessage());

        files.put("shaders/b.glsl", "#include \"lib.glsl\"");
        files.put("shaders/lib.glsl", "int f();\n  #  version 330 core");
        error = assertThrows(ResourceException.class, () -> process("shaders/b.glsl"));
        assertTrue(error.getMessage().contains("shaders/lib.glsl:2"), error.getMessage());
    }

    // The include of the first line comes after the prelude, the next line keeps its number
    @Test
    void includeOnTheFirstLineComesAfterThePrelude() {
        files.put("shaders/a.glsl", "#include \"lib.glsl\"\nvoid main() {}");
        files.put("shaders/lib.glsl", "int f();");

        assertEquals(HEAD + "#line 1 1\nint f();\n#line 2 0\nvoid main() {}", process("shaders/a.glsl"));
    }

    // A prelude without a final line break gets one: #line stays on its own line
    @Test
    void preludeEndsWithALineBreak() {
        files.put("shaders/a.glsl", "x");

        assertEquals("#version 330 core\n#line 1 0\nx",
                new ShaderPreprocessor(files::get, FOLDER, "#version 330 core").process("shaders/a.glsl"));
    }

    @Test
    void indentationAndCarriageReturnsAreHandled() {
        files.put("shaders/a.glsl", "void main() {\r\n    #include \"body.glsl\"\r\n}\r\n");
        files.put("shaders/body.glsl", "\tint x = 1;\r\n\tint y = 2;\r\n");

        assertEquals(HEAD + "void main() {\n#line 1 1\n\tint x = 1;\n\tint y = 2;\n#line 3 0\n}\n",
                process("shaders/a.glsl"));
    }

    @Test
    void invalidIncludeSyntaxIsRefused() {
        files.put("shaders/a.glsl", "#include lib.glsl");

        assertThrows(ResourceException.class, () -> process("shaders/a.glsl"));
    }

    // NVIDIA "0(12)", Mesa/AMD/Intel "0:12(5)" and "ERROR: 0:12:": the first location of each line
    @Test
    void compilerLogIsTranslatedToFileAndLine() {
        List<String> ids = List.of("shaders/a.glsl", "shaders/utils/lib.glsl");

        assertEquals("shaders/utils/lib.glsl:8 : error C0000: syntax error",
                ShaderPreprocessor.translateLog("1(8) : error C0000: syntax error", ids));
        assertEquals("shaders/a.glsl:12(5): error: x undeclared\nERROR: shaders/utils/lib.glsl:3: 'y' : undeclared",
                ShaderPreprocessor.translateLog("0:12(5): error: x undeclared\nERROR: 1:3: 'y' : undeclared", ids));
        // Unknown id or format: kept
        assertEquals("7(2) : error", ShaderPreprocessor.translateLog("7(2) : error", ids));
        assertEquals("syntax error", ShaderPreprocessor.translateLog("syntax error", ids));
        assertEquals("0(1) : error", ShaderPreprocessor.translateLog("0(1) : error", List.of()));
    }

    // The reader of the shader loader: an include of a higher source replaces the lower one, the others stay
    @Test
    void anIncludeOfAHigherSourceReplacesTheLowerOne(@TempDir Path low, @TempDir Path high) throws IOException {
        Files.createDirectories(low.resolve("shaders/2D"));
        Files.createDirectories(high.resolve("shaders/2D"));
        Files.writeString(low.resolve("shaders/2D/a.glsl"), "#include \"lib.glsl\"\n#include \"other.glsl\"\nmain");
        Files.writeString(low.resolve("shaders/2D/lib.glsl"), "low lib");
        Files.writeString(low.resolve("shaders/other.glsl"), "low other");
        Files.writeString(high.resolve("shaders/2D/lib.glsl"), "high lib");
        ResourceSources sources = ResourceSources.of(new FolderSource(low), new FolderSource(high));

        ShaderPreprocessor preprocessor = new ShaderPreprocessor(path -> ShaderLoader.ReadIfFile(sources, path),
                FOLDER, PRELUDE);
        String source = preprocessor.process("shaders/2D/a.glsl");

        assertTrue(source.contains("high lib"), source);
        assertFalse(source.contains("low lib"), source);
        assertTrue(source.contains("low other"), source);
        // Out of the sources: not found, never read
        assertNull(ShaderLoader.ReadIfFile(sources, "../a.glsl"));
        assertNull(ShaderLoader.ReadIfFile(sources, "shaders"));
    }

    @Test
    void pathsAreNormalized() {
        files.put("shaders/2D/a.glsl", "#include \"../utils/lib.glsl\"");
        files.put("shaders/utils/lib.glsl", "lib");

        ShaderPreprocessor preprocessor = preprocessor();
        preprocessor.process("shaders/./2D/a.glsl");
        assertEquals(List.of("shaders/2D/a.glsl", "shaders/utils/lib.glsl"), preprocessor.files());
    }
}
