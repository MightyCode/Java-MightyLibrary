// Common batch program of the vector shapes of the engine (shape2DBatch): the kind of each instance (shapeType,
// utils/shapeTypes2D.glsl) chooses the body of utils/shapeKinds2D.glsl, the one of the shader of the shape drawn
// alone. A kind it doesn't know (none, or a shape of a project linked to it by mistake) fills its quad in magenta.
#include "utils/shapeKinds2D.glsl"
#include "utils/shapeTypes2D.glsl"

flat in int shapeType;

void main() {
    // Derivatives and texture read before the switch, in uniform control flow
    float pixel = shapeFragmentPixel();
    vec4 fill = shapeFillColor();

    switch (shapeType) {
        case SHAPE_TYPE_ROUNDED_RECTANGLE:
            FragColor = shapeColorWith(roundedRectangleDistance(), pixel, fill);
            break;
        case SHAPE_TYPE_CIRCLE:
            FragColor = shapeColorWith(circleDistance(), pixel, fill);
            break;
        case SHAPE_TYPE_ELLIPSE:
            FragColor = shapeColorWith(ellipseDistance(), pixel, fill);
            break;
        case SHAPE_TYPE_TRIANGLE:
            FragColor = shapeColorWith(triangleDistance(), pixel, fill);
            break;
        case SHAPE_TYPE_ARC:
            FragColor = shapeColorWith(arcDistance(), pixel, fill);
            break;
        case SHAPE_TYPE_REGULAR_POLYGON:
            FragColor = shapeColorWith(regularPolygonDistance(), pixel, fill);
            break;
        case SHAPE_TYPE_GRID:
            FragColor = gridColor(pixel, fill);
            break;
        default:
            FragColor = vec4(1.0, 0.0, 1.0, 1.0);
            break;
    }
}
