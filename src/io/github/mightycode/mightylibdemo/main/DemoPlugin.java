package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.algorithms.wavefunctioncollapse.WaveCollapseRulesLoader;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.resources.map.TileMapLoader;
import io.github.mightycode.mightylib.resources.map.TileSetLoader;
import io.github.mightycode.mightylib.resources.models.ModelLoader;
import io.github.mightycode.mightylib.resources.sound.SoundLoader;
import io.github.mightycode.mightylib.resources.texture.TextureDataAtlasLoader;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylibdemo.scenes.MenuScene;

/**
 * What the demo adds to the engine, like a real game: the loaders it uses, its commands.
 */
public class DemoPlugin implements IPlugin {
    public static final String NAMESPACE = "demo";

    @Override
    public String namespace() {
        return NAMESPACE;
    }

    @Override
    public void register(EngineRegistry registry) {
        // The optional loaders of the library the demo uses: all of them (LibraryLoaders.all() does the same)
        registry.resources()
                .add(new TileSetLoader())
                .add(new TileMapLoader())
                .add(new SoundLoader())
                .add(new ModelLoader())
                .add(new TextureDataAtlasLoader())
                .add(new WaveCollapseRulesLoader());

        // Commands of the game: kept for the whole run, whatever the scene
        registry.commands().register(new BaseCommand(registry.id("menu"), ECommandLevel.USER, "Go back to the menu") {
            @Override
            protected void syntax(CommandNode root) {
                root.executes(context -> {
                    Views.engine().sceneManager().requestSceneChange(new MenuScene(), new String[]{});
                    return CommandResult.ok("Back to the menu");
                });
            }
        });
    }
}
