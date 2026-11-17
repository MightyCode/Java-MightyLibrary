package io.github.mightycode.mightylib.resources.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ResourceSourcesTest {
    @TempDir
    Path low;
    @TempDir
    Path high;

    private FolderSource lowSource;
    private FolderSource highSource;
    private ResourceSources stack;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(low.resolve("textures"));
        Files.createDirectories(high.resolve("textures"));
        Files.writeString(low.resolve("textures/a.png"), "low a");
        Files.writeString(low.resolve("textures/onlyLow.png"), "low");
        Files.writeString(high.resolve("textures/a.png"), "high a!");
        Files.writeString(high.resolve("textures/onlyHigh.png"), "high");

        lowSource = new FolderSource(low);
        highSource = new FolderSource(high);
        stack = ResourceSources.builder().add("mightylib", lowSource).add("demo", highSource).build();
    }

    @Test
    void aFolderOfAHigherSourceDoesNotHideAFile() throws IOException {
        // "textures/x" is a file below, a folder above
        Files.writeString(low.resolve("textures/x"), "low x");
        Files.createDirectories(high.resolve("textures/x"));

        assertSame(highSource, stack.resolve("textures/x").orElseThrow());
        assertEquals("low x", stack.readString("textures/x"));
        assertEquals(5, stack.size("textures/x"));
        assertEquals(low.resolve("textures/x").toAbsolutePath().normalize(),
                stack.localFile("textures/x").orElseThrow());
        ByteBuffer buffer = stack.readNative("textures/x");
        try {
            assertEquals(5, buffer.remaining());
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    @Test
    void theHighestSourceWins() throws IOException {
        assertSame(highSource, stack.resolve("textures/a.png").orElseThrow());
        assertSame(lowSource, stack.resolve("textures/onlyLow.png").orElseThrow());
        assertEquals("high a!", stack.readString("textures/a.png"));
        assertEquals("low", stack.readString("textures/onlyLow.png"));
        assertEquals(7, stack.size("textures/a.png"));
        assertEquals(-1, stack.size("textures/missing.png"));
        assertTrue(stack.resolve("missing").isEmpty());
        assertThrows(NoSuchFileException.class, () -> stack.readBytes("missing"));
    }

    @Test
    void listIsTheUnionWithTheHighestSource() {
        List<SourceEntry> entries = stack.list("textures");

        assertEquals(List.of("a.png", "onlyHigh.png", "onlyLow.png"),
                entries.stream().map(SourceEntry::name).collect(Collectors.toList()));
        assertSame(highSource, entries.get(0).source());
        assertSame(lowSource, entries.get(2).source());
    }

    @Test
    void layersFromTheLowest() {
        assertEquals(List.of(lowSource, highSource), stack.layers("textures/a.png"));
        assertEquals(List.of(highSource), stack.layers("textures/onlyHigh.png"));
        assertTrue(stack.layers("missing").isEmpty());
    }

    @Test
    void localCandidatesInEverySourceOnTheDisk() {
        assertEquals(List.of(low.resolve("textures/new.png").toAbsolutePath().normalize(),
                        high.resolve("textures/new.png").toAbsolutePath().normalize()),
                stack.localCandidates("textures/new.png"));
        assertEquals(high.resolve("textures/a.png").toAbsolutePath().normalize(),
                stack.localFile("textures/a.png").orElseThrow());
        assertTrue(stack.localFile("textures/new.png").isEmpty());
    }

    @Test
    void readNative() throws IOException {
        ByteBuffer buffer = stack.readNative("textures/a.png");
        try {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            assertEquals("high a!", new String(bytes, StandardCharsets.UTF_8));
        } finally {
            MemoryUtil.memFree(buffer);
        }

        // From a source that is not on the disk
        ResourceSources classpath = ResourceSources.of(new ClasspathSource(getClass().getClassLoader(),
                "sources/classpath"));
        ByteBuffer fromClasspath = classpath.readNative("sub/b.txt");
        try {
            assertEquals(11, fromClasspath.remaining());
        } finally {
            MemoryUtil.memFree(fromClasspath);
        }
    }

    @Test
    void owners() {
        assertEquals("mightylib", stack.owner(lowSource));
        assertEquals("demo", stack.owner(highSource));
        assertThrows(IllegalArgumentException.class, () -> stack.owner(new FolderSource(low)));
        assertThrows(IllegalArgumentException.class, () -> ResourceSources.of(lowSource, lowSource));
    }

    @Test
    void normalize() {
        assertEquals("textures/a.png", ResourceSources.normalize("./textures//a.png"));
        assertEquals("textures/gui", ResourceSources.normalize("textures\\gui\\"));
        assertEquals("", ResourceSources.normalize(""));
        assertEquals("", ResourceSources.normalize("./"));
        assertThrows(IllegalArgumentException.class, () -> ResourceSources.normalize("/textures"));
        assertThrows(IllegalArgumentException.class, () -> ResourceSources.normalize("C:\\textures"));
        assertThrows(IllegalArgumentException.class, () -> ResourceSources.normalize("textures/../../a"));
        assertThrows(IllegalArgumentException.class, () -> ResourceSources.normalize(".."));
    }
}
