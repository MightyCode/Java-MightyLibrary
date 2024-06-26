#version 330 core

out vec3 FragPos;
out vec2 TexCoords;
out vec3 Normal;

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 texPos;
layout (location = 2) in vec3 aNormal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    FragPos = vec3(model * vec4(aPos, 1.0));
    TexCoords = texPos;
    Normal = mat3(transpose(inverse(model))) * aNormal;

    gl_Position = projection * view  * vec4(FragPos, 1.0);
}
