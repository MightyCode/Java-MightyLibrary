package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShaderPreprocessorTest {
    private static final String VERSION_FOLDER = "shaders/3_30/";

    private final Map<String, String> files = new HashMap<>();

    private ShaderPreprocessor preprocessor(int lineOffset) {
        return new ShaderPreprocessor(files::get, VERSION_FOLDER, lineOffset);
    }

    private String process(String path) {
        return preprocessor(0).process(path);
    }

    @Test
    void includeIsReplacedWithLineDirectives() {
        files.put("shaders/3_30/2D/a.glsl", "#version 330 core\nuniform int x;\n#include \"lib.glsl\"\nvoid main() {}\n");
        files.put("shaders/3_30/2D/lib.glsl", "int f() { return 1; }\n");

        ShaderPreprocessor preprocessor = preprocessor(0);
        String source = preprocessor.process("shaders/3_30/2D/a.glsl");

        assertEquals("#version 330 core\nuniform int x;\n#line 1 1\nint f() { return 1; }\n#line 4 0\nvoid main() {}\n",
                source);
        assertEquals(List.of("shaders/3_30/2D/a.glsl", "shaders/3_30/2D/lib.glsl"), preprocessor.files());
        assertEquals(List.of("shaders/3_30/2D/a.glsl", "shaders/3_30/2D/lib.glsl"), preprocessor.watchedFiles());
    }

    // GLSL 140: the line after "#line n" is n + 1
    @Test
    void lineOffsetForOlderGLSL() {
        files.put("shaders/3_30/a.glsl", "#version 140\n#include \"lib.glsl\"\nvoid main() {}");
        files.put("shaders/3_30/lib.glsl", "int f();");

        assertEquals("#version 140\n#line 0 1\nint f();\n#line 2 0\nvoid main() {}",
                preprocessor(-1).process("shaders/3_30/a.glsl"));
    }

    @Test
    void includeIsSearchedNextToTheFileThenInTheVersionFolder() {
        files.put("shaders/3_30/2D/a.glsl", "#include \"utils/colors.glsl\"\n#include \"near.glsl\"");
        files.put("shaders/3_30/2D/near.glsl", "near");
        files.put("shaders/3_30/near.glsl", "far");
        files.put("shaders/3_30/utils/colors.glsl", "colors");

        String source = process("shaders/3_30/2D/a.glsl");

        assertTrue(source.contains("colors\n"));
        assertTrue(source.contains("near\n"));
        assertFalse(source.contains("far"));
    }

    // An included file includes next to itself
    @Test
    void nestedIncludeIsRelativeToTheIncludingFile() {
        files.put("shaders/3_30/a.glsl", "#include \"utils/b.glsl\"");
        files.put("shaders/3_30/utils/b.glsl", "#include \"c.glsl\"\nb");
        files.put("shaders/3_30/utils/c.glsl", "c");

        ShaderPreprocessor preprocessor = preprocessor(0);
        String source = preprocessor.process("shaders/3_30/a.glsl");

        assertEquals("#line 1 1\n#line 1 2\nc\n#line 2 1\nb\n#line 2 0\n", source);
        assertEquals(List.of("shaders/3_30/a.glsl", "shaders/3_30/utils/b.glsl", "shaders/3_30/utils/c.glsl"),
                preprocessor.files());
    }

    @Test
    void fileIsIncludedOncePerSource() {
        files.put("shaders/3_30/a.glsl", "#include \"lib.glsl\"\n#include \"b.glsl\"\n#include \"lib.glsl\"\nend");
        files.put("shaders/3_30/b.glsl", "#include \"lib.glsl\"\nb");
        files.put("shaders/3_30/lib.glsl", "lib");

        String source = process("shaders/3_30/a.glsl");

        assertEquals(1, source.split("\nlib\n", -1).length - 1);
        assertTrue(source.contains("// #include \"lib.glsl\" (already included)\nb"));
        assertTrue(source.endsWith("// #include \"lib.glsl\" (already included)\nend"));

        // Each source starts again: the same file is included in the next one
        assertTrue(process("shaders/3_30/b.glsl").contains("lib\n"));
    }

    @Test
    void cycleIsRefused() {
        files.put("shaders/3_30/a.glsl", "#include \"b.glsl\"");
        files.put("shaders/3_30/b.glsl", "#include \"c.glsl\"");
        files.put("shaders/3_30/c.glsl", "#include \"b.glsl\"");

        ResourceException error = assertThrows(ResourceException.class, () -> process("shaders/3_30/a.glsl"));
        assertTrue(error.getMessage().contains("shaders/3_30/b.glsl -> shaders/3_30/c.glsl -> shaders/3_30/b.glsl"),
                error.getMessage());
    }

    // The source fails (fallback) instead of being compiled without its include; both paths are watched
    @Test
    void missingIncludeFailsAndIsWatched() {
        files.put("shaders/3_30/2D/a.glsl", "#include \"missing.glsl\"");

        ShaderPreprocessor preprocessor = preprocessor(0);
        ResourceException error = assertThrows(ResourceException.class,
                () -> preprocessor.process("shaders/3_30/2D/a.glsl"));
        assertTrue(error.getMessage().contains("missing.glsl"));
        assertTrue(error.getMessage().contains("shaders/3_30/2D/a.glsl:1"));
        assertEquals(List.of("shaders/3_30/2D/a.glsl", "shaders/3_30/2D/missing.glsl", "shaders/3_30/missing.glsl"),
                preprocessor.watchedFiles());
    }

    @Test
    void missingMainFileFailsAndIsWatched() {
        ShaderPreprocessor preprocessor = preprocessor(0);
        assertThrows(ResourceException.class, () -> preprocessor.process("shaders/3_30/none.glsl"));
        assertEquals(List.of("shaders/3_30/none.glsl"), preprocessor.watchedFiles());
    }

    @Test
    void versionInAnIncludedFileIsRefused() {
        files.put("shaders/3_30/a.glsl", "#version 330 core\n#include \"lib.glsl\"");
        files.put("shaders/3_30/lib.glsl", "  #version 330 core\nint f();");

        ResourceException error = assertThrows(ResourceException.class, () -> process("shaders/3_30/a.glsl"));
        assertTrue(error.getMessage().contains("shaders/3_30/lib.glsl:1"), error.getMessage());
    }

    // A #line before #version would not compile
    @Test
    void includeBeforeVersionIsRefused() {
        files.put("shaders/3_30/a.glsl", "#include \"lib.glsl\"\n#version 330 core");
        files.put("shaders/3_30/lib.glsl", "int f();");

        assertThrows(ResourceException.class, () -> process("shaders/3_30/a.glsl"));
    }

    @Test
    void indentationAndCarriageReturnsAreHandled() {
        files.put("shaders/3_30/a.glsl", "void main() {\r\n    #include \"body.glsl\"\r\n}\r\n");
        files.put("shaders/3_30/body.glsl", "\tint x = 1;\r\n\tint y = 2;\r\n");

        assertEquals("void main() {\n#line 1 1\n\tint x = 1;\n\tint y = 2;\n#line 3 0\n}\n",
                process("shaders/3_30/a.glsl"));
    }

    @Test
    void invalidIncludeSyntaxIsRefused() {
        files.put("shaders/3_30/a.glsl", "#include lib.glsl");

        assertThrows(ResourceException.class, () -> process("shaders/3_30/a.glsl"));
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

    @Test
    void pathsAreNormalized() {
        files.put("shaders/3_30/2D/a.glsl", "#include \"../utils/lib.glsl\"");
        files.put("shaders/3_30/utils/lib.glsl", "lib");

        ShaderPreprocessor preprocessor = preprocessor(0);
        preprocessor.process("shaders/3_30/./2D/a.glsl");
        assertEquals(List.of("shaders/3_30/2D/a.glsl", "shaders/3_30/utils/lib.glsl"), preprocessor.files());
    }
}
