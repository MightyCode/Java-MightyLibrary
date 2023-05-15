#version 330

in vec2 texCoords;

out vec4 FragColor;

uniform vec4 color;

uniform sampler2DRect ourTexture;
void main()
{
    FragColor = texture(ourTexture, texCoords) * color;

    //FragColor = vec4(0, 0, 0, 1);
}