package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

// The ends of an open stroked line
public enum EStrokeCap {
    // Cut at the end point (the default, like SVG)
    BUTT,
    // A half disk around the end point
    ROUND,
    // Extended by half the width beyond the end point
    SQUARE
}
