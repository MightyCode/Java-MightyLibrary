package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.graphics.shader.ShaderLoader;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.resources.sound.SoundData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The loaders are given at the construction of Resources (those of the plugins, the engine first), then fixed
class ResourceLoadersTest {
    private static Resources create(List<ResourceLoader> loaders) {
        return new Resources(new Resources.AllResourcesMethod(), 1, Resources.DEFAULT_FOLDER, loaders);
    }

    private static List<ResourceLoader> mandatory() {
        ShaderManager shaders = new ShaderManager(Resources.DEFAULT_FOLDER);
        return LibraryLoaders.mandatory(() -> shaders);
    }

    @Test
    void registeredLoadersKeepTheirOrder() {
        FakeLoader first = new FakeLoader();
        CsvLikeLoader second = new CsvLikeLoader();
        List<ResourceLoader> loaders = create(List.of(first, second)).loaders();

        assertEquals(List.of(first, second), loaders);
    }

    // The loader of the shaders is registered before the shader manager exists: it reads it when it works
    @Test
    void shaderLoaderReadsItsManagerLate() {
        ShaderManager[] manager = new ShaderManager[1];
        ShaderLoader loader = new ShaderLoader(() -> manager[0]);
        assertThrows(IllegalStateException.class, loader::createFallback);

        manager[0] = new ShaderManager(Resources.DEFAULT_FOLDER);
        assertInstanceOf(ShaderLoader.class, LibraryLoaders.mandatory(() -> manager[0]).get(0));
    }

    // The mandatory loaders and the optional ones of the library: one per type, new instances at each call
    @Test
    void libraryLoadersHaveDistinctTypes() {
        List<ResourceLoader> library = new ArrayList<>(mandatory());
        library.addAll(LibraryLoaders.all());

        Resources resources = create(library);
        assertEquals(library.size(), resources.loaders().size());
        assertEquals(1, library.stream().filter(loader -> loader instanceof BatchLoader).count());
        assertNotSame(LibraryLoaders.all().get(0), LibraryLoaders.all().get(0));
    }

    // A type used without its loader (sounds of a game that did not register SoundLoader): the loader to register
    @Test
    void typeWithoutLoaderNamesItsLoader() {
        Resources resources = create(List.of());
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> resources.getResource(SoundData.class, "any"));
        assertTrue(error.getMessage().contains("SoundLoader"), error.getMessage());
        assertTrue(Resources.NoLoaderMessage(CsvLikeData.class).contains("its loader"));
    }

    // The shader sources are a data like the others (ShaderData), their program a GL element
    @Test
    void shaderLoaderServesShaderData() {
        Resources resources = create(mandatory());
        assertSame(ShaderData.class, resources.getClassFromName("ShaderData"));
        assertNull(resources.getClassFromName("Shader"));
    }

    @Test
    void listCantBeChanged() {
        Resources resources = create(List.of());
        List<ResourceLoader> loaders = resources.loaders();
        assertThrows(UnsupportedOperationException.class, () -> loaders.add(new FakeLoader()));
        assertThrows(UnsupportedOperationException.class, () -> loaders.remove(0));
    }

    @Test
    void listGivenByTheProjectIsCopied() {
        List<ResourceLoader> given = new ArrayList<>();
        Resources resources = create(given);
        int size = resources.loaders().size();

        given.add(new FakeLoader());
        assertEquals(size, resources.loaders().size());
    }

    @Test
    void twoLoadersOfTheSameTypeAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> create(List.of(new FakeLoader(), new FakeLoader())));
        // A second loader of a mandatory type either (the registry replaces with replace)
        List<ResourceLoader> twice = new ArrayList<>(mandatory());
        twice.add(new ShaderLoader(new ShaderManager(Resources.DEFAULT_FOLDER)));
        assertThrows(IllegalArgumentException.class, () -> create(twice));
    }

    // A second project type, different from FakeData
    private static class CsvLikeLoader extends FakeLoader {
        @Override
        public Class<? extends DataType> getType() {
            return CsvLikeData.class;
        }

        @Override
        public String getResourceNameType() {
            return "CsvLike";
        }
    }

    private abstract static class CsvLikeData extends DataType {
        CsvLikeData() {
            super(TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT, "csvLike");
        }
    }
}
