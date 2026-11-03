#version 140

in vec3 aPos;
in vec2 inTexCoords;

out vec2 texCoords;

void main()
{
    gl_Position = vec4(aPos, 1.0f);
    texCoords = inTexCoords;
}