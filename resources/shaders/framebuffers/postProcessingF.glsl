#version 330

in vec2 texCoords;

out vec4 FragColor;

uniform sampler2D ourTexture;


void main()
{
    FragColor = vec4(texture(ourTexture, texCoords), 1.0);
}