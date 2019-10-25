#version 330
in layout(location = 0) vec3 aPos;
in layout(location = 1) vec2 inTexCoords;

out vec2 outTexCoords;

void main()
{
    gl_Position = vec4(aPos, 1.0f);
    outTexCoords = inTexCoords;
}