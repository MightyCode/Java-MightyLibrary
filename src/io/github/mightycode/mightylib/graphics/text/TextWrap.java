package io.github.mightycode.mightylib.graphics.text;

/**
 * Word wrapping without GL: the width of a line is given by the caller (Text.measureWidth in the game).
 *
 * The breaks are line feeds added after the space that ends a word, so the characters keep their index (the line
 * feeds do not count as characters in a Text: cursors and selections of the original text still match). A word
 * longer than a line is cut where the line is full, one character at least per line. The explicit line feeds of the
 * text are kept.
 */
public final class TextWrap {
    /** Width of one line, without line feed, in the unit of the maximum width. */
    public interface ILineWidth {
        float of(CharSequence line);
    }

    private TextWrap() {}

    /**
     * @param maxWidth the widest a line may be, spaces at its end not counted; 0 or less: no wrapping
     * @return the text with a line feed added at each break
     */
    public static String Wrap(String text, float maxWidth, ILineWidth width) {
        if (text == null || text.isEmpty() || maxWidth <= 0)
            return text == null ? "" : text;

        StringBuilder result = new StringBuilder(text.length() + 8);
        int lineStart = 0;
        while (true) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0)
                lineEnd = text.length();

            WrapLine(text, lineStart, lineEnd, maxWidth, width, result);
            if (lineEnd == text.length())
                break;

            result.append('\n');
            lineStart = lineEnd + 1;
        }

        return result.toString();
    }

    // One explicit line: text[from, to)
    private static void WrapLine(String text, int from, int to, float maxWidth, ILineWidth width, StringBuilder out) {
        int visualStart = from;
        int index = from;
        while (index < to) {
            int wordEnd = index;
            while (wordEnd < to && text.charAt(wordEnd) != ' ')
                ++wordEnd;
            int tokenEnd = wordEnd;
            while (tokenEnd < to && text.charAt(tokenEnd) == ' ')
                ++tokenEnd;

            if (width.of(text.subSequence(visualStart, wordEnd)) <= maxWidth) {
                index = tokenEnd;
            } else if (index > visualStart) {
                // The word starts a new line
                out.append(text, visualStart, index).append('\n');
                visualStart = index;
            } else {
                // Alone on its line and still too wide: cut where the line is full, one character at least
                int cut = index + 1;
                while (cut < wordEnd && width.of(text.subSequence(visualStart, cut + 1)) <= maxWidth)
                    ++cut;

                out.append(text, visualStart, cut);
                if (cut < to)
                    out.append('\n');
                visualStart = cut;
                index = cut;
            }
        }

        out.append(text, visualStart, to);
    }
}
