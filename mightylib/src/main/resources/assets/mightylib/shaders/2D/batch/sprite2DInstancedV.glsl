// Batch of sprite quads by instances: the vertices are the corners of the unit quad, each instance gives the affine
// transform of its quad (model of the draw, size and reference point included), the affine map of its texture
// coordinates (flips and rotations of the texture included), the region of the texture it reads and its color
out vec2 texCoords;
flat out vec4 texRegion;
out vec4 vertexColor;

// Corner of the unit quad, (0, 0) to (1, 1)
layout(location = 0) in vec2 aCorner;
// Rows of the 2x3 affine transform: x = dot(row0, (corner, 1)), y = dot(row1, (corner, 1))
layout(location = 1) in vec3 aRow0;
layout(location = 2) in vec3 aRow1;
// Texture coordinates at the corner (0, 0), then their change along each axis of the quad
layout(location = 3) in vec2 aTexOrigin;
layout(location = 4) in vec4 aTexAxes;
// Region of the texture read (utils/textureRegion.glsl)
layout(location = 5) in vec4 aTexRegion;
// RGBA8 normalized, an sRGB color
layout(location = 6) in vec4 aColor;

uniform mat4 projection;
uniform mat4 view;

void main()
{
    vec3 corner = vec3(aCorner, 1.0f);
    gl_Position = projection * view * vec4(dot(aRow0, corner), dot(aRow1, corner), 0.0f, 1.0f);
    texCoords = aTexOrigin + aCorner.x * aTexAxes.xy + aCorner.y * aTexAxes.zw;
    texRegion = aTexRegion;
    vertexColor = aColor;
}
