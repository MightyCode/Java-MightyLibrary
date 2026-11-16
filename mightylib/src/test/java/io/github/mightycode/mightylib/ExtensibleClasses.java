package io.github.mightycode.mightylib;

import java.util.List;

/**
 * The classes of mightylib chosen to be inherited by the projects (docs/architecture.md, "API publique"): their
 * hooks are documented, they have no protected field (ExtensionFieldsTest). Every other public concrete class is
 * final. The only list of them, shared by the guard tests.
 */
final class ExtensibleClasses {
    static final List<String> NAMES = List.of(
            // Scenes and views
            "io.github.mightycode.mightylib.scenes.Scene",
            "io.github.mightycode.mightylib.main.WindowView",
            "io.github.mightycode.mightylib.scenes.TemplateSceneLoading",
            // Rendering
            "io.github.mightycode.mightylib.graphics.renderer.Mesh",
            "io.github.mightycode.mightylib.graphics.renderer.Renderer",
            "io.github.mightycode.mightylib.graphics.renderer.MeshRenderer",
            "io.github.mightycode.mightylib.graphics.renderer._2D.shape.Shape2DRenderer",
            // Resources
            "io.github.mightycode.mightylib.resources.DataType",
            "io.github.mightycode.mightylib.resources.SingleSourceDataType",
            "io.github.mightycode.mightylib.resources.MultiSourceDataType",
            "io.github.mightycode.mightylib.resources.ResourceLoader",
            "io.github.mightycode.mightylib.resources.texture.TextureData",
            "io.github.mightycode.mightylib.graphics.GLElement",
            "io.github.mightycode.mightylib.resources.texture.Texture",
            // Commands and GUI
            "io.github.mightycode.mightylib.utils.enginecommand.BaseCommand",
            "io.github.mightycode.mightylib.graphics.GUI.GUI",
            "io.github.mightycode.mightylib.graphics.GUI.Slider",
            "io.github.mightycode.mightylib.graphics.GUI.ValueSelector",
            // Open to the projects, EXPERIMENTAL
            "io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage",
            "io.github.mightycode.mightylib.physics.tweenings.Tweening"
    );

    /*
     * Public concrete classes left open because the library (or its tests) extends them, without being a hook
     * contract for the projects: not in NAMES, their protected members are not documented hooks.
     */
    static final List<String> EXTENDED_BY_THE_LIBRARY = List.of(
            // FallbackShader, FallbackShaderData, FallbackCubeMapData, FallbackModel: the fallback of each type
            "io.github.mightycode.mightylib.graphics.shader.Shader",
            "io.github.mightycode.mightylib.graphics.shader.ShaderData",
            "io.github.mightycode.mightylib.resources.cubemap.CubeMapData",
            "io.github.mightycode.mightylib.resources.models.Model",
            // CommandSyntaxException; an exception stays open like the ones of the JDK
            "io.github.mightycode.mightylib.utils.enginecommand.CommandException",
            // The fake mice of the tests (no-window constructor)
            "io.github.mightycode.mightylib.inputs.MouseManager",
            // FallbackAnimationData, FallbackSoundData, BatchResources
            "io.github.mightycode.mightylib.resources.animation.AnimationData",
            "io.github.mightycode.mightylib.resources.sound.SoundData",
            "io.github.mightycode.mightylib.resources.data.JSONFile",
            // EllipseRenderer, VirtualSceneRenderer
            "io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer",
            // ConsoleView
            "io.github.mightycode.mightylib.scenes.camera.Camera2D",
            // The lights and Material (3D)
            "io.github.mightycode.mightylib.graphics.lighting.materials.BasicMaterial",
            // NodeIntersection, NodeSubIntersection
            "io.github.mightycode.mightylib.algorithms.graph.NodeContainer",
            // Base of Scene, WindowView, Renderer, DataType, Camera, Collision2D (their id)
            "io.github.mightycode.mightylib.utils.math.UUID"
    );

    private ExtensibleClasses() {}
}
