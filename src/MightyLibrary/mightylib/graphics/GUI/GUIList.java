package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.scene.Camera;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GUIList {
    private final InputManager inputManager;
    private final MouseManager mouseManager;

    private GUI selected;
    private boolean selectedByMouse;
    private Integer id;
    private Integer oldId;

    public final Map<Integer, GUI> GUIs;

    private int actionUpValue = -1,
            actionDownValue = -1;

    public boolean ShouldLoop = true;

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

    public boolean isMouseSelecting(){ return selectedByMouse; }

    public void setupActionInputValues(int actionUpValue, int actionDownValue){
        this.actionUpValue = actionUpValue;
        this.actionDownValue = actionDownValue;
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
        if (actionUpValue != -1 && inputManager.inputPressed(actionUpValue)) {
            id = selectUp();
        } else if (actionDownValue != -1 && inputManager.inputPressed(actionDownValue)) {
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

    private Integer selectUp(){
        if (this.id == null)
            return selectMinimum();

        Integer maxId = null, minId = null;

        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()) {
            if (minId == null || minId > pair.getKey())
                minId = pair.getKey();

            if (pair.getKey() < id){
                if (maxId == null || maxId < pair.getKey()){
                    maxId = pair.getKey();
                }
            }
        }

        if (maxId == null){
            if (minId != null && ShouldLoop) {
               return minId;
            }
        }

        return maxId;
    }


    private Integer selectDown(){
        if (this.id == null)
            return selectMinimum();

        Integer maxId = null, minId = null;

        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()) {
            if (maxId == null || maxId < pair.getKey())
                maxId = pair.getKey();

            if (pair.getKey() > id){
                if (minId == null || minId > pair.getKey()){
                    minId = pair.getKey();
                }
            }
        }

        if (minId == null){
            if (maxId != null && ShouldLoop) {
                return maxId;
            }
        }

        return minId;
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
            gui.display();
        }
    }
}
