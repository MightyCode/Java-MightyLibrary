package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/** A data read from several files: paths, reading skipped without paths, state of each file, failures naming them. */
class MultiSourceDataTest extends ResourcesTestBase {
    static class PairData extends MultiSourceDataType {
        PairData(String name, String... paths) {
            super(THREAD_CONTEXT, name, paths);
        }

        @Override
        protected boolean internLoad() {
            return true;
        }

        @Override
        protected void internUnload() {
        }
    }

    static class PairLoader extends ResourceLoader {
        final List<PairData> toCreate = new ArrayList<>();
        final AtomicInteger readings = new AtomicInteger();
        // A loader that forgets to mark its files
        volatile boolean markFiles = true;

        @Override
        public Class<? extends DataType> getType() {
            return PairData.class;
        }

        @Override
        public String getResourceNameType() {
            return "Pair";
        }

        @Override
        public void create(Map<String, DataType> data) {
            for (PairData pair : toCreate)
                data.put(pair.getDataName(), pair);
        }

        @Override
        public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        }

        @Override
        public String filterFile(String path) {
            return null;
        }

        @Override
        protected void initWithFile(DataType dataType) {
            readings.incrementAndGet();
            PairData pair = (PairData) dataType;
            for (int i = 0; i < pair.sourceCount(); ++i) {
                try {
                    Files.readAllBytes(Path.of(pair.sourcePath(i)));
                } catch (IOException e) {
                    throw new ResourceException("can't read " + pair.sourcePath(i), e);
                }
                if (markFiles)
                    pair.markSourceRead(i);
            }
        }
    }

    private PairLoader pairLoader;
    private String first, second;

    @BeforeEach
    void setUpPairs() throws IOException {
        pairLoader = new PairLoader();
        resources = new Resources(new Resources.BatchResourcesMethod("unused"), WORKERS, folder.toString(),
                Arrays.asList(pairLoader, new JSONLoader(), new BatchLoader()));

        first = folder.resolve("first.txt").toString();
        second = folder.resolve("second.txt").toString();
        Files.write(Path.of(first), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(Path.of(second), "2".getBytes(StandardCharsets.UTF_8));
    }

    private PairData pair(String name, String... paths) throws IOException {
        PairData pair = new PairData(name, paths);
        pairLoader.toCreate.add(pair);
        Files.write(folder.resolve("batchs").resolve(name + ".batch"),
                ("{\"batchs\":[{\"type\":\"Pair\",\"files\":[\"" + name + "\"],\"regex\":[]}]}")
                        .getBytes(StandardCharsets.UTF_8));
        return pair;
    }

    @Test
    void pathsAreFixedAndListed() throws IOException {
        PairData pair = pair("pair", first, second);

        assertEquals(List.of(first, second), pair.sourcePaths());
        assertTrue(pair.hasSource());
        assertEquals(2, pair.sourceCount());
        assertThrows(UnsupportedOperationException.class, () -> pair.sourcePaths().add("other"));
        assertEquals(List.of(first, second), pair.unreadSources());
    }

    @Test
    void readingIsSkippedWhenAPathIsEmpty() throws Exception {
        PairData pair = pair("pair", first, "");
        start();
        resources.loadBatch("pair");
        pump();

        assertFalse(pair.hasSource());
        assertTrue(pair.sourcePaths().isEmpty());
        assertEquals(0, pairLoader.readings.get());
    }

    @Test
    void eachFileIsMarkedThenResetBeforeTheNextReading() throws Exception {
        PairData pair = pair("pair", first, second);
        start();
        resources.loadBatch("pair");
        pump();

        assertTrue(pair.isLoaded());
        assertTrue(pair.isSourceRead(0));
        assertTrue(pair.isSourceRead(1));

        // Read again without its second file: the state of the first reading is not kept
        Files.delete(Path.of(second));
        String errors = captureErrors(() -> resources.reload(pair));

        assertTrue(pair.isSourceRead(0));
        assertFalse(pair.isSourceRead(1));
        assertTrue(errors.contains("not read: " + second), errors);
        assertFalse(errors.contains("not read: " + first), errors);

        JSONArray files = resources.describe(ResourceSelection.ofType(PairData.class).withNames("pair"))
                .getJSONArray("data").getJSONObject(0).getJSONArray("sourceFiles");
        assertEquals(2, files.length());
        JSONObject secondFile = files.getJSONObject(1);
        assertEquals(second, secondFile.getString("path"));
        assertFalse(secondFile.getBoolean("read"));
        assertTrue(files.getJSONObject(0).getBoolean("read"));
    }

    @Test
    void aLoaderThatDoesNotMarkItsFilesFails() throws Exception {
        PairData pair = pair("pair", first, second);
        pairLoader.markFiles = false;
        start();

        String errors = loadCapturingErrors("pair");

        assertFalse(pair.isLoaded());
        assertTrue(pair.hasFailed());
        assertTrue(errors.contains("files not read: " + first + ", " + second), errors);
    }

    private String captureErrors(Runnable action) throws Exception {
        PrintStream previous = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            action.run();
            pump();
        } finally {
            System.setErr(previous);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}
