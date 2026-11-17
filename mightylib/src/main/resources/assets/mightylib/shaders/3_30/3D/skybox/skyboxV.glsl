#version 330 core
layout (location = 0) in vec3 aPos;

out vec3 texCoords;

uniform mat4 view;
uniform mat4 projection;

void main()
{
    texCoords = aPos;
    // Only the rotation of the view: the sky stays around the camera wherever it goes
    gl_Position = projection * mat4(mat3(view)) * vec4(aPos, 1.0);
}
