package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

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
    // Behind the selected part of the command line
    private final RectangleRenderer inputSelection;
    private final Text inputText;
    // "v N lines" at the right of the last row when the view is not at the end
    private final RectangleRenderer indicatorBackground;
    private final Text indicatorText;
    private String indicatorShown = "";
    private final List<Text> rows;
    // Behind the selected part of each row of the log, reused from a frame to the next (one per row at most)
    private final List<RectangleRenderer> highlights = new ArrayList<>();

    private float fontSize;
    private float lineHeight;

    // Lines cut to the width, rows shown and scroll
    private final ConsoleLayout layout;
    // First character of the command line shown, when it is wider than the panel
    private int inputScroll;
    private float originX;
    private float originY;

    // Level of the player (only the lines it sees are shown) and filter of the console
    private final Console console;

    // What the rows were built for
    private boolean rowsDirty = true;
    private float builtWidth = -1;
    private float builtHeight = -1;
    private float builtX = -1;
    private float builtY = -1;
    private float builtFontSize = -1;
    private int visibleRows;

    ConsolePanel(ConsoleView view, Console console, Color4f backgroundColor, Color4f inputBackgroundColor) {
        this.view = view;
        this.console = console;
        this.log = console.getLog();
        this.editor = console.getEditor();
        this.fontName = console.getFontName();

        background = new RectangleRenderer("colorShape2D");
        background.setColorMode(backgroundColor);
        inputBackground = new RectangleRenderer("colorShape2D");
        this.inputBackgroundColor = inputBackgroundColor;
        inputBackgroundValue = inputBackground.setColorMode(inputBackgroundColor);
        cursor = new RectangleRenderer("colorShape2D");
        cursor.setColorMode(new Color4f(1f, 1f, 1f, 0.85f));
        inputSelection = new RectangleRenderer("colorShape2D");
        inputSelection.setColorMode(ConsoleColors.SELECTION);

        indicatorBackground = new RectangleRenderer("colorShape2D");
        indicatorBackground.setColorMode(ConsoleColors.INDICATOR_BACKGROUND);
        indicatorText = createText();
        indicatorText.setColor(ConsoleColors.INDICATOR);
        inputText = createText();
        inputText.setColor(ConsoleColors.INPUT);
        rows = new ArrayList<>();
        // Measured with the font of the command line, at the size of the rows
        layout = new ConsoleLayout(log, inputText::charAdvance);
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

    ConsoleLayout getLayout() { return layout; }

    /** Toward the oldest lines (positive) or the newest (negative), by rows. */
    void scroll(int rows) {
        layout.scroll(rows);
    }

    void scrollToTop() {
        layout.scrollToTop();
    }

    void scrollToEnd() {
        layout.scrollToEnd();
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

        layout.setGeometry(width - 2 * PADDING, rowCount, lineHeight);
        layout.setViewer(console.getViewerLevel());
        layout.setHiddenTypes(console.getHiddenTypes());
        // A line hidden from the viewer (program output for the player) rebuilds nothing
        if (layout.refresh() || rowsDirty || width != builtWidth || height != builtHeight || x != builtX || y != builtY
                || fontSize != builtFontSize)
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

        displayLogSelection(x + PADDING, inputTop);
        for (Text row : rows)
            row.display();

        displayIndicator(x, inputTop);

        displayInput(x + PADDING, inputTop, width - 2 * PADDING, showCursor);

        // Where the mouse points to, read by the next ticks
        originX = x;
        originY = y;
        layout.setPanelGeometry(PADDING, rowTop(inputTop, 0) - y, PADDING + inputText.measureWidth(PROMPT), inputScroll);
    }

    // Top left of the panel in the window, in pixels (y downward), as last drawn
    float originX() { return originX; }

    float originY() { return originY; }

    // Font size giving rows of ROW_HEIGHT pixels with the metrics of the font used now
    private void updateFontSize() {
        // Height of a row for a font size of 1 (changing the size of a text rebuilds it: measured, not set)
        float unit = inputText.lineHeight() / inputText.getFontSize();
        float wanted = unit > 0 ? ROW_HEIGHT / unit : ROW_HEIGHT;
        if (wanted != fontSize) {
            fontSize = wanted;
            for (Text row : rows)
                row.setFontSize(fontSize);
            layout.invalidate();
        }

        inputText.setFontSize(fontSize);
        indicatorText.setFontSize(fontSize);
        lineHeight = Math.max(1, inputText.lineHeight());
    }

    private void resizeRows(int count) {
        while (rows.size() > count)
            rows.remove(rows.size() - 1).unload();
        while (rows.size() < count)
            rows.add(createText());

        visibleRows = count;
        rowsDirty = true;
    }

    // The rows of the layout, the newest at the bottom, just above the command line
    private void buildRows(float x, float y, float width, float height, float inputTop) {
        List<ConsoleLayout.Row> shown = layout.rows();
        for (int i = 0; i < rows.size(); ++i) {
            Text row = rows.get(i);
            int index = i - layout.slotOf(0);
            if (index < 0 || index >= shown.size()) {
                row.setText("");
                continue;
            }

            ConsoleLayout.Row content = shown.get(index);
            row.setFontSize(fontSize);
            row.setColor(ConsoleColors.of(content.line().getType()));
            row.setText(content.text());
            row.setPosition(new Vector2f(x + PADDING, rowTop(inputTop, i)));
        }

        rowsDirty = false;
        builtWidth = width;
        builtHeight = height;
        builtX = x;
        builtY = y;
        builtFontSize = fontSize;
    }

    // The selected part of each row shown, behind its text
    private void displayLogSelection(float textLeft, float inputTop) {
        LogSelection selection = console.getLogSelection();
        if (selection == null)
            return;

        LogPosition start = selection.start();
        LogPosition end = selection.end();
        List<ConsoleLayout.Row> shown = layout.rows();
        int used = 0;
        for (int i = 0; i < shown.size(); ++i) {
            ConsoleLayout.Row row = shown.get(i);
            long id = row.line().getId();
            if (id < start.lineId() || id > end.lineId())
                continue;

            int from = id == start.lineId() ? Math.max(row.start(), start.character()) : row.start();
            int to = id == end.lineId() ? Math.min(row.end(), end.character()) : row.end();
            float left = layout.xOf(row, from);
            float right = layout.xOf(row, to);
            // The end of a line inside the selection: a little more, an empty line is seen selected too
            if (row.isLastPart() && id != end.lineId())
                right += ConsoleColors.SELECTION_LINE_END;
            if (right <= left)
                continue;

            if (used == highlights.size()) {
                RectangleRenderer highlight = new RectangleRenderer("colorShape2D");
                highlight.setColorMode(ConsoleColors.SELECTION);
                highlights.add(highlight);
            }
            RectangleRenderer highlight = highlights.get(used++);
            highlight.setSizePix(right - left, lineHeight);
            highlight.setPosition(new Vector2f(textLeft + left, rowTop(inputTop, layout.slotOf(i))));
            highlight.display();
        }
    }

    // "v N lines" over the right of the last row, when lines are below the view
    private void displayIndicator(float x, float inputTop) {
        String text = layout.linesBelowText();
        if (text == null || rows.isEmpty())
            return;

        if (!text.equals(indicatorShown)) {
            indicatorText.setText(text);
            indicatorShown = text;
        }

        float top = rowTop(inputTop, rows.size() - 1);
        float left = x + layout.indicatorLeft();
        indicatorBackground.setSizePix(layout.indicatorWidth(), lineHeight);
        indicatorBackground.setPosition(new Vector2f(left, top));
        indicatorBackground.display();
        indicatorText.setPosition(new Vector2f(left + ConsoleLayout.INDICATOR_PADDING, top));
        indicatorText.display();
    }

    // Top of a slot of the panel (0: the top row)
    private float rowTop(float inputTop, int slot) {
        return inputTop - PADDING - (rows.size() - slot) * lineHeight;
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

        // The selection behind the text, the part of it that is shown
        int selectionStart = Math.max(inputScroll, editor.selectionStart());
        int selectionEnd = Math.min(end, editor.selectionEnd());
        if (editor.hasSelection() && selectionEnd > selectionStart) {
            float left = x + promptWidth + inputText.measureWidth(text.subSequence(inputScroll, selectionStart));
            inputSelection.setSizePix(inputText.measureWidth(text.subSequence(selectionStart, selectionEnd)), lineHeight);
            inputSelection.setPosition(new Vector2f(left, top));
            inputSelection.display();
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
        inputSelection.unload();
        indicatorBackground.unload();
        indicatorText.unload();
        inputText.unload();
        for (Text row : rows)
            row.unload();
        rows.clear();
        for (RectangleRenderer highlight : highlights)
            highlight.unload();
        highlights.clear();
    }
}
