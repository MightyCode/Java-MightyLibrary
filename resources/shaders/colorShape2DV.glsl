#version 330 core
layout (location = 0) in vec2 aPos;

void main() {
    gl_Position = vec4(aPos.x, 0.0f, aPos.y, 1.0f);
}
