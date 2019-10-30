package MightyLibrary.main;

import org.joml.Vector2f;
import org.joml.Vector2i;

public class WindowParams {
    public long windowId;
    public Vector2i size;
    public float ratio;
    public Vector2i virtualSize;

    public WindowParams(){
        windowId = 0;
        size = new Vector2i(1, 1);
        virtualSize = new Vector2i(1,1);
    }


    public WindowParams setWindowId(int windowId){
        this.windowId = windowId;
        return this;
    }


    public WindowParams setSize(Vector2f size){
        return setSize(size.x, size.y);
    }


    public WindowParams setSize(float width, float height){
        return setSize((int)width, (int)height);
    }


    public WindowParams setSize(int width, int height){
        setWidth(width);
        return setHeight(height);
    }

    public WindowParams setVirtualSize(int virtualWidth, int virtualHeight){
        setVirtualWidth(virtualWidth);
        return setVirtualHeight(virtualHeight);
    }


    public WindowParams setWidth(int width){
        this.size.x = width;
        this.ratio = (float) this.size.x / (float) this.size.y;
        return this;
    }


    public WindowParams setHeight(int height){
        this.size.y = height;
        this.ratio = (float) this.size.x / (float) this.size.y;
        return this;
    }


    public WindowParams setVirtualWidth(int width){
        this.virtualSize.x = width;
        return this;
    }

    public WindowParams setVirtualHeight(int height){
        this.virtualSize.y = height;
        return this;
    }

    public WindowParams forceRatio(float ratio){
        this.ratio = ratio;
        return this;
    }
}
