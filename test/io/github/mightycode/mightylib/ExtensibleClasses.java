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
            "io.github.mightycode.mightylib.resources.texture.Texture"
    );

    private ExtensibleClasses() {}
}
