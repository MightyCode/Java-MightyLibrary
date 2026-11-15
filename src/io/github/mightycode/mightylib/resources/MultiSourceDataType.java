package io.github.mightycode.mightylib.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Data read from several files given at its creation (the faces of a cubemap, the layers of a picture).
 * The loader marks each file once read (markSourceRead): the state is reset before each reading, a reading that does
 * not read every file fails and names the missing ones, and the command "resources" shows the state of each file.
 * A file changed on the disk reads the whole data again.
 */
public abstract class MultiSourceDataType extends DataType {
    private final List<String> sources;
    // Written by the worker reading the data, read by the main thread after the step (the scheduler orders them)
    private final boolean[] sourceRead;

    protected MultiSourceDataType(TYPE_SET_UP typeSetUp, String dataName, List<String> paths) {
        super(typeSetUp, dataName);

        sources = Collections.unmodifiableList(new ArrayList<>(paths));
        sourceRead = new boolean[sources.size()];
    }

    protected MultiSourceDataType(TYPE_SET_UP typeSetUp, String dataName, String... paths) {
        this(typeSetUp, dataName, Arrays.asList(paths));
    }

    public final String sourcePath(int index) {
        return sources.get(index);
    }

    public final int sourceCount() {
        return sources.size();
    }

    // Called by the loader once the file is read (any thread)
    public final synchronized void markSourceRead(int index) {
        sourceRead[index] = true;
    }

    public final synchronized boolean isSourceRead(int index) {
        return sourceRead[index];
    }

    // The files not read by the last reading, all of them before the first one
    public final synchronized List<String> unreadSources() {
        List<String> unread = new ArrayList<>();
        for (int i = 0; i < sourceRead.length; ++i) {
            if (!sourceRead[i])
                unread.add(sources.get(i));
        }

        return unread;
    }

    // Before each reading (ResourceLoader.read)
    final synchronized void resetSourcesRead() {
        Arrays.fill(sourceRead, false);
    }

    @Override
    public final List<String> sourcePaths() {
        return validPaths() ? sources : Collections.emptyList();
    }

    @Override
    public final boolean hasSource() {
        return validPaths();
    }

    public final boolean validPaths() {
        for (String path : sources) {
            if (path == null || path.isEmpty())
                return false;
        }

        return true;
    }
}
