package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.MouseManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GUIList {
    private final InputManager inputManager;
    private final MouseManager mouseManager;

    private GUI selected;
    private boolean selectedByMouse;
    private Integer id;
    private Integer oldId;

    private final Map<Integer, GUI> GUIs;

    // null : no navigation by actions
    private InputAction actionUpValue,
            actionDownValue;

    private InputAction[] activationActions = new InputAction[0];

    private boolean looping = true;

    public GUIList(InputManager inputManager, MouseManager mouseManager) {
        this.mouseManager = mouseManager;
        this.inputManager = inputManager;

        GUIs = new HashMap<>();
        selected = null;
        id = null;

        selectedByMouse = false;
    }

    public Integer getSelected(){
        return id;
    }

    // Adds the element, or replaces the one with this id (the selection stays on the id)
    public GUIList put(int id, GUI gui) {
        GUIs.put(id, Objects.requireNonNull(gui, "gui"));
        if (this.id != null && this.id == id) {
            selected = gui;
            // Selected by the actions: the new element is too (by the mouse, it checks the mouse itself)
            if (!selectedByMouse)
                gui.forceSelect(true);
        }

        return this;
    }

    // Removes the element; the selected one is unselected first, so it can't be activated any more
    public GUI remove(int id) {
        GUI removed = GUIs.remove(id);
        if (this.id != null && this.id == id) {
            if (removed != null)
                removed.forceSelect(false);
            selected = null;
            this.id = null;
            selectedByMouse = false;
        }

        return removed;
    }

    public GUI get(int id) {
        return GUIs.get(id);
    }

    // The ids of the elements, unmodifiable
    public Set<Integer> ids() {
        return Collections.unmodifiableSet(GUIs.keySet());
    }

    // Navigation by actions goes from the last element to the first (and back); true by default
    public GUIList setLooping(boolean looping) {
        this.looping = looping;
        return this;
    }

    public boolean isLooping() {
        return looping;
    }

    public boolean isMouseSelecting(){ return selectedByMouse; }

    public void setupActionInputValues(InputAction actionUpValue, InputAction actionDownValue){
        this.actionUpValue = actionUpValue;
        this.actionDownValue = actionDownValue;
    }

    /** Actions (Enter, left click...) that activate the selected element, each on its own trigger. */
    public void setupActivationActions(InputAction... actionIds) {
        this.activationActions = actionIds;
    }

    /** Id of the selected element if it is activated at this tick, null otherwise. */
    public Integer getActivated() {
        if (id == null || selected == null)
            return null;

        return selected.isActivated(inputManager, activationActions) ? id : null;
    }

    public boolean isStateChanged() {
        // Handle the case of unselecting button (move mouse)
        if (id == null)
            return false;

        if (oldId == null)
            return true;

        return !oldId.equals(id);
    }

    public void update(){
        // Store old id
        oldId = id;

        // If mouse move, should unselect button
        boolean mouseMoved = !mouseManager.oldPos().equals(mouseManager.pos());
        if (mouseMoved) {
            selected = null;
            id = null;
        }

        if (id == null) {
            // Check if mouse over a button
            checkMoveOver();
            if (selected != null && selectedByMouse && selected.mouseDisableIt()) {
                for (GUI gui : GUIs.values())
                    gui.forceSelect(false);
            }
        }

        Integer id = null;
        if (actionUpValue != null && inputManager.inputPressed(actionUpValue)) {
            id = selectUp();
        } else if (actionDownValue != null && inputManager.inputPressed(actionDownValue)) {
            id = selectDown();
        }

        // If no action has been done
        if (id == null)
            return;

        if (id.equals(this.id))
            return;

        selected = GUIs.get(id);
        this.id = id;
        selectedByMouse = false;

        for (GUI gui : GUIs.values())
            gui.forceUnselect(true);

        GUIs.get(id).forceSelect(true);
    }

    // Nearest id before the selected one; with looping, from the first to the last
    private Integer selectUp(){
        if (this.id == null)
            return selectMinimum();

        Integer previous = null, last = null;

        for (Integer key : GUIs.keySet()) {
            if (last == null || last < key)
                last = key;

            if (key < id && (previous == null || previous < key))
                previous = key;
        }

        return previous != null || !looping ? previous : last;
    }


    // Nearest id after the selected one; with looping, from the last to the first
    private Integer selectDown(){
        if (this.id == null)
            return selectMinimum();

        Integer next = null, first = null;

        for (Integer key : GUIs.keySet()) {
            if (first == null || first > key)
                first = key;

            if (key > id && (next == null || next > key))
                next = key;
        }

        return next != null || !looping ? next : first;
    }

    private Integer selectMinimum(){
        Integer min = null;
        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()) {
            if (min == null)
                min = pair.getKey();
            else if (min > pair.getKey())
                min = pair.getKey();
        }

        return min;
    }

    private void checkMoveOver(){
        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()){
            if (pair.getValue().GUIMouseSelected()) {
                selected = pair.getValue();
                id = pair.getKey();

                selectedByMouse = true;
                break;
            }
        }
    }

    public void display(){
        for (GUI gui : GUIs.values()){
            gui.display();
        }
    }

    public void unload(){
        for (GUI gui : GUIs.values()){
            gui.unload();
        }
    }
}
