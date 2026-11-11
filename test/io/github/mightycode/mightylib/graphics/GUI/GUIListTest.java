package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.inputs.EInputState;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.InputRegistry;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.IAction;
import org.joml.Vector2f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Selection and activation of GUIList and GUI, without window nor GL (fake mouse, fake actions, fake elements)
class GUIListTest {
    // The registry is shared by the whole JVM : identifiers unique to this class
    private static final InputAction UP = InputRegistry.action("test_gui_list:up");
    private static final InputAction DOWN = InputRegistry.action("test_gui_list:down");
    private static final InputAction VALIDATE = InputRegistry.action("test_gui_list:validate");
    private static final InputAction NOT_BOUND = InputRegistry.action("test_gui_list:not_bound");

    // An action whose state is set by the test
    private static class FakeAction implements IAction {
        boolean held, pressed, released;

        @Override
        public boolean getState(InputManager inputManager) {
            return held;
        }

        @Override
        public boolean actionPressed(InputManager inputManager) {
            return pressed;
        }

        @Override
        public boolean actionReleased(InputManager inputManager) {
            return released;
        }

        void nextTick() {
            pressed = false;
            released = false;
        }
    }

    private static class FakeMouse extends MouseManager {
        final Vector2f position = new Vector2f();
        final Vector2f oldPosition = new Vector2f();

        @Override
        public Vector2f pos() {
            return new Vector2f(position);
        }

        @Override
        public Vector2f oldPos() {
            return new Vector2f(oldPosition);
        }

        void move(float x, float y) {
            oldPosition.set(position);
            position.set(x, y);
        }

        void stay() {
            oldPosition.set(position);
        }
    }

    private static class FakeGUI extends GUI {
        boolean hovered;

        FakeGUI() {
            super(null);
        }

        @Override
        public boolean GUIMouseSelected() {
            return hovered;
        }

        @Override
        public void display() {
        }

        @Override
        public boolean mouseDisableIt() {
            return false;
        }

        @Override
        public void unload() {
        }
    }

    private FakeAction up, down, validate;
    private InputManager inputManager;
    private FakeMouse mouse;
    private GUIList list;
    private FakeGUI first, second, third;

    @BeforeEach
    void setUp() {
        up = new FakeAction();
        down = new FakeAction();
        validate = new FakeAction();
        inputManager = new InputManager(null, null, null);
        inputManager.init(new ActionInput[]{
                new ActionInput(UP, up),
                new ActionInput(DOWN, down),
                new ActionInput(VALIDATE, validate),
        });

        mouse = new FakeMouse();
        list = new GUIList(inputManager, mouse);
        list.setupActionInputValues(UP, DOWN);
        list.setupActivationActions(VALIDATE);

        first = new FakeGUI();
        second = new FakeGUI();
        third = new FakeGUI();
        list.put(1, first);
        list.put(2, second);
        list.put(3, third);
    }

    private void press(FakeAction action) {
        action.pressed = true;
        mouse.stay();
        list.update();
        action.nextTick();
    }

    // GUI

    @Test
    void theDefaultTriggerIsPressed() {
        assertEquals(EInputState.Pressed, first.getTrigger());
    }

    @Test
    void onlyPressedAndReleasedCanTriggerAnElement() {
        first.setTrigger(EInputState.Released);
        assertEquals(EInputState.Released, first.getTrigger());

        assertThrows(IllegalArgumentException.class, () -> first.setTrigger(EInputState.Press));
        assertThrows(IllegalArgumentException.class, () -> first.setTrigger(EInputState.Release));
    }

    @Test
    void anElementIsNotActivatedUnlessSelected() {
        validate.pressed = true;

        assertFalse(first.isActivated(inputManager, VALIDATE));

        first.forceSelect(true);
        assertTrue(first.isActivated(inputManager, VALIDATE));
    }

    @Test
    void anElementUnderTheMouseIsSelected() {
        validate.pressed = true;
        first.hovered = true;

        assertTrue(first.isActivated(inputManager, VALIDATE));
    }

    @Test
    void aForcedUnselectedElementIsNotActivatedEvenUnderTheMouse() {
        validate.pressed = true;
        first.hovered = true;
        first.forceUnselect(true);

        assertFalse(first.isActivated(inputManager, VALIDATE));
    }

    @Test
    void theTriggerChoosesBetweenPressAndRelease() {
        first.forceSelect(true);

        validate.pressed = true;
        assertTrue(first.isActivated(inputManager, VALIDATE));

        first.setTrigger(EInputState.Released);
        assertFalse(first.isActivated(inputManager, VALIDATE));

        validate.pressed = false;
        validate.released = true;
        assertTrue(first.isActivated(inputManager, VALIDATE));
    }

    @Test
    void anUnknownOrMissingActionNeverActivates() {
        first.forceSelect(true);

        assertFalse(first.isActivated(inputManager, NOT_BOUND));
        assertFalse(first.isActivated(inputManager));
    }

    // GUIList

    @Test
    void nothingIsSelectedAtFirst() {
        assertNull(list.getSelected());
        assertNull(list.getActivated());
        assertFalse(list.isStateChanged());
    }

    @Test
    void downSelectsTheLowestIdThenTheNext() {
        press(down);
        assertEquals(1, list.getSelected());
        assertTrue(list.isStateChanged());

        press(down);
        assertEquals(2, list.getSelected());

        press(down);
        assertEquals(3, list.getSelected());
    }

    @Test
    void upSelectsTheLowestIdWhenNothingIsSelected() {
        press(up);
        assertEquals(1, list.getSelected());
    }

    @Test
    void theSelectionLoopsByDefault() {
        press(down);
        assertEquals(1, list.getSelected());

        // Up from the first goes to the last, down from the last goes to the first
        press(up);
        assertEquals(3, list.getSelected());

        press(down);
        assertEquals(1, list.getSelected());
    }

    @Test
    void theSelectionStopsAtTheEndsWithoutLoop() {
        list.setLooping(false);
        press(down);
        press(up);
        assertEquals(1, list.getSelected());

        press(down);
        press(down);
        press(down);
        assertEquals(3, list.getSelected());
    }

    @Test
    void onlyTheSelectedElementIsSelectedAfterAMove() {
        press(down);
        press(down);
        validate.pressed = true;

        assertFalse(first.isActivated(inputManager, VALIDATE));
        assertTrue(second.isActivated(inputManager, VALIDATE));
        assertFalse(third.isActivated(inputManager, VALIDATE));
    }

    @Test
    void removingTheSelectedElementUnselectsIt() {
        press(down);
        press(down);
        assertEquals(2, list.getSelected());

        assertSame(second, list.remove(2));
        assertNull(list.getSelected());
        assertNull(list.get(2));

        validate.pressed = true;
        assertNull(list.getActivated());
    }

    @Test
    void removingAnotherElementKeepsTheSelection() {
        press(down);
        list.remove(3);
        assertEquals(1, list.getSelected());

        // The removed element is skipped by the navigation
        list.setLooping(false);
        press(down);
        press(down);
        assertEquals(2, list.getSelected());
    }

    @Test
    void replacingTheSelectedElementSelectsTheNewOne() {
        press(down);
        FakeGUI replacing = new FakeGUI();
        assertTrue(first.forceSelected());
        list.put(1, replacing);
        assertFalse(first.forceSelected());

        validate.pressed = true;
        assertEquals(1, list.getActivated());
        assertSame(replacing, list.get(1));
    }

    @Test
    void idsCantBeChanged() {
        assertEquals(java.util.Set.of(1, 2, 3), list.ids());
        assertThrows(UnsupportedOperationException.class, () -> list.ids().remove(1));
    }

    @Test
    void getActivatedGivesTheSelectedIdOnlyWhenTheActionHappens() {
        press(down);
        press(down);
        assertNull(list.getActivated());

        validate.pressed = true;
        assertEquals(2, list.getActivated());

        validate.nextTick();
        assertNull(list.getActivated());
    }

    @Test
    void aStateChangeLastsOneUpdate() {
        press(down);
        assertTrue(list.isStateChanged());

        mouse.stay();
        list.update();
        assertFalse(list.isStateChanged());
    }

    @Test
    void theMouseSelectsTheElementItIsOver() {
        second.hovered = true;
        mouse.stay();
        list.update();

        assertEquals(2, list.getSelected());
        assertTrue(list.isMouseSelecting());
    }

    @Test
    void movingTheMouseUnselectsThenSelectsWhatIsUnderIt() {
        press(down);
        assertEquals(1, list.getSelected());
        assertFalse(list.isMouseSelecting());

        third.hovered = true;
        mouse.move(5, 5);
        list.update();

        assertEquals(3, list.getSelected());
        assertTrue(list.isMouseSelecting());
    }

    @Test
    void movingTheMouseOverNothingUnselects() {
        press(down);

        mouse.move(5, 5);
        list.update();

        assertNull(list.getSelected());
        assertNull(list.getActivated());
    }

    @Test
    void thePressedTriggerActivatesOnPressAndTheReleasedTriggerOnRelease() {
        second.setTrigger(EInputState.Released);
        press(down);
        press(down);

        validate.pressed = true;
        assertNull(list.getActivated());

        validate.pressed = false;
        validate.released = true;
        assertEquals(2, list.getActivated());
    }
}
