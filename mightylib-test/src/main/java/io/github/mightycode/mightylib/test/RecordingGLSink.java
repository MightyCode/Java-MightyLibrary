package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.internal.IGLElementSink;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Without GL: keeps which data had their GL element asked for (and how many times), sends nothing
final class RecordingGLSink implements IGLElementSink {
    private final Map<DataType, Integer> references = new LinkedHashMap<>();

    @Override
    public GLElement acquire(DataType data, boolean urgent) {
        references.merge(data, 1, Integer::sum);
        // No GL element: the data is loaded on the CPU only
        return null;
    }

    @Override
    public void release(GLElement element, boolean urgent) {
        // acquire never gives an element: nothing comes back
    }

    Map<DataType, Integer> references() {
        return Collections.unmodifiableMap(references);
    }
}
