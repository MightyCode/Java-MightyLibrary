package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.inputs.EInputState;
import io.github.mightycode.mightylib.inputs.InputAction;
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

    private final List<String> items = new ArrayList<>();
    private int firstItem;
    private int hovered = -1;
    private InputAction[] activationActions = new InputAction[0];

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

        BackgroundlessButton model = new BackgroundlessButton(context);
        model.Text.setFont(fontName)
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setFontSize(fontSize);
        model.Text.copyTo(model.OverlapsText);
        model.OverlapsText.setColor(new Color4f(0.3f));

        rows = new ArrayList<>();
        for (int i = 0; i < visibleRows; i++) {
            BackgroundlessButton row = (i == 0) ? model : model.copy();
            row.Text.setPosition(new Vector2f(x + (width - SCROLLBAR_WIDTH) * 0.5f, y + rowHeight * (i + 0.5f)));
            row.Text.copyTo(row.OverlapsText);
            row.OverlapsText.setColor(new Color4f(0.3f));
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

    public void setItems(List<String> newItems) {
        items.clear();
        items.addAll(newItems);
        firstItem = clampFirst(firstItem);
        needRefill = true;
    }

    /** Index in the items of the row under the mouse, null if none. */
    public Integer getHovered() {
        return hovered < 0 ? null : hovered;
    }

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

    /** Index in the items of the row activated at this tick, null otherwise. */
    public Integer getActivated() {
        if (hovered < 0)
            return null;

        BackgroundlessButton row = rows.get(hovered - firstItem);
        return row.isActivated(context.getInputManager(), activationActions) ? hovered : null;
    }

    public int getFirstVisible() {
        return firstItem;
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public void scrollTo(int newFirst) {
        int clamped = clampFirst(newFirst);
        if (clamped != firstItem) {
            firstItem = clamped;
            needRefill = true;
        }
    }

    private int clampFirst(int value) {
        return Math.max(0, Math.min(value, items.size() - visibleRows));
    }

    private boolean mouseInArea(Vector2f mouse) {
        return mouse.x >= x && mouse.x <= x + width && mouse.y >= y && mouse.y <= y + height;
    }

    public void update() {
        Vector2f mouse = camera.transformPosition(context.getMouseManager().pos());
        boolean inArea = mouseInArea(mouse);

        if (inArea) {
            // Positive y : wheel up, so the list goes toward its start
            float wheel = context.getMouseManager().getMouseScroll().y;
            if (wheel != 0)
                scrollTo(firstItem - Math.round(wheel * 2f));
        }

        int newHovered = -1;
        if (inArea) {
            int row = (int) ((mouse.y - y) / rowHeight);
            int item = firstItem + row;
            if (row >= 0 && row < visibleRows && item < items.size())
                newHovered = item;
        }
        hovered = newHovered;

        if (needRefill)
            refill();

        for (int i = 0; i < visibleRows; i++) {
            boolean over = firstItem + i == hovered;
            rows.get(i).forceSelect(over);
            if (!over)
                rows.get(i).forceUnselect(true);
        }

        updateThumb();
    }

    private void refill() {
        for (int i = 0; i < visibleRows; i++) {
            BackgroundlessButton row = rows.get(i);
            int item = firstItem + i;
            String label = item < items.size() ? items.get(item) : "";

            row.Text.setText(label);
            row.OverlapsText.setText(label.isEmpty() ? "" : "->" + label + "<-");
        }

        needRefill = false;
    }

    private void updateThumb() {
        float ratio = Math.min(1f, visibleRows / (float) Math.max(1, items.size()));
        float thumbHeight = Math.max(rowHeight * 0.5f, height * ratio);
        int maxFirst = Math.max(1, items.size() - visibleRows);
        float thumbY = y + (height - thumbHeight) * (firstItem / (float) maxFirst);

        scrollThumb.setSizePix(SCROLLBAR_WIDTH, thumbHeight);
        scrollThumb.setPosition(new Vector2f(x + width - SCROLLBAR_WIDTH, thumbY));
    }

    public void display() {
        for (BackgroundlessButton row : rows)
            row.display();

        if (items.size() > visibleRows) {
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
