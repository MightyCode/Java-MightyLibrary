// The bodies of the vector shapes of the engine, shared by the shader of each shape drawn alone (<shape>2DF.glsl)
// and by the common batch program (shape2DBatchF.glsl): the same code, so the same pixels. Each shape gives its
// signed distance (negative inside, frame of the shape) from the ins of utils/shape2D.glsl; the grid composes its
// own color. Nothing here reads derivatives or textures: the caller does it first, in uniform control flow.
// Replaceable by a higher source (game, mod): keep the names of the functions.
#include "utils/shape2D.glsl"

// RoundedRectangleRenderer: shapeData0 = radii of the corners (top left, top right, bottom right, bottom left)
float roundedRectangleDistance() {
    return sdRoundedBox(shapeLocal, shapeHalfSize, shapeData0);
}

// CircleRenderer: the largest circle in the size of the shape
float circleDistance() {
    return sdCircle(shapeLocal, min(shapeHalfSize.x, shapeHalfSize.y));
}

// EllipseRenderer: the ellipse in the size of the shape
float ellipseDistance() {
    return sdEllipse(shapeLocal, shapeHalfSize);
}

// TriangleRenderer: shapeData0 = first and second vertices, shapeData1.xy = third (frame of the shape)
float triangleDistance() {
    return sdTriangle(shapeLocal, shapeData0.xy, shapeData0.zw, shapeData1.xy);
}

// ArcRenderer: shapeData0 = inner radius ratio (of the outer one), start angle, sweep (radians); outer radius: the
// largest circle in the size of the shape
float arcDistance() {
    float outer = min(shapeHalfSize.x, shapeHalfSize.y);
    return sdAnnularSector(shapeLocal, outer * clamp(shapeData0.x, 0.0, 1.0), outer, shapeData0.y, shapeData0.z);
}

// RegularPolygonRenderer, HexagonRenderer: shapeData0 = sides, start angle; shapeData1 = bounding box of the unit
// polygon (min x, max x, min y, max y), stretched onto the size of the shape
float regularPolygonDistance() {
    return sdStretchedRegularPolygon(shapeLocal, shapeHalfSize, int(shapeData0.x + 0.5), shapeData0.y, shapeData1);
}

// GridRenderer: lines every shapeData0.xy (world units) through the point shapeData0.zw of the frame of the shape,
// drawn with the stroke (width, unit, color) over the fill, inside the rectangle of the shape; pixel and fill:
// shapeFragmentPixel() and shapeFillColor(); nothing covered is discarded
vec4 gridColor(float pixel, vec4 fill) {
    vec2 spacing = max(abs(shapeData0.xy), vec2(1e-6));
    // Distance to the nearest line of each direction
    vec2 toLine = abs(mod(shapeLocal - shapeData0.zw + 0.5 * spacing, spacing) - 0.5 * spacing);
    float lineCoverage = shapeCoverage(min(toLine.x, toLine.y) - 0.5 * shapeStroke, pixel);
    float inside = shapeCoverage(sdRoundedBox(shapeLocal, shapeHalfSize, vec4(0.0)), pixel);

    float lineAlpha = shapeStroke > 0.0 ? shapeStrokeColor.a * lineCoverage : 0.0;
    float alpha = (lineAlpha + fill.a * (1.0 - lineAlpha)) * inside;
    if (alpha <= 1.0 / 1024.0)
        discard;

    vec3 rgb = (shapeStrokeColor.rgb * lineAlpha + fill.rgb * fill.a * (1.0 - lineAlpha)) * inside / alpha;
    return vec4(rgb, alpha);
}
