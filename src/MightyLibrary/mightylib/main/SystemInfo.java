package MightyLibrary.mightylib.main;

import org.lwjgl.glfw.GLFW;

public class SystemInfo {
    private final WindowInfo info;

    public SystemInfo(WindowInfo info){
        this.info = info;
    }

    public String getClipboardContent(){
        return GLFW.glfwGetClipboardString(info.getWindowId());
    }

    public void setClipboardContent(String str){
        GLFW.glfwSetClipboardString(info.getWindowId(), str);
    }
}
