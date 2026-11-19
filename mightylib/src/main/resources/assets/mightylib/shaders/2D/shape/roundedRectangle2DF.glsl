// RoundedRectangleRenderer: shapeData0 = radii of the corners (top left, top right, bottom right, bottom left)
#include "utils/shape2D.glsl"

void main() {
    FragColor = shapeColor(sdRoundedBox(shapeLocal, shapeHalfSize, shapeData0));
}
