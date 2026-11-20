// Batch of sprite quads by instances: the vertices are the corners of the unit quad, each instance gives the affine
// transform of its quad (model of the draw, size and reference point included), its texture rectangle and its color
out vec2 texCoords;
out vec4 vertexColor;

// Corner of the unit quad, (0, 0) to (1, 1)
layout(location = 0) in vec2 aCorner;
// Rows of the 2x3 affine transform: x = dot(row0, (corner, 1)), y = dot(row1, (corner, 1))
layout(location = 1) in vec3 aRow0;
layout(location = 2) in vec3 aRow1;
// Texture rectangle: (u, v) at the corner (0, 0), then at the corner (1, 1)
layout(location = 3) in vec4 aTexRect;
// RGBA8 normalized, an sRGB color
layout(location = 4) in vec4 aColor;

uniform mat4 projection;
uniform mat4 view;

void main()
{
    vec3 corner = vec3(aCorner, 1.0f);
    gl_Position = projection * view * vec4(dot(aRow0, corner), dot(aRow1, corner), 0.0f, 1.0f);
    texCoords = mix(aTexRect.xy, aTexRect.zw, aCorner);
    vertexColor = aColor;
}
