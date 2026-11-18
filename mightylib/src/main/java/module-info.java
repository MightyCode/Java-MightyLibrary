/**
 * The engine as a Java module. Every package is exported except the ".internal" ones (API status INTERNAL, checked by
 * ApiStatusTest and ModuleDescriptorTest). Some public members of exported types still expose an internal type
 * (Engine.consoleLog, Window.getGLContext...): accepted, see docs/decisions.md.
 *
 * <p>The resources of the engine (assets/mightylib/) need no "opens": ClasspathSource looks up the folder
 * "assets/mightylib" (package "assets", not a package of any module, so not encapsulated) then reads it as a path
 * (folder or jar file system), never through the module.
 */
module io.github.mightycode.mightylib {
    // In the public API (signatures): readable by the modules that read the engine
    requires transitive java.desktop;
    requires transitive org.joml;
    requires transitive org.json;
    // Annotations only, optional at run time; not transitive, so that a game compiles without apiguardian
    requires static org.apiguardian.api;

    requires jdk.jfr;
    // Provider of the jar file systems (ClasspathSource), only looked up at run time: named so that an image built
    // by jlink contains it
    requires jdk.zipfs;
    requires org.lwjgl;
    requires org.lwjgl.assimp;
    requires org.lwjgl.glfw;
    requires org.lwjgl.openal;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
    // Automatic modules, named after their jar (no module name of their own): see docs/later.md
    requires jbox2d.library;
    requires jlayer;
    requires opensimplex;


    exports io.github.mightycode.mightylib.algorithms;
    exports io.github.mightycode.mightylib.algorithms.astar;
    exports io.github.mightycode.mightylib.algorithms.dijkstra;
    exports io.github.mightycode.mightylib.algorithms.graph;
    exports io.github.mightycode.mightylib.algorithms.wavefunctioncollapse;
    exports io.github.mightycode.mightylib.console;
    exports io.github.mightycode.mightylib.graphics;
    exports io.github.mightycode.mightylib.graphics.debug;
    exports io.github.mightycode.mightylib.graphics.game;
    exports io.github.mightycode.mightylib.graphics.gui;
    exports io.github.mightycode.mightylib.graphics.lighting;
    exports io.github.mightycode.mightylib.graphics.lighting.lights;
    exports io.github.mightycode.mightylib.graphics.lighting.materials;
    exports io.github.mightycode.mightylib.graphics.renderer;
    exports io.github.mightycode.mightylib.graphics.renderer._2D;
    exports io.github.mightycode.mightylib.graphics.renderer._2D.shape;
    exports io.github.mightycode.mightylib.graphics.renderer._3D;
    exports io.github.mightycode.mightylib.graphics.renderer._3D.heightMap;
    exports io.github.mightycode.mightylib.graphics.renderer._3D.shape;
    exports io.github.mightycode.mightylib.graphics.renderer._3D.terrain;
    exports io.github.mightycode.mightylib.graphics.renderer.utils;
    exports io.github.mightycode.mightylib.graphics.shader;
    exports io.github.mightycode.mightylib.graphics.surface;
    exports io.github.mightycode.mightylib.graphics.text;
    exports io.github.mightycode.mightylib.graphics.utils;
    exports io.github.mightycode.mightylib.inputs;
    exports io.github.mightycode.mightylib.inputs.inputType;
    exports io.github.mightycode.mightylib.inputs.keyboardlanguage;
    exports io.github.mightycode.mightylib.inputs.text;
    exports io.github.mightycode.mightylib.main;
    exports io.github.mightycode.mightylib.main.monitors;
    exports io.github.mightycode.mightylib.main.utils;
    exports io.github.mightycode.mightylib.network;
    exports io.github.mightycode.mightylib.physics;
    exports io.github.mightycode.mightylib.physics.collision;
    exports io.github.mightycode.mightylib.physics.tweenings;
    exports io.github.mightycode.mightylib.physics.tweenings.type;
    exports io.github.mightycode.mightylib.registry;
    exports io.github.mightycode.mightylib.resources;
    exports io.github.mightycode.mightylib.resources.animation;
    exports io.github.mightycode.mightylib.resources.cubemap;
    exports io.github.mightycode.mightylib.resources.data;
    exports io.github.mightycode.mightylib.resources.map;
    exports io.github.mightycode.mightylib.resources.models;
    exports io.github.mightycode.mightylib.resources.sound;
    exports io.github.mightycode.mightylib.resources.sound.decoder;
    exports io.github.mightycode.mightylib.resources.source;
    exports io.github.mightycode.mightylib.resources.texture;
    exports io.github.mightycode.mightylib.scenes;
    exports io.github.mightycode.mightylib.scenes.camera;
    exports io.github.mightycode.mightylib.scenes.camera.cameraComponents;
    exports io.github.mightycode.mightylib.services;
    exports io.github.mightycode.mightylib.sounds;
    exports io.github.mightycode.mightylib.utils;
    exports io.github.mightycode.mightylib.utils.enginecommand;
    exports io.github.mightycode.mightylib.utils.enginecommand.syntax;
    exports io.github.mightycode.mightylib.utils.json;
    exports io.github.mightycode.mightylib.utils.math;
    exports io.github.mightycode.mightylib.utils.math.color;
    exports io.github.mightycode.mightylib.utils.math.geometry;
    exports io.github.mightycode.mightylib.utils.valueDebug;
}
