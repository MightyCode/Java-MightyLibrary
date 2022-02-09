#version 330
out vec2 texCoords;

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 inTexCoords;


void main()
{
    gl_Position = vec4(aPos, 1.0f);
    texCoords = inTexCoords;
}