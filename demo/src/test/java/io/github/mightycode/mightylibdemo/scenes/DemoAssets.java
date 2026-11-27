package io.github.mightycode.mightylibdemo.scenes;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The demonstration pack (out of git, python tools/demo_assets.py): present when every file of its manifest
 * (tools/demo_assets.json) is in resources/. Its checksums are the business of demo_assets.py --check.
 */
final class DemoAssets {
    /** True (the CI, which downloads the pack): a scene that needs the pack fails without it instead of being skipped. */
    static final String REQUIRE_PROPERTY = "mightylib.test.requireDemoAssets";

    // Seen from the working directory of the tests (the module folder)
    private static final Path ROOT = Paths.get("..");
    static final Path MANIFEST = ROOT.resolve("tools").resolve("demo_assets.json");
    private static final Path RESOURCES = ROOT.resolve("resources");

    private DemoAssets() {}

    /** The files of the manifest missing from resources/ (paths of the manifest). */
    static List<String> Missing() {
        JSONObject manifest;
        try {
            manifest = new JSONObject(new String(Files.readAllBytes(MANIFEST), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read the manifest of the demonstration pack " + MANIFEST, e);
        }

        List<String> missing = new ArrayList<>();
        for (String file : manifest.getJSONObject("files").keySet()) {
            if (!Files.isRegularFile(RESOURCES.resolve(file)))
                missing.add(file);
        }
        missing.sort(null);
        return missing;
    }

    /** Skips the test without the pack, or fails it when the pack is required (REQUIRE_PROPERTY). */
    static void assumePresent() {
        List<String> missing = Missing();
        if (missing.isEmpty())
            return;

        String reason = "Demonstration pack missing (" + missing.size() + " file(s), first " + missing.get(0)
                + "): run python tools/demo_assets.py";
        if (Boolean.getBoolean(REQUIRE_PROPERTY))
            fail(reason + " (" + REQUIRE_PROPERTY + "=true)");
        assumeTrue(false, reason);
    }
}
