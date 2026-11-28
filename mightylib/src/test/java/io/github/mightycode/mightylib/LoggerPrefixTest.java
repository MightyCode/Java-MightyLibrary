package io.github.mightycode.mightylib;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard of the rule "the prefix -- is added by Logger" (CLAUDE.md, logs of the engine): no message given to
 * Logger.LogException, Logger.Error or Logger.Warn starts with "--", whose line would start with "----" (the
 * registration of the plugins in Engine and the redraw of a window in Context did, fixed in November 2026). Read
 * from the sources of the library and of the demo (working directory of the tests = folder of the module).
 */
class LoggerPrefixTest {
    // The message is the first argument of LogException, the second of Error and Warn (after the category), possibly
    // on the next line
    static final Pattern PREFIXED = Pattern.compile(
            "Logger\\.(?:LogException|Error|Warn)\\(\\s*(?:[\\w.]+\\s*,\\s*)?\"--");

    @Test
    void noMessageGivenToTheLoggerHasItsPrefix() throws IOException {
        List<String> found = new ArrayList<>(Find(Paths.get("src/main/java")));
        found.addAll(Find(Paths.get("../demo/src/main/java")));
        found.addAll(Find(Paths.get("../mightylib-test/src/main/java")));
        assertTrue(found.isEmpty(), "Messages starting with -- (Logger adds it):\n" + String.join("\n", found));
    }

    @Test
    void thePatternFindsThePrefixedMessages() {
        assertTrue(PREFIXED.matcher("Logger.LogException(\"--The registration failed\", e);").find());
        assertTrue(PREFIXED.matcher("Logger.Error(Logger.CATEGORY_GL,\n        \"--Uniform not found\");").find());
        assertTrue(PREFIXED.matcher("Logger.Warn(CATEGORY, \"--Unknown property\");").find());
        assertTrue(!PREFIXED.matcher("Logger.Warn(CATEGORY, \"Unknown property --x\");").find());
        assertTrue(!PREFIXED.matcher("System.out.println(\"--Load batch\");").find());
    }

    // "file:line: start of the call" of every prefixed message
    static List<String> Find(Path sources) throws IOException {
        assertTrue(Files.isDirectory(sources), "No sources at " + sources.toAbsolutePath());
        List<Path> files;
        try (Stream<Path> walk = Files.walk(sources)) {
            files = walk.filter(path -> path.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }

        List<String> found = new ArrayList<>();
        for (Path file : files) {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher matcher = PREFIXED.matcher(content);
            while (matcher.find()) {
                int line = 1;
                for (int i = 0; i < matcher.start(); ++i) {
                    if (content.charAt(i) == '\n')
                        ++line;
                }
                found.add(sources.relativize(file).toString().replace('\\', '/') + ":" + line + ": "
                        + matcher.group().replace('\n', ' '));
            }
        }
        return found;
    }
}
