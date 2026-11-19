// Fragment side of the vector shapes (VectorShape2DRenderer), with utils/shape2DVertex.glsl on the vertex side.
// A shape gives its signed distance (negative inside, world units, in the frame of the shape: shapeLocal) to
// shapeColor, which composes the fill (color, or texture tinted by the color) and the stroke over it, with one
// pixel of anti-aliasing in the alpha. The distance functions follow the usual formulas of signed distance fields
// (Inigo Quilez, "2D distance functions", iquilezles.org), written again here.
// Replaceable by a higher source (game, mod): keep the names of the functions and of the ins.
#include "utils/colorSpace.glsl"

in vec2 shapeLocal;
in vec2 shapeUV;
flat in vec2 shapeHalfSize;
flat in float shapePixel;
flat in float shapeStroke;
flat in float shapeStrokeOuter;
flat in vec4 shapeFill;
flat in vec4 shapeStrokeColor;
flat in float shapeAntialiasing;
flat in float shapeHasTexture;
flat in vec4 shapeData0;
flat in vec4 shapeData1;

uniform sampler2D shapeTexture;

out vec4 FragColor;

const float SHAPE_PI = 3.14159265358979;

// ---- Distances (negative inside) ----

// Box of half size halfSize centered on the origin, corners rounded by radii (top left, top right, bottom right,
// bottom left: y goes down), each clamped to the half size
float sdRoundedBox(vec2 p, vec2 halfSize, vec4 radii) {
    float r = p.x > 0.0 ? (p.y > 0.0 ? radii.z : radii.y) : (p.y > 0.0 ? radii.w : radii.x);
    r = clamp(r, 0.0, min(halfSize.x, halfSize.y));
    vec2 q = abs(p) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

// ---- Composition ----

// Size of a pixel in the frame of the shape, at this fragment (the zoom and the rotations included)
float shapeFragmentPixel() {
    return max(0.5 * (length(dFdx(shapeLocal)) + length(dFdy(shapeLocal))), 1e-6);
}

// Part of the pixel inside the edge at the distance d (negative inside): one pixel of smoothing, or a sharp edge
float shapeCoverage(float d, float pixel) {
    if (shapeAntialiasing < 0.5)
        return d <= 0.0 ? 1.0 : 0.0;

    return 1.0 - smoothstep(-0.5 * pixel, 0.5 * pixel, d);
}

// The fill color at this fragment: the color, multiplied by the texture when the shape has one
vec4 shapeFillColor() {
    // Read in uniform control flow (implicit derivatives), used only with a texture
    vec4 texel = textureIn(texture(shapeTexture, shapeUV));
    return shapeFill * mix(vec4(1.0), texel, shapeHasTexture);
}

// The color of the fragment for the distance d to the edge of the shape: the stroke drawn over the fill
// (inside the edge), alpha multiplied by the coverage; nothing covered is discarded
vec4 shapeColor(float d) {
    float pixel = shapeFragmentPixel();
    vec4 fill = shapeFillColor();

    float fillAlpha = fill.a * shapeCoverage(d, pixel);
    float strokeAlpha = 0.0;
    if (shapeStroke > 0.0) {
        float band = shapeCoverage(d - shapeStrokeOuter, pixel)
                - shapeCoverage(d - shapeStrokeOuter + shapeStroke, pixel);
        strokeAlpha = shapeStrokeColor.a * max(band, 0.0);
    }

    float alpha = strokeAlpha + fillAlpha * (1.0 - strokeAlpha);
    if (alpha <= 1.0 / 1024.0)
        discard;

    vec3 rgb = (shapeStrokeColor.rgb * strokeAlpha + fill.rgb * fillAlpha * (1.0 - strokeAlpha)) / alpha;
    return vec4(rgb, alpha);
}
