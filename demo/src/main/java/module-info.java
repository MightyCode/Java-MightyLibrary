/**
 * The demo as a Java module: an application, it exports nothing. Its resources (assets/demo/) need no "opens", like
 * those of the engine (see the module-info of mightylib).
 */
module io.github.mightycode.mightylibdemo {
    // JOML, org.json, java.desktop and JBox2D come with the engine (requires transitive)
    requires io.github.mightycode.mightylib;
    requires org.lwjgl.glfw;
    requires org.lwjgl.openal;
}
