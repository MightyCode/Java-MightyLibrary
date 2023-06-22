package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;

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

    private boolean mouseMoved;

    public GUIList(InputManager inputManager, MouseManager mouseManager) {
        this.mouseManager = mouseManager;
        this.inputManager = inputManager;

        GUIs = new HashMap<>();
        selected = null;
        id = null;

        mouseMoved = false;
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

    public boolean isStateChanged(){
        return !Objects.equals(oldId, id);
    }

    public void update(){
        oldId = id;

        // Check for mouse
        mouseMoved = !mouseManager.oldPos().equals(mouseManager.pos());
        if (mouseMoved){
            checkMoveOver();

            if (selected != null && selectedByMouse && selected.mouseDisableIt()) {
                for (GUI gui : GUIs.values())
                    gui.forceSelect(false);
            }

            return;
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

        checkMoveOver();
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
            if (mouseMoved && pair.getValue().GUIMouseSelected()) {
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
