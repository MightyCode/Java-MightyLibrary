package io.github.mightycode.mightylib.resources;

import java.nio.file.Path;
import java.util.List;

// A loader of the tests reads its files through the sources of the Resources it serves
public final class LoaderTestAccess {
    private LoaderTestAccess() {}

    // The loader attached to a Resources on a stack of this folder only: it reads the logical paths of the folder
    public static <L extends ResourceLoader> L attached(L loader, Path folder) {
        Resources resources = new Resources(new Resources.AllResourcesMethod(), 1, folder.toString(), List.of(loader));
        loader.attachTo(resources);
        return loader;
    }
}
