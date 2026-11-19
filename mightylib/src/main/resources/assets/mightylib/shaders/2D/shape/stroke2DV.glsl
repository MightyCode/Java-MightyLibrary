// Geometric shapes (PolylineRenderer, ArrowRenderer, PolygonRenderer): triangles made by Stroker and fills, the
// vertices of the strokes extruded here by the half width (world units or screen pixels) and the fringe of one pixel.
// Positions in the frame of the shape (world units, origin at its position), divided by its size before model.
layout (location = 0) in vec2 aBase;
// Extrusion (scaled by half width + fringe, within the limit), fringe extrusion (scaled by the fringe)
layout (location = 2) in vec4 aExtrusion;
// Side across the line, along (coefficients of the half width and of the fringe), limit of the extrusion
layout (location = 3) in vec4 aCoverage;
// Kind (0 fill, 1 fringe of a fill, 2 stroke), color (0 the fill color, 1 the stroke color)
layout (location = 4) in vec2 aPaint;

#include "utils/shape2DVertex.glsl"

out float strokeSide;
out float strokeAlong;
flat out float strokeKind;
flat out float strokeUsesStrokeColor;
flat out float strokeHalfWidth;
flat out float strokeFringe;

void main() {
    float worldPerPixel = shapeWorldPerPixel(projection * view);
    float stroke = shapeStrokeWidth(worldPerPixel);
    float fringe = antialiasing > 0.5 ? worldPerPixel : 0.0;
    // The fringe of a fill is drawn outside its edge only, without width
    float halfWidth = aPaint.x > 1.5 ? 0.5 * stroke : 0.0;

    vec2 local = aBase + aExtrusion.xy * min(halfWidth + fringe, aCoverage.w) + aExtrusion.zw * fringe;
    vec2 safeSize = vec2(shapeSize.x < 0.0 ? -1.0 : 1.0, shapeSize.y < 0.0 ? -1.0 : 1.0)
            * max(abs(shapeSize), vec2(1e-6));
    gl_Position = projection * view * model * vec4(local / safeSize, 0.0, 1.0);

    // shapeParams0: bounding box of the shape in its frame (min x, min y, width, height), for the texture
    vec2 t = (local - shapeParams0.xy) / max(shapeParams0.zw, vec2(1e-6));
    shapeSetOuts(local, t, 0.5 * shapeParams0.zw, worldPerPixel, stroke);

    strokeSide = aCoverage.x;
    strokeAlong = aCoverage.y * halfWidth + aCoverage.z * fringe;
    strokeKind = aPaint.x;
    strokeUsesStrokeColor = aPaint.y;
    strokeHalfWidth = halfWidth;
    strokeFringe = fringe;
}
