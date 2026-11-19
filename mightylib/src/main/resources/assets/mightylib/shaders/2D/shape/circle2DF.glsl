// CircleRenderer: the largest circle in the size of the shape
#include "utils/shape2D.glsl"

void main() {
    FragColor = shapeColor(sdCircle(shapeLocal, min(shapeHalfSize.x, shapeHalfSize.y)));
}
