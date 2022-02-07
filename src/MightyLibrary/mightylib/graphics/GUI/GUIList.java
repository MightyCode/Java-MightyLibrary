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

    public final Map<Integer, GUI> GUIs;

    public GUIList(InputManager inputManager, MouseManager mouseManager) {
        this.mouseManager = mouseManager;
        this.inputManager = inputManager;

        GUIs = new HashMap<>();
    }

    public void update(){
        if (mouseManager.oldPos().equals(mouseManager.pos(), PIXEL_PER_SECOND_DISABLE)){
            if (selected.mouseDeableIt())
                selected.userSelect(false);
        }


    }
}
