package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.glViewport;

/**
 * Draws the console in a rectangle of a window: the last lines of the log (cut to the width), the command line and
 * its cursor. Created, drawn and unloaded under its ConsoleView (Views.enter) with the window's OpenGL context current.
 * One Text per visible row: the texts change only when the log, the scroll or the size change.
 */
final class ConsolePanel {
    // Wanted distance between two rows, in pixels
    private static final float ROW_HEIGHT = 20;
    private static final float PADDING = 6;
    private static final String PROMPT = "> ";
    private static final long CURSOR_BLINK_NANOSECONDS = 530_000_000L;
    private static final long FLASH_NANOSECONDS = 150_000_000L;
    private static final Color4f FLASH_COLOR = new Color4f(0.45f, 0.45f, 0.45f, 0.8f);

    private static final Color4f OUT_COLOR = new Color4f(0.85f, 0.85f, 0.85f, 1f);
    private static final Color4f ERR_COLOR = new Color4f(1f, 0.45f, 0.45f, 1f);
    private static final Color4f COMMAND_COLOR = new Color4f(1f, 0.85f, 0.35f, 1f);
    private static final Color4f RESULT_COLOR = new Color4f(1f, 1f, 1f, 1f);
    private static final Color4f INPUT_COLOR = new Color4f(1f, 1f, 1f, 1f);

    private final ConsoleView view;
    private final ConsoleLog log;
    private final CommandLineEditor editor;
    private final String fontName;

    private final RectangleRenderer background;
    private final RectangleRenderer inputBackground;
    private final Color4f inputBackgroundColor;
    private final ShaderValue inputBackgroundValue;
    // The command line is lit until then (System.nanoTime), 0: not lit
    private long flashEnd;
    private final RectangleRenderer cursor;
    private final Text inputText;
    private final List<Text> rows;

    private float fontSize;
    private float lineHeight;

    // Rows hidden below the visible ones (0: the newest lines are shown)
    private int scrollOffset;
    // First character of the command line shown, when it is wider than the panel
    private int inputScroll;

    private final Supplier<ECommandLevel> viewer;
    private ECommandLevel shownLevel;

    // What the rows were built for
    private long builtVersion = -1;
    private ECommandLevel builtLevel;
    private int builtScroll = -1;
    private float builtWidth = -1;
    private float builtHeight = -1;
    private float builtX = -1;
    private float builtY = -1;
    private float builtFontSize = -1;
    private int visibleRows;

    // viewer: the level of the player, only the lines it sees are shown
    ConsolePanel(ConsoleView view, ConsoleLog log, Supplier<ECommandLevel> viewer, CommandLineEditor editor,
                 String fontName, Color4f backgroundColor, Color4f inputBackgroundColor) {
        this.view = view;
        this.log = log;
        this.viewer = viewer;
        this.editor = editor;
        this.fontName = fontName;

        background = new RectangleRenderer("colorShape2D");
        background.setColorMode(backgroundColor);
        inputBackground = new RectangleRenderer("colorShape2D");
        this.inputBackgroundColor = inputBackgroundColor;
        inputBackgroundValue = inputBackground.setColorMode(inputBackgroundColor);
        cursor = new RectangleRenderer("colorShape2D");
        cursor.setColorMode(new Color4f(1f, 1f, 1f, 0.85f));

        inputText = createText();
        inputText.setColor(INPUT_COLOR);
        rows = new ArrayList<>();
    }

    private Text createText() {
        Text text = new Text();
        text.setFont(fontName)
                .setAlignment(ETextAlignment.Left)
                .setReference(EDirection.LeftUp);
        text.setFontSize(fontSize > 0 ? fontSize : 1);
        return text;
    }

    ConsoleView getView() { return view; }

    // Rows shown at once: the scroll of Page Up / Page Down
    int getVisibleRows() { return Math.max(1, visibleRows); }

    /** Toward the oldest lines (positive) or the newest (negative), clamped when drawn. */
    void scroll(int rows) {
        scrollOffset = Math.max(0, scrollOffset + rows);
    }

    void scrollToEnd() {
        scrollOffset = 0;
    }

    // Visual bell: the command line is lit a moment (a Tab with nothing new to show)
    void flash() {
        flashEnd = System.nanoTime() + FLASH_NANOSECONDS;
    }

    // The window's OpenGL context is current, the console view entered
    void display(float x, float y, float width, float height, boolean showCursor) {
        view.pixelCamera().refresh();
        glViewport(0, 0, view.context().getWindow().getInfo().getSizeRef().x, view.context().getWindow().getInfo().getSizeRef().y);

        updateFontSize();

        float inputTop = y + height - PADDING - lineHeight;
        int rowCount = Math.max(0, (int) ((inputTop - PADDING - y) / lineHeight));
        if (rowCount != visibleRows)
            resizeRows(rowCount);

        shownLevel = viewer.get();
        // A line hidden from the viewer (program output for the player) rebuilds nothing
        if (log.version(shownLevel) != builtVersion || shownLevel != builtLevel || scrollOffset != builtScroll || width != builtWidth
                || height != builtHeight || x != builtX || y != builtY || fontSize != builtFontSize)
            buildRows(x, y, width, height, inputTop);

        background.setSizePix(width, height);
        background.setPosition(new Vector2f(x, y));
        background.display();

        boolean flashing = flashEnd != 0 && System.nanoTime() < flashEnd;
        if (!flashing)
            flashEnd = 0;
        inputBackgroundValue.setObject(flashing ? FLASH_COLOR : inputBackgroundColor);
        inputBackground.setSizePix(width, lineHeight + PADDING);
        inputBackground.setPosition(new Vector2f(x, inputTop - PADDING * 0.5f));
        inputBackground.display();

        for (Text row : rows)
            row.display();

        displayInput(x + PADDING, inputTop, width - 2 * PADDING, showCursor);
    }

    // Font size giving rows of ROW_HEIGHT pixels with the metrics of the font used now
    private void updateFontSize() {
        // Height of a row for a font size of 1 (changing the size of a text rebuilds it: measured, not set)
        float unit = inputText.lineHeight() / inputText.getFontSize();
        float wanted = unit > 0 ? ROW_HEIGHT / unit : ROW_HEIGHT;
        if (wanted != fontSize) {
            fontSize = wanted;
            for (Text row : rows)
                row.setFontSize(fontSize);
        }

        inputText.setFontSize(fontSize);
        lineHeight = Math.max(1, inputText.lineHeight());
    }

    private void resizeRows(int count) {
        while (rows.size() > count)
            rows.remove(rows.size() - 1).unload();
        while (rows.size() < count)
            rows.add(createText());

        visibleRows = count;
        builtVersion = -1;
    }

    // The newest lines at the bottom, just above the command line
    private void buildRows(float x, float y, float width, float height, float inputTop) {
        float textWidth = width - 2 * PADDING;
        List<RowText> wrapped = lastRows(textWidth);

        int shown = wrapped.size();
        for (int i = 0; i < rows.size(); ++i) {
            Text row = rows.get(i);
            int index = i - (rows.size() - shown);
            if (index < 0) {
                row.setText("");
                continue;
            }

            RowText content = wrapped.get(index);
            row.setFontSize(fontSize);
            row.setColor(content.color);
            row.setText(content.text);
            row.setPosition(new Vector2f(x + PADDING, inputTop - PADDING - (rows.size() - i) * lineHeight));
        }

        builtVersion = log.version(shownLevel);
        builtLevel = shownLevel;
        builtScroll = scrollOffset;
        builtWidth = width;
        builtHeight = height;
        builtX = x;
        builtY = y;
        builtFontSize = fontSize;
    }

    private static final class RowText {
        final String text;
        final Color4f color;

        RowText(String text, Color4f color) {
            this.text = text;
            this.color = color;
        }
    }

    /*
     * The rows to show, oldest first, the scroll clamped. Every log line gives at least one row, so the last
     * (scroll + visible) lines are enough: the work grows with what is shown, not with the size of the log.
     */
    private List<RowText> lastRows(float textWidth) {
        int needed = scrollOffset + visibleRows;
        List<ConsoleLog.Line> lines = log.last(needed, shownLevel);

        // Rows from the newest, reversed at the end
        List<RowText> fromNewest = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 0 && fromNewest.size() < needed; --i) {
            ConsoleLog.Line line = lines.get(i);
            List<String> parts = wrap(line.toString().replace("\t", "    "), textWidth);
            Color4f color = colorOf(line.getType());
            for (int p = parts.size() - 1; p >= 0; --p)
                fromNewest.add(new RowText(parts.get(p), color));
        }

        // Scrolled past the oldest line: show the oldest rows
        scrollOffset = Math.max(0, Math.min(scrollOffset, fromNewest.size() - visibleRows));

        List<RowText> result = new ArrayList<>();
        int end = Math.min(fromNewest.size(), scrollOffset + visibleRows);
        for (int i = end - 1; i >= scrollOffset; --i)
            result.add(fromNewest.get(i));

        return result;
    }

    // Cut a line on the characters that pass the width (at least one character per row)
    private List<String> wrap(String line, float width) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        float used = 0;
        for (int i = 0; i < line.length(); ++i) {
            float advance = inputText.charAdvance(line.charAt(i));
            if (used + advance > width && i > start) {
                parts.add(line.substring(start, i));
                start = i;
                used = 0;
            }
            used += advance;
        }

        parts.add(line.substring(start));
        return parts;
    }

    private static Color4f colorOf(ConsoleLog.EType type) {
        switch (type) {
            case ERR:
            case ERROR:
                return ERR_COLOR;
            case COMMAND:
                return COMMAND_COLOR;
            case RESULT:
            case MESSAGE:
                return RESULT_COLOR;
            default:
                return OUT_COLOR;
        }
    }

    // The part of the command line around the cursor that fits, then the cursor
    private void displayInput(float x, float top, float width, boolean showCursor) {
        String text = editor.getText();
        int cursorIndex = editor.getCursor();
        float promptWidth = inputText.measureWidth(PROMPT);
        float available = Math.max(1, width - promptWidth - 2);

        if (inputScroll > cursorIndex)
            inputScroll = cursorIndex;
        if (inputScroll > text.length())
            inputScroll = text.length();
        while (inputScroll < cursorIndex && inputText.measureWidth(text.subSequence(inputScroll, cursorIndex)) > available)
            ++inputScroll;

        int end = inputScroll;
        float used = 0;
        while (end < text.length()) {
            float advance = inputText.charAdvance(text.charAt(end));
            if (used + advance > available)
                break;
            used += advance;
            ++end;
        }

        inputText.setText(PROMPT + text.substring(inputScroll, end));
        inputText.setPosition(new Vector2f(x, top));
        inputText.display();

        boolean blinkOn = (System.nanoTime() / CURSOR_BLINK_NANOSECONDS) % 2 == 0;
        if (showCursor && blinkOn) {
            float cursorX = x + promptWidth + inputText.measureWidth(text.subSequence(inputScroll, cursorIndex));
            cursor.setSizePix(2, lineHeight);
            cursor.setPosition(new Vector2f(cursorX, top));
            cursor.display();
        }
    }

    void unload() {
        background.unload();
        inputBackground.unload();
        cursor.unload();
        inputText.unload();
        for (Text row : rows)
            row.unload();
        rows.clear();
    }
}
