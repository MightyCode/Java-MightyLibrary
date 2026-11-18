package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.graphics.shader.ShaderLoader;
import io.github.mightycode.mightylib.graphics.shader.IShaderService;
import io.github.mightycode.mightylib.resources.sound.SoundData;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// The loaders are given at the construction of Resources (those of the plugins, the engine first), then fixed
class ResourceLoadersTest {
    private static Resources create(List<ResourceLoader> loaders) {
        return new Resources(new Resources.AllResourcesMethod(), 1,
                ResourceSources.of(new FolderSource(Resources.DEFAULT_FOLDER)), loaders);
    }

    private static List<ResourceLoader> mandatory() {
        IShaderService shaders = new NoShaders();
        return LibraryLoaders.mandatory(() -> shaders);
    }

    // A shader service without shaders
    private static final class NoShaders implements IShaderService {
        @Override
        public JSONObject declarations() {
            return new JSONObject();
        }

        @Override
        public String folder() {
            return "shaders/";
        }

        @Override
        public String prelude(Map<String, String> entryDefines) {
            return "#version 330 core\n";
        }

        @Override
        public void setDefine(String name, String value) {}

        @Override
        public void removeDefine(String name) {}

        @Override
        public Map<String, String> defines() {
            return Map.of();
        }
    }

    @Test
    void registeredLoadersKeepTheirOrder() {
        FakeLoader first = new FakeLoader();
        CsvLikeLoader second = new CsvLikeLoader();
        List<ResourceLoader> loaders = create(List.of(first, second)).loaders();

        assertEquals(List.of(first, second), loaders);
    }

    // The loader of the shaders is registered before the shader service exists: it reads it when it works
    @Test
    void shaderLoaderReadsItsServiceLate() {
        IShaderService[] service = new IShaderService[1];
        ShaderLoader loader = new ShaderLoader(() -> service[0]);
        assertThrows(IllegalStateException.class, () -> loader.create(new HashMap<>()));
        // The fallback does not need it: it can't fail
        assertNotNull(loader.createFallback());

        service[0] = new NoShaders();
        Map<String, DataType> data = new HashMap<>();
        loader.create(data);
        assertTrue(data.isEmpty());
        assertInstanceOf(ShaderLoader.class, LibraryLoaders.mandatory(() -> service[0]).get(0));
    }

    // An invalid define of an entry (rule of the service) makes this shader fail alone, before its files are read
    @Test
    void invalidEntryDefineFailsTheShader() {
        ShaderLoader loader = new ShaderLoader(new NoShaders());
        for (Map<String, String> defines : List.of(Map.of("1A", ""), Map.of("GL_MINE", "1"), Map.of("A", "1\n2"),
                Collections.singletonMap("A", (String) null))) {
            ShaderData shader = new ShaderData("bad", true, defines, "shaders/v.glsl", "shaders/f.glsl");
            ResourceException error = assertThrows(ResourceException.class, () -> loader.initWithFile(shader));
            assertTrue(error.getMessage().contains("bad"), error.getMessage());
        }
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
        twice.add(new ShaderLoader(new NoShaders()));
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
