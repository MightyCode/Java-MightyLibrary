package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.inputs.EInputState;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.KeyRepeat;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical list of text rows, scrolled with the mouse wheel.
 * Only the visible rows exist as GL objects (a fixed pool re-filled on scroll),
 * so the number of items does not matter.
 *
 * One row is selected (ListSelection) : by the keyboard (setupKeyboardActions), by the mouse moving over a row,
 * or by setSelected. The list scrolls to keep it shown. The row under the mouse is getHovered() ; a keyboard move
 * or new items hide the hover until the mouse moves again.
 */
public class ScrollList {
    private static final float SCROLLBAR_WIDTH = 6f;

    private final WindowContext context;
    private final Camera2D camera;

    private final float x, y, width, height;
    private final float rowHeight;
    private final int visibleRows;

    private final List<BackgroundlessButton> rows;
    private final RectangleRenderer scrollTrack, scrollThumb;

    private final ListSelection<String> selection;
    private int hovered = -1;
    // After a keyboard move or new items: the mouse resting on a row is not a choice until it moves
    private boolean hoverSuppressed;
    private InputAction[] activationActions = new InputAction[0];

    private InputAction actionUp, actionDown;
    private final KeyRepeat repeatUp = new KeyRepeat();
    private final KeyRepeat repeatDown = new KeyRepeat();
    private InputAction[] keyboardActivationActions = new InputAction[0];

    private boolean needRefill = true;

    /** @param x, y, width, height area in virtual pixels (x, y = top left) */
    public ScrollList(WindowContext context, float x, float y, float width, float height,
                      float rowHeight, String fontName, float fontSize) {
        this.context = context;
        this.camera = Views.current().camera2D();

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rowHeight = rowHeight;
        this.visibleRows = Math.max(1, (int) (height / rowHeight));
        this.selection = new ListSelection<>(visibleRows);

        BackgroundlessButton model = new BackgroundlessButton(context);
        model.text().setFont(fontName)
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setFontSize(fontSize);
        model.text().copyTo(model.overlapsText());
        model.overlapsText().setColor(new Color4f(0.3f));

        rows = new ArrayList<>();
        for (int i = 0; i < visibleRows; i++) {
            BackgroundlessButton row = (i == 0) ? model : model.copy();
            row.text().setPosition(new Vector2f(x + (width - SCROLLBAR_WIDTH) * 0.5f, y + rowHeight * (i + 0.5f)));
            row.text().copyTo(row.overlapsText());
            row.overlapsText().setColor(new Color4f(0.3f));
            rows.add(row);
        }

        scrollTrack = new RectangleRenderer("colorShape2D");
        scrollTrack.setColorMode(new Color4f(0f, 0f, 0f, 0.15f));
        scrollTrack.setPosition(new Vector2f(x + width - SCROLLBAR_WIDTH, y));
        scrollTrack.setSizePix(SCROLLBAR_WIDTH, height);

        scrollThumb = new RectangleRenderer("colorShape2D");
        scrollThumb.setColorMode(new Color4f(0f, 0f, 0f, 0.5f));
        scrollThumb.setPosition(new Vector2f(x + width - SCROLLBAR_WIDTH, y));
        scrollThumb.setSizePix(SCROLLBAR_WIDTH, height);
    }

    /**
     * Replaces the items. The selection stays on the same item if it is still there, otherwise it goes to the first.
     */
    public void setItems(List<String> newItems) {
        selection.setItems(newItems);
        hoverSuppressed = true;
        needRefill = true;
    }

    /** Index in the items of the row under the mouse, null if none (or hidden by the keyboard, see above). */
    public Integer getHovered() {
        return hovered < 0 ? null : hovered;
    }

    /** Index in the items of the selected row, null if the list is empty. */
    public Integer getSelected() {
        return selection.selected() < 0 ? null : selection.selected();
    }

    /** The selected row, scrolling to show it. */
    public void setSelected(int index) {
        if (selection.select(index))
            hoverSuppressed = true;
        needRefill = true;
    }

    /**
     * The row to show information about : the hovered one, otherwise the selected one. Null if the list is empty.
     */
    public Integer getActive() {
        return hovered >= 0 ? Integer.valueOf(hovered) : getSelected();
    }

    /** One row up (negative) or down, without loop ; false when already at the end. */
    public boolean moveSelection(int delta) {
        boolean moved = selection.move(delta);
        if (moved) {
            hoverSuppressed = true;
            needRefill = true;
        }

        return moved;
    }

    public boolean isSelectionAtStart() { return selection.isAtStart(); }

    public boolean isSelectionAtEnd() { return selection.isAtEnd(); }

    /**
     * Keyboard : up and down move the selection (update), the activation actions (Enter...) activate the selected
     * row, each on its own trigger. For a list that is not driven by a GUIList : the keys act at each tick.
     */
    public void setupKeyboardActions(InputAction up, InputAction down, InputAction... activationActions) {
        this.actionUp = up;
        this.actionDown = down;
        this.keyboardActivationActions = activationActions;
    }

    /**
     * Up and down held move the selection again and again : after the delay, one row at each interval (seconds).
     * By default the repeat of the keyboards of the system (0.5 s, then 30 rows a second).
     */
    public void setKeyRepeat(float delay, float interval) {
        repeatUp.setRepeat(delay, interval);
        repeatDown.setRepeat(delay, interval);
    }

    /** Actions (left click...) that activate the row under the mouse. */
    public void setupActivationActions(InputAction... actionIds) {
        this.activationActions = actionIds;
    }

    /** Moment of the input that activates a row : Pressed (default) or Released. */
    public ScrollList setTrigger(EInputState trigger) {
        for (BackgroundlessButton row : rows)
            row.setTrigger(trigger);

        return this;
    }

    public EInputState getTrigger() {
        return rows.get(0).getTrigger();
    }

    /** Index in the items of the row activated at this tick (clicked, or selected and Enter), null otherwise. */
    public Integer getActivated() {
        InputManager inputManager = context.getInputManager();
        if (hovered >= 0) {
            BackgroundlessButton row = rows.get(hovered - selection.first());
            if (row.isActivated(inputManager, activationActions))
                return hovered;
        }

        if (selection.selected() >= 0) {
            for (InputAction action : keyboardActivationActions) {
                if (inputManager.isInput(action, getTrigger()))
                    return selection.selected();
            }
        }

        return null;
    }

    public int getFirstVisible() {
        return selection.first();
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public void scrollTo(int newFirst) {
        if (selection.scrollTo(newFirst))
            needRefill = true;
    }

    private boolean mouseInArea(Vector2f mouse) {
        return mouse.x >= x && mouse.x <= x + width && mouse.y >= y && mouse.y <= y + height;
    }

    public void update() {
        InputManager inputManager = context.getInputManager();
        Vector2f mouse = camera.transformPosition(context.getMouseManager().pos());
        boolean inArea = mouseInArea(mouse);
        boolean mouseMoved = !context.getMouseManager().oldPos().equals(context.getMouseManager().pos());

        boolean wheeled = false;
        if (inArea) {
            // Positive y : wheel up, so the list goes toward its start
            float wheel = context.getMouseManager().getMouseScroll().y;
            if (wheel != 0) {
                scrollTo(selection.first() - Math.round(wheel * 2f));
                wheeled = true;
            }
        }

        float seconds = GameTime.DeltaTime();
        if (actionDown != null) {
            int steps = repeatDown.update(inputManager.inputPressed(actionDown), inputManager.getState(actionDown), seconds);
            for (int i = 0; i < steps; ++i)
                moveSelection(1);
        }
        if (actionUp != null) {
            int steps = repeatUp.update(inputManager.inputPressed(actionUp), inputManager.getState(actionUp), seconds);
            for (int i = 0; i < steps; ++i)
                moveSelection(-1);
        }

        // A click on a row is a choice even if the mouse did not move since the keyboard did
        boolean clicked = false;
        for (InputAction action : activationActions)
            clicked |= inputManager.isInput(action, getTrigger());

        // The mouse moving, the wheel turning under it or a click make the hover a choice again
        if (mouseMoved || wheeled || clicked)
            hoverSuppressed = false;

        // The rows show what the hover is tested against
        if (needRefill)
            refill();

        // Hovered : the mouse is over the text of a row, not only in its band
        int newHovered = -1;
        if (inArea && !hoverSuppressed) {
            int row = (int) ((mouse.y - y) / rowHeight);
            int item = selection.first() + row;
            if (row >= 0 && row < visibleRows && item < selection.count() && rows.get(row).GUIMouseSelected())
                newHovered = item;
        }
        hovered = newHovered;

        // The mouse moving over a row, or clicking it, selects it
        if ((mouseMoved || clicked) && hovered >= 0 && selection.select(hovered))
            needRefill = true;

        if (needRefill)
            refill();

        Integer activeRow = getActive();
        int active = activeRow == null ? -1 : activeRow;
        for (int i = 0; i < visibleRows; i++) {
            boolean over = selection.first() + i == active;
            rows.get(i).forceSelect(over);
            if (!over)
                rows.get(i).forceUnselect(true);
        }

        updateThumb();
    }

    private void refill() {
        for (int i = 0; i < visibleRows; i++) {
            BackgroundlessButton row = rows.get(i);
            int item = selection.first() + i;
            String label = item < selection.count() ? selection.items().get(item) : "";

            row.text().setText(label);
            row.overlapsText().setText(label.isEmpty() ? "" : "->" + label + "<-");
        }

        needRefill = false;
    }

    private void updateThumb() {
        float ratio = Math.min(1f, visibleRows / (float) Math.max(1, selection.count()));
        float thumbHeight = Math.max(rowHeight * 0.5f, height * ratio);
        int maxFirst = Math.max(1, selection.count() - visibleRows);
        float thumbY = y + (height - thumbHeight) * (selection.first() / (float) maxFirst);

        scrollThumb.setSizePix(SCROLLBAR_WIDTH, thumbHeight);
        scrollThumb.setPosition(new Vector2f(x + width - SCROLLBAR_WIDTH, thumbY));
    }

    public void display() {
        for (BackgroundlessButton row : rows)
            row.display();

        if (selection.count() > visibleRows) {
            scrollTrack.display();
            scrollThumb.display();
        }
    }

    public void unload() {
        for (BackgroundlessButton row : rows)
            row.unload();

        scrollTrack.unload();
        scrollThumb.unload();
    }
}
