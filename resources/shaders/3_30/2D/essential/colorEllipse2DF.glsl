#version 330
struct Ellipse
{
    vec2 center;
    vec2 radius;
    float rotation;
};

uniform vec4 color;
uniform vec4 strokeColor; // Stroke color
uniform bool strokeEnabled; // Whether stroke is enabled

in Ellipse transformedEllipse;
in float screenSizeY;
in float strokeSizePercent; // Stroke size in percent of shape

out vec4 FragColor;

float ratioPointInsideEllipse(vec2 point, Ellipse ellipse)
{
    vec2 p = point - ellipse.center;
    float cosTheta = cos(ellipse.rotation);
    float sinTheta = sin(ellipse.rotation);
    vec2 q = vec2(cosTheta * p.x + sinTheta * p.y, -sinTheta * p.x + cosTheta * p.y);

    return ((q.x * q.x) / (ellipse.radius.x * ellipse.radius.x) + (q.y * q.y) / (ellipse.radius.y * ellipse.radius.y));
}


void main()
{
    vec2 fragCoord = vec2(gl_FragCoord.x, screenSizeY - gl_FragCoord.y);
    float ratio = ratioPointInsideEllipse(fragCoord, transformedEllipse);

    if (ratio <= 1.0f)
    {
        if (strokeEnabled && ratio >= 1.0f - strokeSizePercent)
            FragColor = strokeColor;
        else
            FragColor = color;
    }
    else
    discard;
}