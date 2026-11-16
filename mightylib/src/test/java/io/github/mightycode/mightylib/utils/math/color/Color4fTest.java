package io.github.mightycode.mightylib.utils.math.color;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class Color4fTest {
    private static final float EPS = 1e-4f;

    private static void assertColor(float r, float g, float b, float a, Color4f color) {
        assertEquals(r, color.getR(), EPS, "red");
        assertEquals(g, color.getG(), EPS, "green");
        assertEquals(b, color.getB(), EPS, "blue");
        assertEquals(a, color.getA(), EPS, "alpha");
    }

    @Test
    void constructors() {
        assertColor(1, 1, 1, 1, new Color4f());
        assertColor(0.5f, 0.5f, 0.5f, 1, new Color4f(0.5f));
        assertColor(0.1f, 0.2f, 0.3f, 0.4f, new Color4f(0.1f, 0.2f, 0.3f, 0.4f));
        assertColor(0.1f, 0.2f, 0.3f, 0.7f, new Color4f(new Vector3f(0.1f, 0.2f, 0.3f), 0.7f));
        assertColor(1, 0, 0, 1, new Color4f(Color.RED));
        assertColor(0, 0, 1, 0, new Color4f(new Color(0, 0, 255, 0)));
    }

    @Test
    void hexCodesWithAndWithoutAlpha() {
        assertColor(1, 0, 0, 1, new Color4f("#FF0000"));
        assertColor(0, 1, 0, 1, new Color4f("#00ff00"));
        assertColor(0, 0, 1, 0, new Color4f("#0000FF00"));
        assertColor(0.2f, 0.4f, 0.6f, 1, new Color4f("#336699"));
        assertColor(0.2f, 0.4f, 0.6f, 0.8f, new Color4f("#336699CC"));
    }

    @Test
    void invalidHexCodes() {
        assertThrows(IllegalArgumentException.class, () -> new Color4f("#FFF"));
        assertThrows(IllegalArgumentException.class, () -> new Color4f(""));
        assertThrows(IllegalArgumentException.class, () -> new Color4f("#GG0000"));
        assertThrows(IllegalArgumentException.class, () -> new Color4f("#12345678A"));
    }

    @Test
    void hexRoundTrip() {
        assertEquals("#FF0000FF", new Color4f("#FF0000").toHex());
        assertEquals("#336699CC", new Color4f("#336699CC").toHex());
        assertEquals("#00000000", new Color4f(0, 0, 0, 0).toHex());
    }

    @Test
    void everyByteSurvivesTheHexRoundTrip() {
        for (int value = 0; value < 256; ++value) {
            Color4f color = new Color4f(String.format("#%02X%02X%02X", value, value, value));
            assertEquals(value, color.getR255(), "red " + value);
            assertEquals(value, color.getG255(), "green " + value);
            assertEquals(value, color.getB255(), "blue " + value);
        }
    }

    @Test
    void accessorsAndSettersOnTheZeroTo255Scale() {
        Color4f color = new Color4f(0, 0, 0, 0);
        color.setR255(255);
        color.setG255(51);
        color.setB255(102);
        color.setA255(255);

        assertColor(1, 0.2f, 0.4f, 1, color);
        assertEquals(255, color.getR255());
        assertEquals(255, color.getA255());
        assertEquals(51, color.getG255(), 1);
        assertEquals(102, color.getB255(), 1);
    }

    @Test
    void settersAndGetters() {
        Color4f color = new Color4f();
        color.setR(0.1f);
        color.setG(0.2f);
        color.setB(0.3f);
        color.setA(0.4f);
        assertColor(0.1f, 0.2f, 0.3f, 0.4f, color);
    }

    @Test
    void setColorOverloads() {
        Color4f color = new Color4f(0, 0, 0, 0);

        color.setColor(new Vector4f(0.1f, 0.2f, 0.3f, 0.4f));
        assertColor(0.1f, 0.2f, 0.3f, 0.4f, color);

        color.setColor(new Vector3f(0.5f, 0.6f, 0.7f));
        assertColor(0.5f, 0.6f, 0.7f, 1, color);

        color.setColor(0.25f);
        assertColor(0.25f, 0.25f, 0.25f, 1, color);
    }

    @Test
    void setColorBasedOn256() {
        Color4f color = new Color4f();

        color.setColorBased256(new Vector4f(255, 0, 51, 255));
        assertColor(1, 0, 0.2f, 1, color);

        color.setColorBased256(new Vector3f(0, 255, 0));
        assertColor(0, 1, 0, 1, color);

        color.setColorBased256(51f);
        assertColor(0.2f, 0.2f, 0.2f, 1, color);
    }

    @Test
    void setColorBasedOn256KeepsItsArgument() {
        Vector4f source = new Vector4f(255, 0, 51, 255);
        new Color4f().setColorBased256(source);
        assertEquals(new Vector4f(255, 0, 51, 255), source);
    }

    @Test
    void blendInterpolatesEveryChannel() {
        Color4f black = new Color4f(0, 0, 0, 0), white = new Color4f(1, 1, 1, 1);

        assertColor(0, 0, 0, 0, black.blend(white, 0));
        assertColor(0.25f, 0.25f, 0.25f, 0.25f, black.blend(white, 0.25f));
        assertColor(1, 1, 1, 1, black.blend(white, 1));
        assertColor(0, 0, 0, 0, black, "the receiver is not changed");
    }

    private static void assertColor(float r, float g, float b, float a, Color4f color, String message) {
        assertEquals(r, color.getR(), EPS, message);
        assertEquals(a, color.getA(), EPS, message);
    }

    @Test
    void packedIntegers() {
        assertEquals(0xFF0000,new Color4f("#FF0000").toIntRGB());
        assertEquals(0xFF0000FF, new Color4f("#FF0000").toIntRGBA());
        assertEquals(0x00FF00FF, new Color4f("#00FF00").toIntRGBA());
        assertEquals(0x00000000, new Color4f(0, 0, 0, 0).toIntRGBA());
    }

    @Test
    void hueSaturationLightness() {
        assertHsl(0f, 1f, 0.5f, new Color4f("#FF0000"));
        assertHsl(1f / 3, 1f, 0.5f, new Color4f("#00FF00"));
        assertHsl(2f / 3, 1f, 0.5f, new Color4f("#0000FF"));
        assertHsl(0f, 0f, 1f, new Color4f("#FFFFFF"));
        assertHsl(0f, 0f, 0f, new Color4f("#000000"));
        assertHsl(0f, 0f, 0.5f, new Color4f(0.5f));
        assertHsl(1f / 6, 1f, 0.5f, new Color4f("#FFFF00"));
        assertHsl(0.5f, 1f, 0.5f, new Color4f("#00FFFF"));
        assertHsl(5f / 6, 1f, 0.5f, new Color4f("#FF00FF"));
    }

    @Test
    void aMagentaTintedRedKeepsAPositiveHue() {
        // red with a little blue: the hue goes below 0 and wraps to the end of the circle
        Vector3f hsl = new Color4f(1f, 0f, 0.25f, 1f).toHSL();
        assertTrue(hsl.x > 0.9f && hsl.x < 1f, "hue " + hsl.x);
    }

    @Test
    void lightColorsHaveTheirSaturationComputedFromTheDarkSide() {
        Vector3f hsl = new Color4f(1f, 0.5f, 0.5f, 1f).toHSL();
        assertEquals(1f, hsl.y, EPS);
        assertEquals(0.75f, hsl.z, EPS);
    }

    private static void assertHsl(float h, float s, float l, Color4f color) {
        Vector3f hsl = color.toHSL();
        assertEquals(h, hsl.x, EPS, "hue of " + color);
        assertEquals(s, hsl.y, EPS, "saturation of " + color);
        assertEquals(l, hsl.z, EPS, "lightness of " + color);
    }

    @Test
    void awtConversionClampsOutOfRangeChannels() {
        Color awt = new Color4f(2f, -1f, 0.5f, 1f).toColorAwt();
        assertEquals(255, awt.getRed());
        assertEquals(0, awt.getGreen());
        assertEquals(128, awt.getBlue(), 1);
        assertEquals(255, awt.getAlpha());
    }

    @Test
    void awtRoundTrip() {
        Color awt = new Color(12, 200, 99, 180);
        Color back = new Color4f(awt).toColorAwt();
        assertEquals(awt, back);
    }

    @Test
    void copyIsIndependent() {
        Color4f color = new Color4f(0.1f, 0.2f, 0.3f, 0.4f);
        Color4f copy = color.copy();
        copy.setR(0.9f);

        assertEquals(0.1f, color.getR());
        assertEquals(0.9f, copy.getR());
        assertNotSame(color, copy);
    }

    @Test
    void stringForm() {
        String text = new Color4f(1f, 0.5f, 0f, 1f).toString();
        assertTrue(text.startsWith("Color4f[r="), text);
        assertTrue(text.contains("g=0") && text.contains("a=1"), text);
    }
}
