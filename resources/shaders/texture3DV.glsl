#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 texPos;

out vec2 TexCoord;
out vec3 Normal;
out vec3 FragPos;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    TexCoord = texPos;
    gl_Position = projection * view * model * vec4(FragPos, 1.0);
}
