package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal.ShapeTypes;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;

/**
 * An ellipse filling the size of the shape, with smooth edges and a stroke (its width is approached on a very
 * flat ellipse). Reference point at the center by default.
 */
public final class EllipseRenderer extends VectorShape2DRenderer {
    public EllipseRenderer() {
        this(EngineResources.ELLIPSE_2D);
    }

    // A shader of the project, with the parameters of ellipse2DF.glsl
    public EllipseRenderer(String shaderName) {
        super(shaderName);
        setReferenceDirection(EDirection.None);
    }

    @Override
    int shapeType() {
        return ShapeTypes.ELLIPSE;
    }
}
