package io.github.mightycode.mightylibdemo.main;

import static org.lwjgl.glfw.GLFW.*;

import io.github.mightycode.mightylib.algorithms.wavefunctioncollapse.WaveCollapseRulesLoader;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.EInputType;
import io.github.mightycode.mightylib.inputs.inputType.InputOr;
import io.github.mightycode.mightylib.inputs.inputType.InputSimple;
import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.main.procedures.IProjectLoading;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.map.TileMapLoader;
import io.github.mightycode.mightylib.resources.map.TileSetLoader;
import io.github.mightycode.mightylib.resources.models.ModelLoader;
import io.github.mightycode.mightylib.resources.sound.SoundLoader;
import io.github.mightycode.mightylib.resources.texture.TextureDataAtlasLoader;

class ProjectLoading implements IProjectLoading {
    @Override
    public void init(Resources resources) {
        resources.Loaders.add(new TileSetLoader());
        resources.Loaders.add(new TileMapLoader());
        resources.Loaders.add(new SoundLoader());
        resources.Loaders.add(new ModelLoader());
        resources.Loaders.add(new TextureDataAtlasLoader());
        resources.Loaders.add(new WaveCollapseRulesLoader());
    }

    @Override
    public void contextLoading(Context context){
        InputManager inputManager = context.getInputManager();

        ActionInput[] configurations = {
                new ActionInput(ActionId.ESCAPE, "ESCAPE",
                        new InputSimple(GLFW_KEY_ESCAPE, EInputType.Keyboard)),

                new ActionInput(ActionId.MOVE_LEFT, "MOVE_LEFT",
                        new InputSimple(GLFW_KEY_A, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_RIGHT, "MOVE_RIGHT",
                        new InputSimple(GLFW_KEY_D, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_FORWARD, "MOVE_FORWARD",
                        new InputSimple(GLFW_KEY_W, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_BACKWARD, "MOVE_BACKWARD",
                        new InputSimple(GLFW_KEY_S, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_UP, "MOVE_UP",
                        new InputSimple(GLFW_KEY_SPACE, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_DOWN, "MOVE_DOWN",
                        new InputSimple(GLFW_KEY_LEFT_CONTROL, EInputType.Keyboard)),
                new ActionInput(ActionId.SHIFT, "SHIFT",
                        new InputSimple(GLFW_KEY_LEFT_SHIFT, EInputType.Keyboard)),

                new ActionInput(ActionId.MOVE_LEFT_2D, "MOVE_LEFT_2D",
                        new InputOr(
                                new InputSimple(GLFW_KEY_A, EInputType.Keyboard),
                                new InputSimple(GLFW_KEY_LEFT, EInputType.Keyboard))),
                new ActionInput(ActionId.MOVE_RIGHT_2D, "MOVE_RIGHT_2D",
                        new InputOr(
                                new InputSimple(GLFW_KEY_D, EInputType.Keyboard),
                                new InputSimple(GLFW_KEY_RIGHT, EInputType.Keyboard))),
                new ActionInput(ActionId.MOVE_UP_2D, "MOVE_UP_2D",
                        new InputOr(
                                new InputSimple(GLFW_KEY_W, EInputType.Keyboard),
                                new InputSimple(GLFW_KEY_UP, EInputType.Keyboard))),
                new ActionInput(ActionId.MOVE_DOWN_2D, "MOVE_DOWN_2D",
                        new InputOr(
                                new InputSimple(GLFW_KEY_S, EInputType.Keyboard),
                                new InputSimple(GLFW_KEY_DOWN, EInputType.Keyboard))),

                new ActionInput(ActionId.TAB,
                        "TAB", new InputSimple(GLFW_KEY_TAB, EInputType.Keyboard)),
                new ActionInput(ActionId.ENTER,
                        "ENTER", new InputSimple(GLFW_KEY_ENTER, EInputType.Keyboard)),

                new ActionInput(ActionId.SELECT_UP,
                        "SELECT_UP", new InputSimple(GLFW_KEY_W, EInputType.Keyboard)),
                new ActionInput(ActionId.SELECT_DOWN,
                        "SELECT_DOWN", new InputSimple(GLFW_KEY_S, EInputType.Keyboard)),

                new ActionInput(ActionId.LEFT_CLICK,
                        "LEFT_CLICK", new InputSimple(GLFW_MOUSE_BUTTON_1, EInputType.Mouse)),
                new ActionInput(ActionId.RIGHT_CLICK,
                        "RIGHT_CLICK", new InputSimple(GLFW_MOUSE_BUTTON_2, EInputType.Mouse)),
        };

        inputManager.init(configurations);
    }
}