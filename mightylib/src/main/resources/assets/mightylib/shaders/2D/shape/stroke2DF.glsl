// Geometric shapes (stroke2DV.glsl): the coverage of each kind of triangle, in the fill or the stroke color
#include "utils/shape2D.glsl"

in float strokeSide;
in float strokeAlong;
flat in float strokeKind;
flat in float strokeUsesStrokeColor;
flat in float strokeHalfWidth;
flat in float strokeFringe;

void main() {
    // Read in uniform control flow (the texture)
    vec4 fill = shapeFillColor();
    vec4 color = strokeUsesStrokeColor > 0.5 ? shapeStrokeColor : fill;

    float coverage = 1.0;
    if (strokeKind > 1.5) {
        // Stroke: across the line, and beyond the end of a cap
        float across = abs(strokeSide) * (strokeHalfWidth + strokeFringe) - strokeHalfWidth;
        coverage = shapeCoverage(across, shapePixel) * shapeCoverage(strokeAlong, shapePixel);
    } else if (strokeKind > 0.5) {
        // Fringe of a fill: from its edge (side 0) to one fringe outside (side 1), centered half a fringe out
        coverage = shapeCoverage((abs(strokeSide) - 0.5) * strokeFringe, shapePixel);
    }

    float alpha = color.a * coverage;
    if (alpha <= 1.0 / 1024.0)
        discard;

    FragColor = vec4(color.rgb, alpha);
}
