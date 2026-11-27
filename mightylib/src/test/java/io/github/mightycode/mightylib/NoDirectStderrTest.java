package io.github.mightycode.mightylib;

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
 * Guard of the rule "a problem goes through Logger.Error / Logger.Warn" (docs/bonnes-pratiques.md): no direct
 * System.err.print nor printStackTrace() in the sources of mightylib, so that the problem channel (command problems,
 * test kit) sees every problem. Read from the sources (working directory of the tests = folder of the module).
 */
class NoDirectStderrTest {
    static final Pattern FORBIDDEN = Pattern.compile("System\\.err\\.print|\\.printStackTrace\\(\\s*\\)");

    // File (relative to the sources) -> why it writes to System.err itself
    private static final Map<String, String> EXCEPTIONS = Map.of(
            "io/github/mightycode/mightylib/utils/Logger.java",
            "the channel itself; the failure of a listener is printed raw, never sent again (loop)");

    @Test
    void librarySourcesUseTheLogger() throws IOException {
        List<String> found = Find(Paths.get("src/main/java"), EXCEPTIONS);
        assertTrue(found.isEmpty(), "Direct writes to System.err (use Logger.Error / Logger.Warn):\n"
                + String.join("\n", found));
    }

    @Test
    void everyExceptionExistsAndHasAReason() {
        for (Map.Entry<String, String> exception : EXCEPTIONS.entrySet()) {
            assertTrue(Files.isRegularFile(Paths.get("src/main/java").resolve(exception.getKey())), exception.getKey());
            assertFalse(exception.getValue().trim().isEmpty(), exception.getKey());
        }
    }

    // "file:line: text" of every forbidden write outside the exceptions; line comments are skipped
    static List<String> Find(Path sources, Map<String, String> exceptions) throws IOException {
        assertTrue(Files.isDirectory(sources), "No sources at " + sources.toAbsolutePath());
        List<Path> files;
        try (Stream<Path> walk = Files.walk(sources)) {
            files = walk.filter(path -> path.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }

        List<String> found = new ArrayList<>();
        for (Path file : files) {
            String relative = sources.relativize(file).toString().replace('\\', '/');
            if (exceptions.containsKey(relative))
                continue;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); ++i) {
                String line = lines.get(i).trim();
                if (line.startsWith("//") || line.startsWith("*"))
                    continue;
                if (FORBIDDEN.matcher(line).find())
                    found.add(relative + ":" + (i + 1) + ": " + line);
            }
        }
        return found;
    }
}
