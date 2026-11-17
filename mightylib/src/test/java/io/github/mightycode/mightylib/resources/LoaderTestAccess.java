package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;

import java.nio.file.Path;
import java.util.List;

// A loader or a data of the tests reads its files through the sources of the Resources it belongs to
public final class LoaderTestAccess {
    private LoaderTestAccess() {}

    // The loader attached to a Resources on a stack of this folder only: it reads the logical paths of the folder
    public static <L extends ResourceLoader> L attached(L loader, Path folder) {
        return attached(loader, ResourceSources.of(new FolderSource(folder)));
    }

    public static <L extends ResourceLoader> L attached(L loader, ResourceSources sources) {
        loader.attachTo(new Resources(new Resources.AllResourcesMethod(), 1, sources, List.of(loader)));
        return loader;
    }

    // The data attached to a Resources on a stack of this folder only (estimates, reads by the data itself)
    public static <D extends DataType> D attached(D data, Path folder) {
        data.attachTo(new Resources(new Resources.AllResourcesMethod(), 1,
                ResourceSources.of(new FolderSource(folder)), List.of()));
        return data;
    }
}
