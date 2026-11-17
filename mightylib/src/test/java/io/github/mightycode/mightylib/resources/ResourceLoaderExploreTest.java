package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.data.JSONFile;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

// ResourceLoader.exploreResourcesFile on the sources: logical paths, skipped folders at any depth, names in two sources
class ResourceLoaderExploreTest {
    // Names every file it is given, detects nothing
    private static final class ListingLoader extends FakeLoader {
        final Set<String> seen = new TreeSet<>();

        @Override
        public String filterFile(String path) {
            seen.add(path);
            return null;
        }
    }

    private static void file(Path root, String relative) throws IOException {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "{}");
    }

    private static Set<String> explore(Path root, String folder, List<String> ignored) {
        ListingLoader loader = LoaderTestAccess.attached(new ListingLoader(), root);
        Map<String, DataType> data = new HashMap<>();
        if (ignored == null)
            loader.exploreResourcesFile(data, folder);
        else
            loader.exploreResourcesFile(data, folder, ignored);
        return loader.seen;
    }

    private static void tree(Path root) throws IOException {
        file(root, "a.txt");
        file(root, "batchs/b.txt");
        file(root, "sub/batchs/c.txt");
        file(root, "sub/d.txt");
        file(root, "skip/e.txt");
        file(root, "sub/skip/f.txt");
    }

    @Test
    void batchFoldersAreSkippedByDefault(@TempDir Path root) throws IOException {
        tree(root);
        assertEquals(Set.of("a.txt", "sub/d.txt", "skip/e.txt", "sub/skip/f.txt"), explore(root, "", null));
    }

    @Test
    void givenFoldersAreSkippedAtAnyDepth(@TempDir Path root) throws IOException {
        tree(root);
        assertEquals(Set.of("a.txt", "batchs/b.txt", "sub/batchs/c.txt", "sub/d.txt"),
                explore(root, "", List.of("skip")));
    }

    @Test
    void emptyListSkipsNothing(@TempDir Path root) throws IOException {
        tree(root);
        assertEquals(6, explore(root, "", List.of()).size());
    }

    @Test
    void aFolderOrAFile(@TempDir Path root) throws IOException {
        tree(root);
        assertEquals(Set.of("sub/d.txt", "sub/skip/f.txt"), explore(root, "sub", List.of("batchs")));
        assertEquals(Set.of("sub/d.txt"), explore(root, "sub/d.txt", null));
        assertTrue(explore(root, "missing", null).isEmpty());
        // The folder given is skipped too when its name is ignored
        assertTrue(explore(root, "skip", List.of("skip")).isEmpty());
    }

    // ---- Two sources ----

    private static Map<String, DataType> exploreJson(Path low, Path high, boolean strict) {
        ResourceLoader loader = new JSONLoader();
        Resources resources = new Resources(new Resources.AllResourcesMethod(), 1,
                ResourceSources.of(new FolderSource(low), new FolderSource(high)), List.of(loader));
        resources.setStrict(strict);
        loader.attachTo(resources);
        Map<String, DataType> data = new HashMap<>();
        loader.exploreResourcesFile(data, "");
        return data;
    }

    @Test
    void aPathInTwoSourcesIsDetectedOnce(@TempDir Path low, @TempDir Path high) throws IOException {
        file(low, "data/a.json");
        file(high, "data/a.json");
        file(low, "data/onlyLow.json");
        file(high, "onlyHigh.json");

        Map<String, DataType> data = exploreJson(low, high, false);

        assertEquals(Set.of("a", "onlyLow", "onlyHigh"), data.keySet());
        assertEquals("data/a.json", ((JSONFile) data.get("a")).path());
        assertEquals("onlyHigh.json", ((JSONFile) data.get("onlyHigh")).path());
    }

    @Test
    void aNameInTwoSourcesKeepsTheHighestOne(@TempDir Path low, @TempDir Path high) throws IOException {
        // Explored in the order of the paths: "a/x.json" (high) before "b/x.json" (low)
        file(high, "a/x.json");
        file(low, "b/x.json");
        // And the other way: "c/y.json" (low) before "d/y.json" (high)
        file(low, "c/y.json");
        file(high, "d/y.json");

        Map<String, DataType> data = exploreJson(low, high, false);

        assertEquals("a/x.json", ((JSONFile) data.get("x")).path());
        assertEquals("d/y.json", ((JSONFile) data.get("y")).path());
    }

    @Test
    void aNameInTwoSourcesThrowsInStrictMode(@TempDir Path low, @TempDir Path high) throws IOException {
        file(low, "a/x.json");
        file(high, "b/x.json");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> exploreJson(low, high, true));
        assertTrue(e.getMessage().contains("b/x.json"), e.getMessage());
        assertTrue(e.getMessage().contains("overrides a/x.json"), e.getMessage());
    }

    // Names every file by its file name, stores nothing, records what it is given
    private static final class RecordingLoader extends FakeLoader {
        final List<String> detected = new ArrayList<>();

        @Override
        public String filterFile(String path) {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        @Override
        public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
            detected.add(currentPath);
        }
    }

    @Test
    void theLosingFileIsNeverGivenToTheLoader(@TempDir Path low, @TempDir Path high) throws IOException {
        // The high source first ("a/" before "b/"), then the low one: the loader stores nothing under the name
        file(high, "a/x.json");
        file(low, "b/x.json");
        file(low, "c/y.json");
        file(high, "d/y.json");

        RecordingLoader loader = LoaderTestAccess.attached(new RecordingLoader(),
                ResourceSources.of(new FolderSource(low), new FolderSource(high)));
        Map<String, DataType> data = new ConcurrentHashMap<>();
        loader.exploreResourcesFile(data, "");

        // The low y.json, explored first, is given, then replaced by the high one
        assertEquals(List.of("a/x.json", "c/y.json", "d/y.json"), loader.detected);
        assertTrue(data.isEmpty());
    }

    @Test
    void aNameTwiceInOneSourceIsLeftToTheLoader(@TempDir Path low, @TempDir Path high) throws IOException {
        file(low, "a/x.json");
        file(low, "b/x.json");

        // The last detected wins (JSONLoader), even in strict mode
        assertEquals("b/x.json", ((JSONFile) exploreJson(low, high, true).get("x")).path());
    }
}
