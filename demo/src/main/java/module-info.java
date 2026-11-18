/**
 * The demo as a Java module: an application, it exports nothing. Its resources (assets/demo/) need no "opens", like
 * those of the engine (see the module-info of mightylib).
 */
module io.github.mightycode.mightylibdemo {
    // JOML, org.json and java.desktop come with the engine (requires transitive)
    requires io.github.mightycode.mightylib;
    requires org.lwjgl.glfw;
    requires org.lwjgl.openal;
    // Automatic module, named after its jar
    requires jbox2d.library;
}
