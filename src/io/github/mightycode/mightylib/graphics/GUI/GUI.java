package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.inputs.EInputState;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;

public abstract class GUI {
    protected Camera2D referenceCamera;
    private boolean forceSelect;

    public GUI(Camera2D referenceCamera){
        this.referenceCamera = referenceCamera;
    }

    public GUI(){
        this(ShaderManager.getInstance().getMainCamera2D());
    }

    public void forceSelect(boolean state){
        this.forceSelect = state;
        this.forceUnselect = false;
    }

    boolean forceSelected() { return forceSelect; }

    private boolean forceUnselect;

    public void forceUnselect(boolean state){
        this.forceSelect = true;
        this.forceUnselect = state;
    }

    boolean forceUnselected() { return forceUnselect; }

    private EInputState trigger = EInputState.Pressed;

    public EInputState getTrigger() { return trigger; }

    /** Moment of the input that activates the element : Pressed (default) or Released. */
    public GUI setTrigger(EInputState trigger) {
        if (trigger != EInputState.Pressed && trigger != EInputState.Released)
            throw new IllegalArgumentException("A GUI is activated on Pressed or Released, not " + trigger);

        this.trigger = trigger;
        return this;
    }

    // Under the mouse, or selected (keyboard, gamepad) and not forced unselected
    boolean isSelectedNow() {
        return (GUIMouseSelected() || forceSelected()) && !forceUnselected();
    }

    /** True at the tick one of the actions is in the trigger state while the element is selected. */
    public boolean isActivated(InputManager inputManager, int... actionIds) {
        if (!isSelectedNow())
            return false;

        for (int actionId : actionIds) {
            if (inputManager.isInput(actionId, trigger))
                return true;
        }

        return false;
    }

    public abstract boolean GUIMouseSelected();

    public abstract void display();

    public abstract boolean mouseDisableIt();

    public abstract void unload();
}
