package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.algorithms.wavefunctioncollapse.WaveCollapseRulesLoader;
import io.github.mightycode.mightylib.graphics.shader.IShaderService;
import io.github.mightycode.mightylib.graphics.shader.ShaderLoader;
import io.github.mightycode.mightylib.graphics.text.FontLoader;
import io.github.mightycode.mightylib.resources.animation.AnimationDataLoader;
import io.github.mightycode.mightylib.resources.cubemap.CubeMapLoader;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import io.github.mightycode.mightylib.resources.map.TileMapLoader;
import io.github.mightycode.mightylib.resources.map.TileSetLoader;
import io.github.mightycode.mightylib.resources.models.ModelLoader;
import io.github.mightycode.mightylib.resources.sound.SoundLoader;
import io.github.mightycode.mightylib.resources.texture.IconLoader;
import io.github.mightycode.mightylib.resources.texture.TextureDataAtlasLoader;
import io.github.mightycode.mightylib.resources.texture.TextureDataLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The loaders of the library. The engine registers the mandatory ones itself; a game registers the optional ones it
 * uses, one by one or all of them: registry.resources().addAll(LibraryLoaders.all()).
 */
public final class LibraryLoaders {
    private LibraryLoaders() {}

    /**
     * New instances of the optional loaders: tile sets, tile maps, sounds, models, texture atlases, rules of the wave
     * function collapse, cubemaps.
     */
    public static List<ResourceLoader> all() {
        List<ResourceLoader> loaders = new ArrayList<>();
        loaders.add(new TileSetLoader());
        loaders.add(new TileMapLoader());
        loaders.add(new SoundLoader());
        loaders.add(new ModelLoader());
        loaders.add(new TextureDataAtlasLoader());
        loaders.add(new WaveCollapseRulesLoader());
        loaders.add(new CubeMapLoader());
        return loaders;
    }

    /**
     * New instances of the mandatory loaders, registered by the plugin of the engine (a plugin replaces one with
     * registry.resources().replace).
     * @param shaderService read by the loader of the shaders when it works (the services are created after the
     *                      registration of the plugins)
     */
    public static List<ResourceLoader> mandatory(Supplier<IShaderService> shaderService) {
        List<ResourceLoader> loaders = new ArrayList<>();
        loaders.add(new ShaderLoader(shaderService));
        loaders.add(new IconLoader());
        loaders.add(new TextureDataLoader());
        loaders.add(new AnimationDataLoader());
        loaders.add(new FontLoader());
        loaders.add(new JSONLoader());
        loaders.add(new BatchLoader());
        return loaders;
    }

    // The simple name of the library loader of this type of data, null if none
    static String loaderName(Class<? extends DataType> type) {
        for (ResourceLoader loader : all()) {
            if (loader.getType().equals(type))
                return loader.getClass().getSimpleName();
        }
        for (ResourceLoader loader : mandatory(() -> null)) {
            if (loader.getType().equals(type))
                return loader.getClass().getSimpleName();
        }

        return null;
    }
}
