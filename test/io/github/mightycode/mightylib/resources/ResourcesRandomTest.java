package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;

/**
 * Random load and unload cycles: the final state must stay consistent.
 */
class ResourcesRandomTest extends ResourcesTestBase {
    private void randomCycles(int seed) throws Exception {
        resources = new Resources(new Resources.BatchResourcesMethod("unused"), WORKERS,
                Arrays.asList(fakeLoader = new FakeLoader(), new JSONLoader(), new BatchLoader(new JSONLoader())));
        all.clear();

        String[] names = new String[12];
        for (int i = 0; i < names.length; ++i) {
            FakeData data = fake(i % 3 == 0 ? MAIN_CONTEXT : THREAD_CONTEXT, "data" + i);
            data.preloadMilliseconds = i % 4;
            names[i] = data.getDataName();
        }
        // A few dependencies, some of them crossing batches (data4 -> data9 is in no batch with data4 but b)
        all.get(0).dependencyToAdd = all.get(7);
        all.get(4).dependencyToAdd = all.get(9);
        all.get(10).dependencyToAdd = all.get(1);
        batch("a", Arrays.copyOfRange(names, 0, 6));
        batch("b", Arrays.copyOfRange(names, 4, 10));
        batch("c", Arrays.copyOfRange(names, 8, 12));
        start();

        String[] batches = {"a", "b", "c"};
        boolean[] loaded = new boolean[batches.length];
        Random random = new Random(seed);

        for (int step = 0; step < 300; ++step) {
            int index = random.nextInt(batches.length);
            if (loaded[index])
                resources.unloadBatch(batches[index]);
            else
                resources.loadBatch(batches[index]);
            loaded[index] = !loaded[index];

            int frames = random.nextInt(4);
            for (int f = 0; f < frames; ++f)
                frame();

            if (step % 50 == 49) {
                pump();
                assertConsistent();
            }
        }

        pump();
        assertConsistent();
    }

    @Test
    void randomCyclesWithOtherSeeds() throws Exception {
        for (int seed = 1; seed <= 5; ++seed)
            randomCycles(seed);
    }

    // Random load / unload of overlapping batches, with a few frames between them, then a final check
    @Test
    void randomCyclesStayConsistent() throws Exception {
        randomCycles(42);
    }
}
