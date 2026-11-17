#version 330
out vec2 texCoords;

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 inTexCoords;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main()
{
    gl_Position = projection * view * model * vec4(aPos, 0.0f, 1.0f);
    texCoords = inTexCoords;
}