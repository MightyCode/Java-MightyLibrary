package io.github.mightycode.mightylibdemo.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The demo draws its texts with the fonts of the engine (free license): no proprietary font comes back in resources/
class DemoFontsTest {
    private static final Path RESOURCES = Path.of("..", "resources");

    @Test
    void noProprietaryFontInTheResources() throws IOException {
        assertTrue(Files.isDirectory(RESOURCES), RESOURCES.toAbsolutePath().toString());

        List<String> found;
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            found = files.map(file -> file.getFileName().toString().toLowerCase(Locale.ROOT))
                    .filter(name -> name.startsWith("arial") || name.startsWith("bahnschrift"))
                    .collect(Collectors.toList());
        }

        assertEquals(List.of(), found);
    }
}
