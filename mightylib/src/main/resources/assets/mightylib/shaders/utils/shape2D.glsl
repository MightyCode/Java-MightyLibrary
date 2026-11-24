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

float sdCircle(vec2 p, float radius) {
    return length(p) - radius;
}

// Ellipse of semi-axes radii, approached by f / |grad f| (exact on the axes, close elsewhere)
float sdEllipse(vec2 p, vec2 radii) {
    radii = max(radii, vec2(1e-6));
    float k0 = length(p / radii);
    float k1 = length(p / (radii * radii));
    if (k1 < 1e-9)
        return -min(radii.x, radii.y);
    return k0 * (k0 - 1.0) / k1;
}

// Squared distance from p to the segment [a, b]
float shapeSegmentDistance2(vec2 p, vec2 a, vec2 b) {
    vec2 edge = b - a;
    vec2 toP = p - a;
    vec2 closest = toP - edge * clamp(dot(toP, edge) / max(dot(edge, edge), 1e-12), 0.0, 1.0);
    return dot(closest, closest);
}

// Triangle of vertices a, b, c (either order)
float sdTriangle(vec2 p, vec2 a, vec2 b, vec2 c) {
    float d2 = min(min(shapeSegmentDistance2(p, a, b), shapeSegmentDistance2(p, b, c)), shapeSegmentDistance2(p, c, a));
    // Inside when p is on the same side of the three edges
    float s0 = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
    float s1 = (c.x - b.x) * (p.y - b.y) - (c.y - b.y) * (p.x - b.x);
    float s2 = (a.x - c.x) * (p.y - c.y) - (a.y - c.y) * (p.x - c.x);
    bool inside = (s0 >= 0.0 && s1 >= 0.0 && s2 >= 0.0) || (s0 <= 0.0 && s1 <= 0.0 && s2 <= 0.0);
    return inside ? -sqrt(d2) : sqrt(d2);
}

// Part of a ring between the radii inner and outer, from the angle start over sweep (radians, from +x toward +y,
// so clockwise on the screen); inner 0: a pie, sweep 2 pi: a ring (a disk with inner 0)
float sdAnnularSector(vec2 p, float inner, float outer, float start, float sweep) {
    sweep = clamp(sweep, 0.0, 2.0 * SHAPE_PI);
    float halfSweep = 0.5 * sweep;
    // Turned so that the middle of the sector is along +x, mirrored onto y >= 0
    float middle = start + halfSweep;
    vec2 q = vec2(cos(middle) * p.x + sin(middle) * p.y, -sin(middle) * p.x + cos(middle) * p.y);
    q.y = abs(q.y);

    float len = length(q);
    bool inAngle = atan(q.y, q.x) <= halfSweep;
    float d = 1e30;
    if (inAngle) {
        d = abs(len - outer);
        if (inner > 0.0)
            d = min(d, abs(len - inner));
    }

    // The straight edge at the end of the sector, when it isn't a full ring
    if (halfSweep < SHAPE_PI - 1e-4) {
        vec2 side = vec2(cos(halfSweep), sin(halfSweep));
        d = min(d, length(q - side * clamp(dot(q, side), inner, outer)));
    }

    bool inside = inAngle && len <= outer && len >= inner;
    return inside ? -d : d;
}

// Polygon of sides vertices on the unit circle from the angle start, its bounding box stretched onto the box of
// half size halfSize; unitBox: the bounding box of the unit polygon (min x, max x, min y, max y)
float sdStretchedRegularPolygon(vec2 p, vec2 halfSize, int sides, float start, vec4 unitBox) {
    sides = clamp(sides, 3, 64);
    vec2 scale = 2.0 * halfSize / max(vec2(unitBox.y - unitBox.x, unitBox.w - unitBox.z), vec2(1e-6));
    vec2 offset = -halfSize - vec2(unitBox.x, unitBox.z) * scale;

    float angleStep = 2.0 * SHAPE_PI / float(sides);
    vec2 previous = offset + scale * vec2(cos(start - angleStep), sin(start - angleStep));
    float d2 = 1e30;
    bool inside = false;
    for (int i = 0; i < sides; ++i) {
        float angle = start + angleStep * float(i);
        vec2 current = offset + scale * vec2(cos(angle), sin(angle));
        d2 = min(d2, shapeSegmentDistance2(p, previous, current));
        // Crossing count of a ray toward +x
        if ((current.y > p.y) != (previous.y > p.y)
                && p.x < (previous.x - current.x) * (p.y - current.y) / (previous.y - current.y) + current.x)
            inside = !inside;
        previous = current;
    }

    return inside ? -sqrt(d2) : sqrt(d2);
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
// (inside the edge), alpha multiplied by the coverage; nothing covered is discarded. pixel and fill:
// shapeFragmentPixel() and shapeFillColor(), read by the caller in uniform control flow (derivatives, texture)
vec4 shapeColorWith(float d, float pixel, vec4 fill) {
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

// shapeColorWith at this fragment, for a shape that gives only its distance
vec4 shapeColor(float d) {
    return shapeColorWith(d, shapeFragmentPixel(), shapeFillColor());
}
