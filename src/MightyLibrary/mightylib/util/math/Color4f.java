package MightyLibrary.mightylib.util.math;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class Color4f extends Vector4f {

    public Color4f() {
        this(1.0f);
    }

    public Color4f(float color) {
        this(color, color, color, 1.0f);
    }

    public Color4f(float r, float g, float b, float a) {
        super(r, g, b, a);
    }

    public float getR() {
        return x;
    }

    public void setR(float r) {
        this.x = r;
    }
    public void setR255(float r) { setR(r / 255); }

    public float getG() {
        return y;
    }

    public void setG(float g) {
        this.y = g;
    }
    public void setG255(float g) { setG(g / 255); }

    public float getB() {
        return z;
    }

    public void setB(float b) {
        this.z = b;
    }
    public void setB255(float b) { setB(b / 255); }

    public float getA() {
        return w;
    }

    public void setA(float a) {
        this.w = a;
    }
    public void setA255(float a) { setA(a / 255); }

    public void setColor(Vector4f color){
        set(color);
    }

    public void setColorBased256(Vector4f color){
        setColor(color.div(255f));
    }

    public void setColor(Vector3f color){
        setColor(new Vector4f(color, 1.0f));
    }

    public void setColorBased256(Vector3f color){
        setColor(color.div(255f));
    }

    public void setColor(float f){
        setColor(new Vector3f(f));
    }

    public void setColorBased256(float x){
        setColor(x / 255f);
    }


    public Color4f blend(Color4f otherColor, float amount){
        return new Color4f(
                getR() * (1 - amount) + otherColor.getR() * amount,
                getG() * (1 - amount) + otherColor.getG() * amount,
                getB() * (1 - amount) + otherColor.getB() * amount,
                getA() * (1 - amount) + otherColor.getA() * amount);
    }

    public Color4f copy(){
        return new Color4f(x, y, z, w);
    }
}
