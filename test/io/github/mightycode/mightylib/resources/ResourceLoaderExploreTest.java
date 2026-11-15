package io.github.mightycode.mightylib.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

// The folders skipped by ResourceLoader.exploreResourcesFile: the given list applies at any depth
class ResourceLoaderExploreTest {
    // Names every file it is given (relative path), detects nothing
    private static final class ListingLoader extends FakeLoader {
        private final Path root;
        final Set<String> seen = new TreeSet<>();

        ListingLoader(Path root) {
            this.root = root;
        }

        @Override
        public String filterFile(String path) {
            seen.add(root.relativize(Path.of(path)).toString().replace(File.separatorChar, '/'));
            return null;
        }
    }

    private static void file(Path root, String relative) throws IOException {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.write(path, new byte[0]);
    }

    private static Set<String> explore(Path root, List<String> ignored) {
        ListingLoader loader = new ListingLoader(root);
        Map<String, DataType> data = new HashMap<>();
        if (ignored == null)
            loader.exploreResourcesFile(data, root.toString());
        else
            loader.exploreResourcesFile(data, root.toString(), ignored);
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
        assertEquals(Set.of("a.txt", "sub/d.txt", "skip/e.txt", "sub/skip/f.txt"), explore(root, null));
    }

    @Test
    void givenFoldersAreSkippedAtAnyDepth(@TempDir Path root) throws IOException {
        tree(root);
        assertEquals(Set.of("a.txt", "batchs/b.txt", "sub/batchs/c.txt", "sub/d.txt"),
                explore(root, List.of("skip")));
    }

    @Test
    void emptyListSkipsNothing(@TempDir Path root) throws IOException {
        tree(root);
        assertEquals(6, explore(root, List.of()).size());
    }
}
