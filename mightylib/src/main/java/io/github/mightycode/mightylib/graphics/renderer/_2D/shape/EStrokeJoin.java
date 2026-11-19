package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

// How two segments of a stroked line meet, on the outer side of the turn
public enum EStrokeJoin {
    // Edges extended until they meet, bevel beyond the miter limit (the default, like SVG)
    MITER,
    // A circular arc around the point
    ROUND,
    // The corner cut by a straight edge
    BEVEL
}
