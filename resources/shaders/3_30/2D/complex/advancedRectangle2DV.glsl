#version 330
struct Rectangle
{
    vec2 center;
    vec2 size;
    float rotation;
    float cornerRoundSize;
};

layout(location = 0) in vec2 aPos;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform Rectangle rectangle;

out Rectangle transformedRectangle;
out vec2 screenSize;

void main()
{
    gl_Position = projection * view * model * vec4(aPos, 0.0f, 1.0f);

    transformedRectangle.center = (view * vec4(rectangle.center, 0.0f, 1.0f)).xy;

    transformedRectangle.size = vec2(view[0][0] * rectangle.size.x / 2, view[1][1] * rectangle.size.y / 2);
    transformedRectangle.rotation = rectangle.rotation;

    screenSize = vec2(-2.0 / projection[0][0], -2.0 / projection[1][1]);

    // Transfer stroke size to screen space
    transformedRectangle.cornerRoundSize = rectangle.cornerRoundSize;
}