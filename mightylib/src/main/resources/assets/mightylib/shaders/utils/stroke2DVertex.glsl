// Vertex side of the geometric shapes (stroke2DV.glsl, and their batch program stroke2DBatchV.glsl): the vertices of
// the strokes extruded by the half width (world units or screen pixels) and the fringe of one pixel. Positions in the
// frame of the shape (world units, origin at its position), divided by its size before model.
#include "utils/shape2DVertex.glsl"

out float strokeSide;
out float strokeAlong;
flat out float strokeKind;
flat out float strokeUsesStrokeColor;
flat out float strokeHalfWidth;
flat out float strokeFringe;

// base: position; extrusion: scaled by half width + fringe (within the limit), then by the fringe; coverage: side
// across the line, along (coefficients of the half width and of the fringe), limit of the extrusion; paint: kind
// (0 fill, 1 fringe of a fill, 2 stroke), color (0 the fill color, 1 the stroke color)
void strokeVertex(vec2 base, vec4 extrusion, vec4 coverage, vec2 paint) {
    float worldPerPixel = shapeWorldPerPixel(projection * view);
    float stroke = shapeStrokeWidth(worldPerPixel);
    float fringe = antialiasing > 0.5 ? worldPerPixel : 0.0;
    // The fringe of a fill is drawn outside its edge only, without width
    float halfWidth = paint.x > 1.5 ? 0.5 * stroke : 0.0;

    vec2 local = base + extrusion.xy * min(halfWidth + fringe, coverage.w) + extrusion.zw * fringe;
    vec2 safeSize = vec2(shapeSize.x < 0.0 ? -1.0 : 1.0, shapeSize.y < 0.0 ? -1.0 : 1.0)
            * max(abs(shapeSize), vec2(1e-6));
    gl_Position = projection * view * model * vec4(local / safeSize, 0.0, 1.0);

    // shapeParams0: bounding box of the shape in its frame (min x, min y, width, height), for the texture
    vec2 t = (local - shapeParams0.xy) / max(shapeParams0.zw, vec2(1e-6));
    shapeSetOuts(local, t, 0.5 * shapeParams0.zw, worldPerPixel, stroke);

    strokeSide = coverage.x;
    strokeAlong = coverage.y * halfWidth + coverage.z * fringe;
    strokeKind = paint.x;
    strokeUsesStrokeColor = paint.y;
    strokeHalfWidth = halfWidth;
    strokeFringe = fringe;
}
