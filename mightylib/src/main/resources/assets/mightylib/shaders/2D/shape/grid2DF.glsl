// GridRenderer: lines every shapeData0.xy (world units) through the point shapeData0.zw of the frame of the shape,
// drawn with the stroke (width, unit, color) over the fill, inside the rectangle of the shape
#include "utils/shape2D.glsl"

void main() {
    float pixel = shapeFragmentPixel();
    vec4 fill = shapeFillColor();

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
    FragColor = vec4(rgb, alpha);
}
