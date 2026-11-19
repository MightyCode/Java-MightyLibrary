package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector4f;

/**
 * A part of a ring, with smooth edges and a stroke: outer radius = the largest circle in the size of the shape,
 * inner radius = a ratio of it (0: a pie, or a disk), from a start angle over a sweep (radians, from +x toward +y:
 * clockwise on the screen, like setRotationZ; 2 pi: a full ring). A full disk by default. Reference point at the
 * center by default.
 */
public final class ArcRenderer extends VectorShape2DRenderer {
    private float innerRatio;
    private float startAngle;
    private float sweep;

    public ArcRenderer() {
        this(EngineResources.ARC_2D);
    }

    // A shader of the project, with the parameters of arc2DF.glsl
    public ArcRenderer(String shaderName) {
        super(shaderName);
        sweep = (float) (2 * Math.PI);
        setReferenceDirection(EDirection.None);
    }

    // 0 (a pie) to 1 (nothing left)
    public ArcRenderer setInnerRatio(float innerRatio) {
        this.innerRatio = Math.max(0f, Math.min(innerRatio, 1f));
        return this;
    }

    public float getInnerRatio() {
        return innerRatio;
    }

    // Radians; the sweep is kept in 0..2 pi
    public ArcRenderer setAngles(float startAngle, float sweep) {
        this.startAngle = startAngle;
        this.sweep = (float) Math.max(0, Math.min(sweep, 2 * Math.PI));
        return this;
    }

    public float getStartAngle() {
        return startAngle;
    }

    public float getSweep() {
        return sweep;
    }

    @Override
    protected void writeShapeParameters(Vector4f first, Vector4f second) {
        first.set(innerRatio, startAngle, sweep, 0f);
    }
}
