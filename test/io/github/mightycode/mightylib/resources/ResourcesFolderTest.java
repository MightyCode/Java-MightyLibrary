package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.resources.data.JSONFile;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The folder of the resources belongs to each Resources, given at its construction
class ResourcesFolderTest {
    @TempDir
    Path first;
    @TempDir
    Path second;

    @BeforeEach
    void setUp() {
        MainThreadTestAccess.setCurrentAsMain();
    }

    private static Resources create(Path folder, String jsonName) throws IOException {
        Files.write(folder.resolve(jsonName + ".json"), "{}".getBytes(StandardCharsets.UTF_8));
        Resources resources = new Resources(new Resources.AllResourcesMethod(), 1, folder.toString(),
                List.of(new JSONLoader()));
        resources.init();
        return resources;
    }

    @Test
    void twoResourcesOnTwoFoldersDontSeeEachOther() throws IOException {
        Resources a = create(first, "onlyInFirst");
        Resources b = create(second, "onlyInSecond");

        assertEquals(List.of("onlyInFirst"), a.getResourceNames(JSONFile.class));
        assertEquals(List.of("onlyInSecond"), b.getResourceNames(JSONFile.class));
        assertNotEquals(a.folder(), b.folder());
    }

    @Test
    void folderEndsWithASlashAndUsesForwardSlashes() {
        Resources resources = new Resources(new Resources.AllResourcesMethod(), 1, "some\\folder", List.of());
        assertEquals("some/folder/", resources.folder());

        resources = new Resources(new Resources.AllResourcesMethod(), 1, "other/", List.of());
        assertEquals("other/", resources.folder());
    }
}
