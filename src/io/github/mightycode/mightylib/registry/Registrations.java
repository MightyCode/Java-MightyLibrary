package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.inputs.InputRegistry;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.scenes.SceneRegistry;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything the plugins registered (EngineRegistry.Collect), frozen once the last plugin is done: the engine reads it
 * to create its systems. Pure data: no GL, no engine.
 */
public final class Registrations {
    private final List<String> plugins = new ArrayList<>();
    final Entries<Class<? extends DataType>, ResourceLoader> loaders =
            new Entries<>("Loader of", Class::getSimpleName);
    final Entries<Identifier, BaseCommand> commands = new Entries<>("Command", Identifier::toString);
    final Entries<Identifier, InputsSection.Input> inputs = new Entries<>("Input", Identifier::toString);
    final Entries<Identifier, ScenesSection.SceneEntry> scenes = new Entries<>("Scene", Identifier::toString);

    private boolean frozen;
    // Built at the freeze
    private InputRegistry inputRegistry;
    private SceneRegistry sceneRegistry;

    Registrations() {}

    void addPlugin(String namespace) {
        plugins.add(namespace);
    }

    void freeze() {
        inputRegistry = new InputRegistry();
        for (InputsSection.Input input : inputs.values()) {
            if (input.action != null)
                inputRegistry.declareAction(input.action, input.actionBinding);
            else
                inputRegistry.declareAxis(input.axis, input.axisBinding);
        }
        inputRegistry.freeze();

        sceneRegistry = new SceneRegistry();
        for (ScenesSection.SceneEntry scene : scenes.values())
            sceneRegistry.declare(scene.identifier, scene.type, scene.factory, scene.description);
        sceneRegistry.freeze();

        frozen = true;
    }

    void checkOpen() {
        if (frozen)
            throw new IllegalStateException("The registry is frozen: register from IPlugin.register only");
    }

    public boolean isFrozen() {
        return frozen;
    }

    // Namespaces of the plugins, in registration order: the engine first
    public List<String> plugins() {
        return Collections.unmodifiableList(plugins);
    }

    // The loaders of every plugin, in registration order
    public List<ResourceLoader> loaders() {
        return Collections.unmodifiableList(loaders.values());
    }

    // The actions and axes of every plugin with their default bindings, frozen; null before the freeze
    public InputRegistry inputs() {
        return inputRegistry;
    }

    // The scenes of every plugin by name, frozen; null before the freeze
    public SceneRegistry scenes() {
        return sceneRegistry;
    }

    // The commands the plugin registered or replaced: it owns them
    public List<BaseCommand> commands(String plugin) {
        return Collections.unmodifiableList(commands.valuesOf(plugin));
    }
}
