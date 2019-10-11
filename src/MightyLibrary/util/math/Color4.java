package MightyLibrary.util.math;

public class Color4 {

    public static final Color4 WHITE = new Color4(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Color4 BLACK = new Color4(0.0f, 0.0f, 0.0f, 1.0f);
    public static final Color4 RED = new Color4(1.0f, 0.0f, 0.0f, 1.0f);
    public static final Color4 GREEN = new Color4(0.0f, 1.0f, 0.0f, 1.0f);
    public static final Color4 BLUE = new Color4(0.0f, 0.0f, 1.0f, 1.0f);

    private float r;
    private float g;
    private float b;
    private float a;

    public Color4() {
        this.r = 0.0f;
        this.g = 0.0f;
        this.b = 0.0f;
        this.a = 0.0f;
    }

    public Color4(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public float getR() {
        return r;
    }

    public void setR(float r) {
        this.r = r;
    }

    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        this.b = b;
    }

    public float getA() {
        return a;
    }

    public void setA(float a) {
        this.a = a;
    }

    public Color4 copy(){
        return new Color4(r,g,b,a);
    }
}
