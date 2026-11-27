out vec2 texCoords;
flat out vec4 texRegion;

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 inTexCoords;
// Region of the texture read (utils/textureRegion.glsl): a mesh without this attribute reads (0, 0, 0, 1), no limit
layout(location = 2) in vec4 inTexRegion;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main()
{
    gl_Position = projection * view * model * vec4(aPos, 0.0f, 1.0f);
    texCoords = inTexCoords;
    texRegion = inTexRegion;
}
