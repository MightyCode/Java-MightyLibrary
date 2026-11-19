package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

// Where the stroke of a vector shape lies around its edge
public enum EStrokeAlignment {
    // Inside the edge: the shape keeps its size
    INNER(0f),
    // Half inside, half outside (the default, like Processing, SVG and the HTML canvas)
    CENTERED(0.5f),
    // Outside the edge: the fill keeps its size
    OUTER(1f);

    private final float outsideFraction;

    EStrokeAlignment(float outsideFraction) {
        this.outsideFraction = outsideFraction;
    }

    // Part of the width outside the edge (0, 0.5 or 1): sent to the shaders
    public float outsideFraction() {
        return outsideFraction;
    }
}
