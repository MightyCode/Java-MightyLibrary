package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylibdemo.main.DemoPlugin;
import io.github.mightycode.mightylibdemo.scenes.platformer2d.PlatformerBox2D;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The scenes of the demo, registered by DemoPlugin like the scenes of a real game: what the command scene and the
 * menu offer. A new scene of the demo is added here (a test checks that none is missing).
 * Identifier: the class name relative to this package, in lower case (demo:scene2d, demo:platformer2d.platformerbox2d),
 * so "scene Scene2D" and "scene platformer2d.PlatformerBox2D" work.
 */
public final class DemoScenes {
    static final class Entry<T extends Scene> {
        private final Class<T> type;
        private final Supplier<T> factory;
        private final String description;

        private Entry(Class<T> type, Supplier<T> factory, String description) {
            this.type = type;
            this.factory = factory;
            this.description = description;
        }

        Class<T> type() { return type; }

        String description() { return description; }

        private void register(EngineRegistry registry) {
            registry.scenes().register(IdOf(type), type, factory, description);
        }
    }

    private static <T extends Scene> Entry<T> scene(Class<T> type, Supplier<T> factory, String description) {
        return new Entry<>(type, factory, description);
    }

    static final List<Entry<?>> ALL = List.of(
            scene(MenuScene.class, MenuScene::new, "The menu of the demo: the registered scenes, filtered, sorted and described."),
            scene(BatchRenderers2D.class, BatchRenderers2D::new, "The renderers that go through the 2D batch: wrapped texts, a tile map larger than the screen under a moving camera (only the visible tiles are written), animations and the debug renderers (nodes, graph, matrix). M changes the mode of the batch to compare the pictures; the camera follows a path until Space stops it or you move it (Z Q S D, arrows, right drag, wheel)."),
            scene(BatchStress2D.class, BatchStress2D::new, "Measure of the 2D drawing: N textured and colored rectangles, turning, or after F the seven vector shapes of the engine mixed, or after F again a mix like in a game (animated slimes and tiles of one atlas, shapes with strokes, texts). Page Up doubles N, Page Down halves it, M changes the mode of the 2D batch, O sorts the draws by state; the render time and the draw calls are shown."),
            scene(ViewCulling2D.class, ViewCulling2D::new, "The view culling of the 2D draws: a world twenty screens large filled with every batched renderer, a camera to move (Z Q S D, arrows, right drag), zoom (wheel, + -) and turn (Q E). C turns the culling off and on, I reduces its test zone by 200 pixels, B draws the box of each draw, F freezes the boxes of the views; the counters show the draws tested, culled and without box."),
            scene(Box2D.class, Box2D::new, "Physics with JBox2D: bodies and shapes in a world, with a camera that moves, zooms and drags."),
            scene(CollisionGridScene2D.class, CollisionGridScene2D::new, "Collision2DGrid: obstacles sorted into the cells of a grid, only the ones near the rectangle that follows the mouse are tested. Grey: ignored, blue: tested, red: colliding."),
            scene(CommandsScene2D.class, CommandsScene2D::new, "A scene with its own command, spawn, that creates shapes. Open the console with the key left of 1 and try: spawn 3 --color red"),
            scene(ConsoleFeedScene.class, ConsoleFeedScene::new, "Writes a line of every type in the console at a regular pace, to try scrolling, filters and selection while the log moves."),
            scene(SharedMaterial2D.class, SharedMaterial2D::new, "Materials shared by renderers: a grid of sprites on one material (T changes its texture, B its blend mode), a color of their own for rectangles sharing one material, two renderers on one mesh, the five blend modes, and the stats counters."),
            scene(CustomRenderer2D.class, CustomRenderer2D::new, "Custom geometry with a custom shader: stars drawn by a MeshRenderer subclass and by a Mesh of the scene. D toggles the depth test."),
            scene(ColorSpaceSplit2D.class, ColorSpaceSplit2D::new, "The same 2D picture in linear (left half) and gamma (right half): a control ramp, alpha blends, a filtered texture and anti-aliased text. F7 has no effect here."),
            scene(ColorSpaceSplit3D.class, ColorSpaceSplit3D::new, "The same lit 3D scene in linear (left half) and gamma (right half) with the same camera: the lighting is softer on the left. WASD and the mouse move the camera."),
            scene(DebugDrawing2D.class, DebugDrawing2D::new, "The debug drawings of a view, each lifetime: a cross and an arrow redrawn at each frame, a trail kept 3 seconds, a line while Space is held, a circle for 120 frames (C), a rectangle until removed (R), texts on the screen. Up and Down zoom; the command debugdraw switches them off and counts them."),
            scene(DisplayModesScene.class, DisplayModesScene::new, "Windowed, exclusive fullscreen and borderless modes, a second window, the border and the resizing."),
            scene(DocumentationScene.class, DocumentationScene::new, "The reference scene: read its source code, each step of the life of a scene and of its resources is explained."),
            scene(EngineServices2D.class, EngineServices2D::new, "The services of the engine and their plugins (command services), the sound in use (OpenAL or silent) and the overlay of the tick_counter service of the demo, under the console."),
            scene(FallbackScene.class, FallbackScene::new, "Resources that can't be used (unknown texture, missing file, unknown font or sound) are replaced by fallbacks instead of crashing the game."),
            scene(Game3D2D.class, Game3D2D::new, "A 2D animated slime drawn in a 3D scene, next to a textured cube, with a free camera."),
            scene(GamepadScene.class, GamepadScene::new, "One square per player, moved by the sticks of up to four gamepads, with the list of the buttons and axes."),
            scene(GLLoading.class, GLLoading::new, "Loads several big generated textures, sent to the GPU by strips with the time left in each frame, so the game keeps running."),
            scene(GUIWidgetsScene.class, GUIWidgetsScene::new, "Sliders, a selector, a shaking effect and a positional sound that follows a moving square."),
            scene(KeyboardLayoutScene.class, KeyboardLayoutScene::new, "The keys held with their position and their label in the layout of the system (Z on the key W of an AZERTY keyboard), and the text typed."),
            scene(LayeredTexture2D.class, LayeredTexture2D::new, "A data read from several files with its own fallback: a character made of stacked layers, an unknown layered picture (blue stripes, not the checkerboard) and an unknown texture."),
            scene(MultiWindowScene.class, MultiWindowScene::new, "Several windows: a dialog that blocks the main one, a tool window and a second context with its own scene."),
            scene(PathfindingScene2D.class, PathfindingScene2D::new, "Graph, Dijkstra and A*: paint walls with the mouse, move the start and the end, and see the shortest path."),
            scene(PlatformerBox2D.class, PlatformerBox2D::new, "A platformer: a player controlled on JBox2D physics."),
            scene(PreloadScene.class, PreloadScene::new, "Loads the next scene in advance while this one runs, so Enter switches with a short loading screen."),
            scene(Reference3D.class, Reference3D::new, "Reference scene of the image tests, do not change it without generating its references again: lit cubes, a Phong material and a texture, still. WASD moves the camera, TAB frees the look."),
            scene(ReferenceColorSpace2D.class, ReferenceColorSpace2D::new, "Reference scene of the image tests, do not change it without generating its references again: the same picture in linear (left) and gamma (right), a ramp of greys, alpha blends, a filtered texture and a text, still."),
            scene(ReferenceShapesText2D.class, ReferenceShapesText2D::new, "Reference scene of the image tests, do not change it without generating its references again: texts of the fonts of the engine, the seven vector shapes, strokes, joins, caps and points, still."),
            scene(ReferenceSprites2D.class, ReferenceSprites2D::new, "Reference scene of the image tests, do not change it without generating its references again: textures, animations on a fixed frame and a tile map, still."),
            scene(ResourceSourcesScene2D.class,ResourceSourcesScene2D::new, "The sources of the resources: a texture of the game folder replaced by the jar of the demo (same path), a texture added by the demo through its own textures.json, and the sources that have each file."),
            scene(RopeSimulation2D.class, RopeSimulation2D::new, "A rope simulation driven by the mouse."),
            scene(Scene2D.class, Scene2D::new, "The main 2D test scene: tile map, animations and texts."),
            scene(Scene3D.class, Scene3D::new, "The main 3D test scene: cubes and models lit by several lights, with a free camera."),
            scene(Scene3DBis.class, Scene3DBis::new, "A second 3D test scene with cubes and lights."),
            scene(ShaderDefines2D.class, ShaderDefines2D::new, "The defines of the shaders: two entries of shaders.json on the same files, one tinted by a define of its entry. D sets or removes a global define that inverts the colors (every shader is read again)."),
            scene(SoundFormatsScene.class, SoundFormatsScene::new, "The same sound in each supported format (ogg, wav, mp3, flac), played when the mouse enters its line."),
            scene(TerrainGeneration3D.class, TerrainGeneration3D::new, "A generated terrain seen with a free camera."),
            scene(TextFieldScene2D.class, TextFieldScene2D::new, "The TextField and the focus of the window: while the field has the focus, the scene sees no key."),
            scene(TextPaddingScene.class, TextPaddingScene::new, "The padding of a Text, and a long text wrapped at the spaces with setMaxWidth."),
            scene(TileMapReferences2D.class, TileMapReferences2D::new, "The references of a tile map: the same map drawn nine times, one reference each (LeftUp, Up, None, RightDown...); the cross marks the position of each map, at its corner, the middle of its side or its center."),
            scene(VectorShapes2D.class, VectorShapes2D::new, "The vector shapes: smooth edges at every zoom and rotation, strokes inside, centered or outside, in world units or screen pixels, filled with a color or a texture. Wheel, Ctrl and Shift zoom, Alt + wheel turns the camera, Z Q S D or the right button move it, R resets it, X the unit of the strokes, W the anti-aliasing; the mouse over a shape names its renderer."),
            scene(WaveFunctionCollapse2D.class, WaveFunctionCollapse2D::new, "A map generated by the Wave Function Collapse algorithm from the rules of a tile set."));

    private DemoScenes() {}

    // Called by DemoPlugin.register
    public static void Register(EngineRegistry registry) {
        for (Entry<?> entry : ALL)
            entry.register(registry);
    }

    /**
     * The class name relative to the package of the scenes (platformer2d.PlatformerBox2D), the full name for a class
     * outside of it: the name shown by the menu and the key of the dates of SceneHistory.
     */
    public static String RelativeName(Class<?> type) {
        String prefix = DemoScenes.class.getPackageName() + ".";
        return type.getName().startsWith(prefix) ? type.getName().substring(prefix.length()) : type.getName();
    }

    // demo:<relative name in lower case>
    static Identifier IdOf(Class<?> type) {
        return Identifier.of(DemoPlugin.NAMESPACE, RelativeName(type).toLowerCase(Locale.ROOT));
    }
}
