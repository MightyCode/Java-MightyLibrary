// The values of a shape read from the attributes of its instance (aModel0, aModel1, aColor, aStrokeColor, aStroke,
// aMarginTextured, aTexturePosition, aParams0, aParams1, declared by the batch program before this include), into
// the globals of utils/shape2DVertex.glsl (SHAPE_BATCH defined)
#include "utils/shape2DVertex.glsl"

void shapeReadInstance() {
    model = mat4(aModel0.x, aModel1.x, 0.0, 0.0,
                 aModel0.y, aModel1.y, 0.0, 0.0,
                 0.0, 0.0, 1.0, 0.0,
                 aModel0.z, aModel1.z, 0.0, 1.0);
    color = aColor;
    strokeColor = aStrokeColor;
    strokeWidth = aStroke.x;
    strokeAlignment = aStroke.y;
    strokeUnit = aStroke.z;
    antialiasing = aStroke.w;
    shapeMargin = aMarginTextured.x;
    shapeTextured = int(aMarginTextured.y);
    shapeTexturePosition = aTexturePosition;
    shapeParams0 = aParams0;
    shapeParams1 = aParams1;
}
