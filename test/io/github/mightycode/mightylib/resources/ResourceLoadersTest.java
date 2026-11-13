package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.graphics.shader.ShaderLoader;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The loaders are given at the construction of Resources, then fixed
class ResourceLoadersTest {
    private static Resources create(List<ResourceLoader> projectLoaders) {
        return new Resources(new Resources.AllResourcesMethod(), 1, Resources.DEFAULT_FOLDER,
                new ShaderManager(Resources.DEFAULT_FOLDER), projectLoaders);
    }

    @Test
    void mandatoryLoadersComeFirstThenThoseOfTheProject() {
        FakeLoader first = new FakeLoader();
        CsvLikeLoader second = new CsvLikeLoader();
        List<ResourceLoader> loaders = create(List.of(first, second)).loaders();

        assertInstanceOf(ShaderLoader.class, loaders.get(0));
        assertSame(first, loaders.get(loaders.size() - 2));
        assertSame(second, loaders.get(loaders.size() - 1));
        assertEquals(1, loaders.stream().filter(loader -> loader instanceof BatchLoader).count());
    }

    // The shader sources are a data like the others (ShaderData), their program a GL element
    @Test
    void shaderLoaderServesShaderData() {
        Resources resources = create(List.of());
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
        // A project loader can't replace a mandatory one either
        assertThrows(IllegalArgumentException.class, () -> create(List.of(new JSONLoader())));
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
