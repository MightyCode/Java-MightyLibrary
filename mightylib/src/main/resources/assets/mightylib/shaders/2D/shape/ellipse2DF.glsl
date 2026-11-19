// EllipseRenderer: the ellipse in the size of the shape
#include "utils/shape2D.glsl"

void main() {
    FragColor = shapeColor(sdEllipse(shapeLocal, shapeHalfSize));
}
