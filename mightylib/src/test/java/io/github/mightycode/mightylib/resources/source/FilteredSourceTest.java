package io.github.mightycode.mightylib.resources.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// A source limited to one folder of another (the layer of the player: configs/ of its data folder)
class FilteredSourceTest {
    @TempDir
    Path folder;

    private FilteredSource source;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(folder.resolve("configs/game"));
        Files.createDirectories(folder.resolve("saves"));
        Files.write(folder.resolve("configs/game/display.json"), "{}".getBytes(StandardCharsets.UTF_8));
        Files.write(folder.resolve("saves/slot1.json"), "{}".getBytes(StandardCharsets.UTF_8));
        Files.write(folder.resolve("root.json"), "{}".getBytes(StandardCharsets.UTF_8));
        source = new FilteredSource(new FolderSource(folder), "configs");
    }

    @Test
    void onlyTheFolderExists() throws IOException {
        assertTrue(source.exists("configs/game/display.json"));
        assertTrue(source.isFolder("configs"));
        assertTrue(source.exists(""));
        assertFalse(source.exists("saves/slot1.json"));
        assertFalse(source.exists("saves"));
        assertFalse(source.exists("root.json"));
        assertEquals(2, source.size("configs/game/display.json"));
        assertEquals(-1, source.size("root.json"));

        try (InputStream input = source.open("configs/game/display.json")) {
            assertEquals("{}", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThrows(NoSuchFileException.class, () -> source.open("saves/slot1.json"));
    }

    // The entries belong to the filtered source (the stack knows it, not the inner one)
    @Test
    void listShowsOnlyTheFolder() {
        assertEquals(List.of("configs"), names(source.list("")));
        assertEquals(List.of("configs/game"), names(source.list("configs")));
        assertSame(source, source.list("configs").get(0).source());
        assertTrue(source.list("saves").isEmpty());

        ResourceSources stack = ResourceSources.builder().add(ResourceSources.PLAYER_OWNER, source).build();
        assertEquals(ResourceSources.PLAYER_OWNER, stack.owner(stack.list("configs").get(0).source()));
    }

    @Test
    void localFilesOfTheFolderAndItsParentsOnly() {
        assertEquals(folder.resolve("configs/game/display.json").toAbsolutePath().normalize(),
                source.localFile("configs/game/display.json").orElseThrow());
        assertTrue(source.localFile("configs/game/new.json").isPresent());
        // The root: the resource watcher follows the folders
        assertTrue(source.localFile("").isPresent());
        assertTrue(source.localFile("saves/slot1.json").isEmpty());
    }

    @Test
    void deeperFolderAndRootRefused() {
        FilteredSource deep = new FilteredSource(new FolderSource(folder), "configs/game");
        assertEquals(List.of("configs"), names(deep.list("")));
        assertEquals(List.of("configs/game"), names(deep.list("configs")));
        assertTrue(deep.exists("configs/game/display.json"));

        assertThrows(IllegalArgumentException.class, () -> new FilteredSource(new FolderSource(folder), ""));
        assertThrows(IllegalArgumentException.class, () -> new FilteredSource(new FolderSource(folder), "../x"));
    }

    private static List<String> names(List<SourceEntry> entries) {
        return entries.stream().map(SourceEntry::path).collect(Collectors.toList());
    }
}
