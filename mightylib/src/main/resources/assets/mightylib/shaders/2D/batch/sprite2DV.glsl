// Batch of 2D draws: positions already transformed by the model of each draw (CPU), one camera per batch
out vec2 texCoords;
out vec4 vertexColor;

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 inTexCoords;
// RGBA8 normalized, an sRGB color like the packed color of colorShape2D
layout(location = 2) in vec4 inColor;

uniform mat4 projection;
uniform mat4 view;

void main()
{
    gl_Position = projection * view * vec4(aPos, 0.0f, 1.0f);
    texCoords = inTexCoords;
    vertexColor = inColor;
}
