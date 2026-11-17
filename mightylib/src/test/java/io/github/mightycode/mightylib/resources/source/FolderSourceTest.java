package io.github.mightycode.mightylib.resources.source;

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

class FolderSourceTest {
    @TempDir
    Path folder;

    private FolderSource withFiles() throws IOException {
        Files.createDirectories(folder.resolve("textures/gui"));
        Files.writeString(folder.resolve("a.json"), "{}");
        Files.writeString(folder.resolve("textures/gui/button.png"), "png!");
        return new FolderSource(folder);
    }

    @Test
    void existsAndIsFolder() throws IOException {
        FolderSource source = withFiles();

        assertTrue(source.exists("a.json"));
        assertTrue(source.exists("textures/gui"));
        assertTrue(source.isFolder("textures/gui"));
        assertFalse(source.isFolder("a.json"));
        assertFalse(source.exists("missing.json"));
        assertTrue(source.isFolder(""));
    }

    @Test
    void openAndSize() throws IOException {
        FolderSource source = withFiles();

        try (InputStream stream = source.open("textures\\gui/button.png")) {
            assertEquals("png!", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(4, source.size("textures/gui/button.png"));
        assertEquals(-1, source.size("missing.png"));
        assertEquals(-1, source.size("textures"));
        assertThrows(NoSuchFileException.class, () -> source.open("missing.png"));
        assertThrows(NoSuchFileException.class, () -> source.open("textures"));
    }

    @Test
    void listsDirectChildrenSorted() throws IOException {
        FolderSource source = withFiles();

        List<SourceEntry> root = source.list("");
        assertEquals(List.of("a.json", "textures"), root.stream().map(SourceEntry::name).collect(Collectors.toList()));
        assertTrue(root.get(1).isFolder());
        assertFalse(root.get(0).isFolder());

        List<SourceEntry> gui = source.list("textures/gui/");
        assertEquals(1, gui.size());
        assertEquals("textures/gui/button.png", gui.get(0).path());
        assertSame(source, gui.get(0).source());

        assertTrue(source.list("missing").isEmpty());
        assertTrue(source.list("a.json").isEmpty());
    }

    @Test
    void localFileExistingOrNot() throws IOException {
        FolderSource source = withFiles();

        assertEquals(folder.resolve("a.json").toAbsolutePath().normalize(), source.localFile("a.json").orElseThrow());
        assertEquals(folder.resolve("new.json").toAbsolutePath().normalize(),
                source.localFile("new.json").orElseThrow());
    }

    @Test
    void refusesPathsOutsideOfTheRoot() throws IOException {
        FolderSource source = withFiles();

        assertThrows(IllegalArgumentException.class, () -> source.exists("../a.json"));
        assertThrows(IllegalArgumentException.class, () -> source.open("textures/../../a.json"));
        assertThrows(IllegalArgumentException.class, () -> source.list("/textures"));
        assertThrows(IllegalArgumentException.class, () -> source.localFile("C:/a.json"));
    }

    @Test
    void missingFolderIsEmpty() {
        FolderSource source = new FolderSource(folder.resolve("absent"));

        assertFalse(source.exists("a.json"));
        assertTrue(source.list("").isEmpty());
        assertEquals(-1, source.size("a.json"));
    }

    @Test
    void nameShowsTheFolder() {
        assertEquals("folder:resources/", new FolderSource("resources").name());
        assertEquals("folder:resources/", new FolderSource("resources/").name());
    }
}
