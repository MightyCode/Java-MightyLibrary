// RegularPolygonRenderer, HexagonRenderer: shapeData0 = sides, start angle; shapeData1 = bounding box of the unit
// polygon (min x, max x, min y, max y), stretched onto the size of the shape
#include "utils/shape2D.glsl"

void main() {
    FragColor = shapeColor(sdStretchedRegularPolygon(shapeLocal, shapeHalfSize, int(shapeData0.x + 0.5),
            shapeData0.y, shapeData1));
}
