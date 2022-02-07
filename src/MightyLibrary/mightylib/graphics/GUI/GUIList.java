package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;

import java.util.HashMap;
import java.util.Map;

public class GUIList {
    private static final int PIXEL_PER_SECOND_DISABLE = 5;

    private final InputManager inputManager;
    private final MouseManager mouseManager;

    private GUI selected;
    private Integer id;
    private Integer mouseId;

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
        mouseId = null;
    }

    public Integer getSelected(){
        if (mouseId == null)
            return id;

        return mouseId;
    }

    public void setupActionInputValues(int actionUpValue, int actionDownValue){
        this.actionUpValue = actionUpValue;
        this.actionDownValue = actionDownValue;
    }

    public void update(){
        boolean needUpdate = false;

        if (!mouseManager.oldPos().equals(mouseManager.pos(), PIXEL_PER_SECOND_DISABLE)){
            if (selected != null && selected.mouseDisableIt()) {
                selected.forceSelect(false);
            }

            needUpdate = true;
        }

        if (actionUpValue != -1 && inputManager.inputPressed(actionUpValue)) {
            needUpdate = true;
            unselectAll();
            selectUp();
        } else if (actionDownValue != -1 && inputManager.inputPressed(actionDownValue)) {
            needUpdate = true;
            unselectAll();
            selectDown();
        }

        if (needUpdate)
            shouldUpdateSelected();
    }

    private void unselectAll(){
        for (GUI temp : GUIs.values()) {
            temp.forceSelect(false);
        }
    }

    private void selectUp(){
        if (id == null) {
            selectMinimum();
            return;
        }

        Integer maxId = null, minId = null;

        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()) {
            if (minId == null || minId < pair.getKey())
                minId = pair.getKey();

            if (pair.getKey() < id){
                if (maxId == null || maxId < pair.getKey()){
                    maxId = pair.getKey();
                }
            }
        }

        if (maxId == null){
            if (minId != null) {
                GUIs.get(minId).forceSelect(true);
            }
        } else {
            GUIs.get(maxId).forceSelect(true);
        }
    }

    private void selectDown(){
        if (id == null) {
            selectMinimum();
            return;
        }

        Integer maxId = null, minId = null;

        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()) {
            if (maxId == null || maxId > pair.getKey())
                maxId = pair.getKey();

            if (pair.getKey() > id){
                if (minId == null || minId > pair.getKey()){
                    minId = pair.getKey();
                }
            }
        }

        if (minId == null){
            if (maxId != null) {
                GUIs.get(maxId).forceSelect(true);
            }
        } else {
            GUIs.get(minId).forceSelect(true);
        }
    }

    private void selectMinimum(){
        Integer min = null;
        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()) {
            if (min == null)
                min = pair.getKey();
            else if (min > pair.getKey())
                min = pair.getKey();
        }

        if (min != null)
            GUIs.get(min).forceSelect(true);
    }

    private void shouldUpdateSelected(){
        mouseId = null;

        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()){
            if (pair.getValue().forceSelected()){
                selected = pair.getValue();
                id = pair.getKey();
                break;
            } else if (pair.getValue().GUIMouseSelected()){
                selected = pair.getValue();
                mouseId = pair.getKey();
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
