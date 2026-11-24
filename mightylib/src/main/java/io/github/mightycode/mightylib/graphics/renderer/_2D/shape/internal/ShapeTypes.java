package io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal;

/**
 * Numbers of the vector shapes of the engine in the common batch program shape2DBatch (attribute aShapeType of
 * BatchFormats.SHAPE): the same values as utils/shapeTypes2D.glsl (ShapeTypesTest checks them). Written by the
 * shapes of the engine (VectorShape2DRenderer.shapeType, package-private: a shape of a project keeps NONE and has its
 * own batch program) and by DebugDraw2D for its circles.
 */
public final class ShapeTypes {
    public static final int NONE = 0;
    public static final int ROUNDED_RECTANGLE = 1;
    public static final int CIRCLE = 2;
    public static final int ELLIPSE = 3;
    public static final int TRIANGLE = 4;
    public static final int ARC = 5;
    public static final int REGULAR_POLYGON = 6;
    public static final int GRID = 7;

    private ShapeTypes() {}
}
