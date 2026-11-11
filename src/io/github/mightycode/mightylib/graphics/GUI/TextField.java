package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.console.ConsoleInput;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.InputFocus;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * One line of text typed by the player. It takes the focus of its window when clicked (the scene then sees no
 * input: InputFocus) and gives it back on a click elsewhere, Escape, Enter, Up or Down. Edits with the keys of the
 * console command line (cursor, selection, words, Ctrl+A/C/X/V) and a hint is shown while it is empty.
 * The scene calls update() once per tick, then reads isChanged(), isSubmitted() and getLeftWith().
 */
public class TextField extends GUI {
    private static final float PADDING = 8f;
    private static final long CURSOR_BLINK_NANOSECONDS = 500_000_000L;

    // Background: focused (typing), selected (under the mouse or forced by a GUIList or a scene), or neither
    private static final Color4f FOCUSED_COLOR = new Color4f(0.2f, 0.24f, 0.36f, 1f);
    private static final Color4f SELECTED_COLOR = new Color4f(0.17f, 0.18f, 0.26f, 1f);
    private static final Color4f IDLE_COLOR = new Color4f(0.12f, 0.12f, 0.16f, 1f);

    private final WindowContext context;
    private final float x, y, width, height;

    private final TextFieldModel model;
    private final TextFieldModel.Tick tick = new TextFieldModel.Tick();

    private final RectangleRenderer background;
    private final RectangleRenderer selection;
    private final RectangleRenderer cursor;
    private final Text text;
    private final Text hint;
    // The color the background has now, set again only when it changes
    private Color4f backgroundColor;
    // Reused by display, called at each frame
    private final Vector2f drawPosition = new Vector2f();

    private final ConsoleInput.IClipboard clipboard;

    /** @param x, y, width, height area in virtual pixels (x, y = top left) */
    public TextField(WindowContext context, float x, float y, float width, float height, String fontName, float fontSize) {
        super();
        this.context = context;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        text = new Text();
        text.setFont(fontName)
                .setFontSize(fontSize)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setColor(new Color4f(1f, 1f, 1f, 1f));

        hint = text.createCopy();
        hint.setColor(new Color4f(0.55f, 0.55f, 0.6f, 1f));

        model = new TextFieldModel(this, text::measureWidth);
        model.setAvailableWidth(width - 2 * PADDING);

        background = new RectangleRenderer("colorShape2D");
        background.setPosition(new Vector2f(x, y));
        background.setSizePix(width, height);

        selection = new RectangleRenderer("colorShape2D");
        selection.setColorMode(new Color4f(0.25f, 0.45f, 0.9f, 0.6f));

        cursor = new RectangleRenderer("colorShape2D");
        cursor.setColorMode(new Color4f(1f, 1f, 1f, 1f));

        clipboard = new ConsoleInput.IClipboard() {
            @Override
            public String get() {
                return TextField.this.context.getSystemInfo().getClipboardContent();
            }

            @Override
            public void set(String content) {
                TextField.this.context.getSystemInfo().setClipboardContent(content);
            }
        };
    }

    public String getText() { return model.getText(); }

    /** Replaces the text (cursor at the end); isChanged() is not set. */
    public TextField setText(String content) {
        model.setText(content);
        return this;
    }

    /** Shown in grey while the text is empty. */
    public TextField setHint(String content) {
        hint.setText(content);
        return this;
    }

    /** True at the tick the text changed (typed, deleted, pasted). */
    public boolean isChanged() { return model.isChanged(); }

    /** True at the tick Enter was pressed in the field. */
    public boolean isSubmitted() { return model.isSubmitted(); }

    /** The key that gave the focus back at this tick (Escape, Enter, Up, Down: GLFW_KEY_...), -1 if none. */
    public int getLeftWith() { return model.getLeftWith(); }

    public boolean hasFocus() { return focus().isCurrent(this); }

    /** For the scene that activates the field by the keyboard (a GUIList). */
    public void requestFocus() { focus().request(this); }

    public void releaseFocus() { focus().release(this); }

    private InputFocus focus() { return context.getInputManager().focus(); }

    // The cursor of the window in the virtual space of the GUI, whatever the focus
    private Vector2f mouse() {
        return referenceCamera.transformPosition(context.getMouseManager().getRawVirtualPos());
    }

    private boolean isInside(Vector2f point) {
        return point.x >= x && point.x <= x + width && point.y >= y && point.y <= y + height;
    }

    public void update() {
        MouseManager mouse = context.getMouseManager();
        KeyboardManager keyboard = context.getKeyboardManager();
        Vector2f cursorPosition = mouse();

        tick.typedText = keyboard.getRawTypedText();
        tick.keys = keyboard.getRawKeyEvents();
        tick.clipboard = clipboard;
        tick.mousePressed = mouse.rawButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        tick.mouseHeld = mouse.getRawButton(GLFW_MOUSE_BUTTON_LEFT);
        tick.shift = keyboard.getRawKeyState(GLFW_KEY_LEFT_SHIFT) || keyboard.getRawKeyState(GLFW_KEY_RIGHT_SHIFT);
        tick.mouseInside = isInside(cursorPosition);
        tick.mouseX = cursorPosition.x - x - PADDING;

        model.update(focus(), tick);
    }

    @Override
    public boolean GUIMouseSelected() {
        // Hidden by the focus of another owner: not over the field then
        MouseManager mouse = context.getMouseManager();
        return !mouse.isCaptured() && isInside(referenceCamera.transformPosition(mouse.pos()));
    }

    @Override
    public boolean mouseDisableIt() {
        return true;
    }

    @Override
    public void display() {
        boolean focused = hasFocus();
        Color4f color = focused ? FOCUSED_COLOR : isSelectedNow() ? SELECTED_COLOR : IDLE_COLOR;
        if (color != backgroundColor) {
            backgroundColor = color;
            background.setColorMode(color);
        }
        background.display();

        float textY = y + (height - text.lineHeight()) * 0.5f;
        float textX = x + PADDING;
        String content = model.getText();

        if (content.isEmpty()) {
            hint.setPosition(drawPosition.set(textX, textY));
            hint.display();
        } else {
            int start = model.visibleStart();
            int end = model.visibleEnd();

            int from = Math.max(start, model.editor().selectionStart());
            int to = Math.min(end, model.editor().selectionEnd());
            if (focused && model.editor().hasSelection() && to > from) {
                selection.setPosition(drawPosition.set(textX + model.xOf(from), textY));
                selection.setSizePix(model.xOf(to) - model.xOf(from), text.lineHeight());
                selection.display();
            }

            text.setText(content.substring(start, end));
            text.setPosition(drawPosition.set(textX, textY));
            text.display();
        }

        boolean blinkOn = (System.nanoTime() / CURSOR_BLINK_NANOSECONDS) % 2 == 0;
        if (focused && blinkOn) {
            cursor.setPosition(drawPosition.set(textX + model.xOf(model.editor().getCursor()), textY));
            cursor.setSizePix(2, text.lineHeight());
            cursor.display();
        }
    }

    @Override
    public void unload() {
        // A scene left while typing must not keep the focus
        releaseFocus();

        background.unload();
        selection.unload();
        cursor.unload();
        text.unload();
        hint.unload();
    }
}
