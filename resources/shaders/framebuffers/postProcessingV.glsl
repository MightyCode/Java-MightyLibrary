#version 330
// Will resize the virtual screen to real screen and affects some post processing effect


in layout(location = 0) vec3 aPos;
in layout(location = 1) vec2 inTexCoords;

out vec2 texCoords;

void main()
{
    gl_Position = vec4(aPos, 1.0f);
    texCoords = inTexCoords;
}