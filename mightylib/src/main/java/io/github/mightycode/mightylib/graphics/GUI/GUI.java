package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.inputs.EInputState;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;

/**
 * An element of interface selected by the mouse, the keyboard or a gamepad, activated by an input (isActivated).
 * Hooks: GUIMouseSelected, mouseDisableIt, display and unload. The camera it is drawn with is referenceCamera().
 */
public abstract class GUI {
    private final Camera2D referenceCamera;
    private boolean forceSelect;

    public GUI(Camera2D referenceCamera){
        this.referenceCamera = referenceCamera;
    }

    public GUI(){
        this(Views.current().camera2D());
    }

    // The camera the element is drawn with: transforms the mouse position into its coordinates
    protected final Camera2D referenceCamera() {
        return referenceCamera;
    }

    public final void forceSelect(boolean state){
        this.forceSelect = state;
        this.forceUnselect = false;
    }

    boolean forceSelected() { return forceSelect; }

    private boolean forceUnselect;

    public final void forceUnselect(boolean state){
        this.forceSelect = true;
        this.forceUnselect = state;
    }

    boolean forceUnselected() { return forceUnselect; }

    private EInputState trigger = EInputState.Pressed;

    public final EInputState getTrigger() { return trigger; }

    /** Moment of the input that activates the element : Pressed (default) or Released. */
    public final GUI setTrigger(EInputState trigger) {
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
    public final boolean isActivated(IInputManager inputManager, InputAction... actionIds) {
        if (!isSelectedNow())
            return false;

        for (InputAction actionId : actionIds) {
            if (inputManager.isInput(actionId, trigger))
                return true;
        }

        return false;
    }

    // Hook: the mouse is over the element (in the coordinates of referenceCamera())
    public abstract boolean GUIMouseSelected();

    // Hook: draws the element, selected or not
    public abstract void display();

    // Hook: true when the mouse takes the selection away from the keyboard and gamepad (it moved over another place)
    public abstract boolean mouseDisableIt();

    // Hook: releases the renderers of the element
    public abstract void unload();
}
