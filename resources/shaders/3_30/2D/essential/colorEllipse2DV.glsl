#version 330
struct Ellipse
{
    vec2 center;
    vec2 radius;
    float rotation;
};

layout(location = 0) in vec2 aPos;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform Ellipse ellipse;
uniform float strokeSize; // Stroke size in pixels
uniform bool strokeEnabled; // Whether stroke is enabled

out Ellipse transformedEllipse;
out float screenSizeY;
out float strokeSizePercent; // Stroke size transferred to screen space

void main()
{
    gl_Position = projection * view * model * vec4(aPos, 0.0f, 1.0f);

    transformedEllipse.center = (view * vec4(ellipse.center, 0.0f, 1.0f)).xy;

    transformedEllipse.radius = vec2(view[0][0] * ellipse.radius.x, view[1][1] * ellipse.radius.y);
    transformedEllipse.rotation = ellipse.rotation;

    screenSizeY = -2.0f / projection[1][1];

    // Transfer stroke size to screen space
    strokeSizePercent = strokeSize;
}