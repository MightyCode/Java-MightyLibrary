package io.github.mightycode.mightylibdemo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The demo follows the rule of the library (NoDirectStderrTest of mightylib): its problems go through
 * Logger.Error / Logger.Warn, no direct System.err.print nor printStackTrace(). Read from the sources (working
 * directory of the tests = folder of the module).
 */
class DemoNoDirectStderrTest {
    private static final Pattern FORBIDDEN = Pattern.compile("System\\.err\\.print|\\.printStackTrace\\(\\s*\\)");
    private static final Path SOURCES = Paths.get("src/main/java");

    // File (relative to the sources) -> why it writes to System.err itself
    private static final Map<String, String> EXCEPTIONS = Map.of(
            "io/github/mightycode/mightylibdemo/scenes/ConsoleFeedScene.java",
            "shows how the console shows the raw program output: one of its lines is a raw System.err line");

    @Test
    void demoSourcesUseTheLogger() throws IOException {
        assertTrue(Files.isDirectory(SOURCES), "No sources at " + SOURCES.toAbsolutePath());
        List<Path> files;
        try (Stream<Path> walk = Files.walk(SOURCES)) {
            files = walk.filter(path -> path.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }

        List<String> found = new ArrayList<>();
        for (Path file : files) {
            String relative = SOURCES.relativize(file).toString().replace('\\', '/');
            if (EXCEPTIONS.containsKey(relative))
                continue;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); ++i) {
                String line = lines.get(i).trim();
                if (!line.startsWith("//") && !line.startsWith("*") && FORBIDDEN.matcher(line).find())
                    found.add(relative + ":" + (i + 1) + ": " + line);
            }
        }
        assertTrue(found.isEmpty(), "Direct writes to System.err (use Logger.Error / Logger.Warn):\n"
                + String.join("\n", found));
    }

    @Test
    void everyExceptionExistsAndHasAReason() {
        for (Map.Entry<String, String> exception : EXCEPTIONS.entrySet()) {
            assertTrue(Files.isRegularFile(SOURCES.resolve(exception.getKey())), exception.getKey());
            assertFalse(exception.getValue().trim().isEmpty(), exception.getKey());
        }
    }
}
