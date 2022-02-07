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
    private int id;

    public final Map<Integer, GUI> GUIs;

    public GUIList(InputManager inputManager, MouseManager mouseManager) {
        this.mouseManager = mouseManager;
        this.inputManager = inputManager;

        GUIs = new HashMap<>();
        id  = -1;
    }

    public int getSelected(){
        return id;
    }

    public void update(){
        if (mouseManager.oldPos().equals(mouseManager.pos(), PIXEL_PER_SECOND_DISABLE)){
            if (selected != null && selected.mouseDeableIt())
                selected.userSelect(false);
        }

        shouldUpdateSelected();
    }

    private void shouldUpdateSelected(){
        id = -1;
        for (Map.Entry<Integer, GUI> pair : GUIs.entrySet()){
            if (pair.getValue().GUISelected()){
                selected = pair.getValue();
                id = pair.getKey();
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
