#version 140

in vec2 texCoords;

out vec4 FragColor;

uniform sampler2D ourTexture;
void main()
{
    FragColor = texture(ourTexture, texCoords);
}