// Vertex side of the vector shapes (VectorShape2DRenderer), with utils/shape2D.glsl on the fragment side.
// The quad of the shape (unit square of Shape2DRenderer, moved by its reference point, then model) is widened by the
// outer part of the stroke and one pixel of anti-aliasing; the fragment shader works in the frame of the shape
// (origin at its center, world units, axes of the shape), independent of the camera.
// Every parameter reaches the fragment shader through an out: a batch will give them per instance instead.
// Replaceable by a higher source (game, mod): keep the names of the functions and of the outs.
#include "utils/colorsManipulation.glsl"

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform vec2 mlViewportSize;

// Size of the shape (its scale) and its reference point (0..1 of the size, the point at its position)
uniform vec2 shapeSize;
uniform vec2 shapeReference;
// Fill color (packed sRGB, Color4f) and stroke
uniform int color;
uniform int strokeColor;
uniform float strokeWidth;
// Part of the stroke outside the edge: 0 inner, 0.5 centered, 1 outer
uniform float strokeAlignment;
// 0: strokeWidth in world units, 1: in pixels of the screen
uniform float strokeUnit;
uniform float antialiasing;
// Extra margin of the quad (world units), for a shape that goes beyond its rectangle
uniform float shapeMargin;
// Part of the texture over the size of the shape: x1, x2, y1, y2 (Shape2DRenderer.setTexturePosition)
uniform vec4 shapeTexturePosition;
uniform int shapeTextured;
// Parameters of the shape itself, read by its fragment shader
uniform vec4 shapeParams0;
uniform vec4 shapeParams1;

// Position in the frame of the shape: world units, origin at its center
out vec2 shapeLocal;
out vec2 shapeUV;
flat out vec2 shapeHalfSize;
// World units per pixel (the largest over the directions)
flat out float shapePixel;
// Stroke width and distance from the edge to its outer side, world units
flat out float shapeStroke;
flat out float shapeStrokeOuter;
flat out vec4 shapeFill;
flat out vec4 shapeStrokeColor;
flat out float shapeAntialiasing;
flat out float shapeHasTexture;
flat out vec4 shapeData0;
flat out vec4 shapeData1;

// The largest distance in world units covered by one pixel: worldToClip maps the world to the clip space,
// the viewport the clip space to pixels; 1 / (smallest singular value) of that 2D linear map
float shapeWorldPerPixel(mat4 worldToClip) {
    if (mlViewportSize.x <= 0.0 || mlViewportSize.y <= 0.0)
        return 1.0;

    vec2 alongX = worldToClip[0].xy * mlViewportSize * 0.5;
    vec2 alongY = worldToClip[1].xy * mlViewportSize * 0.5;
    float a = dot(alongX, alongX);
    float b = dot(alongX, alongY);
    float c = dot(alongY, alongY);
    float halfTrace = 0.5 * (a + c);
    float smallest = halfTrace - sqrt(max(halfTrace * halfTrace - (a * c - b * b), 0.0));

    return inversesqrt(max(smallest, 1e-12));
}

// Stroke width in world units
float shapeStrokeWidth(float worldPerPixel) {
    return strokeWidth * (strokeUnit > 0.5 ? worldPerPixel : 1.0);
}

// Margin of the quad around the rectangle of the shape: outer part of the stroke, one pixel, extra margin
float shapeQuadMargin(float stroke, float worldPerPixel) {
    return max(stroke * strokeAlignment, 0.0) + (antialiasing > 0.5 ? worldPerPixel : 0.0) + max(shapeMargin, 0.0);
}

// Sets gl_Position and the outs for the vertex aPos of the unit quad
void shapeVertex(vec2 aPos) {
    float worldPerPixel = shapeWorldPerPixel(projection * view);
    float stroke = shapeStrokeWidth(worldPerPixel);
    float margin = shapeQuadMargin(stroke, worldPerPixel);

    vec2 center = vec2(0.5) - shapeReference;
    vec2 halfSize = abs(shapeSize) * 0.5;
    // A negative size mirrors the shape: its frame follows
    vec2 mirror = vec2(shapeSize.x < 0.0 ? -1.0 : 1.0, shapeSize.y < 0.0 ? -1.0 : 1.0);
    vec2 local = sign(aPos - center) * (halfSize + margin) * mirror;
    vec2 safeSize = mirror * max(abs(shapeSize), vec2(1e-6));
    vec2 unitPosition = center + local / safeSize;

    gl_Position = projection * view * model * vec4(unitPosition, 0.0, 1.0);

    vec2 t = local / safeSize + 0.5;
    shapeUV = vec2(mix(shapeTexturePosition.x, shapeTexturePosition.y, t.x),
            mix(shapeTexturePosition.z, shapeTexturePosition.w, t.y));

    shapeLocal = local;
    shapeHalfSize = halfSize;
    shapePixel = worldPerPixel;
    shapeStroke = stroke;
    shapeStrokeOuter = stroke * strokeAlignment;
    shapeFill = decodeColor(color);
    shapeStrokeColor = decodeColor(strokeColor);
    shapeAntialiasing = antialiasing;
    shapeHasTexture = float(shapeTextured);
    shapeData0 = shapeParams0;
    shapeData1 = shapeParams1;
}
