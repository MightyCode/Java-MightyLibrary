package io.github.mightycode.mightylib.graphics.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// One unit of width per character
class TextWrapTest {
    private static final TextWrap.ILineWidth CHARACTERS = CharSequence::length;

    private static String wrap(String text, float maxWidth) {
        return TextWrap.Wrap(text, maxWidth, CHARACTERS);
    }

    // The characters of a Text are the ones that are not line feeds
    private static String withoutBreaks(String text) {
        return text.replace("\n", "");
    }

    @Test
    void breaksAfterTheSpaceThatEndsAWord() {
        assertEquals("aaa \nbbb \nccc", wrap("aaa bbb ccc", 5));
    }

    @Test
    void keepsWhatFitsOnOneLine() {
        assertEquals("aa bb cc", wrap("aa bb cc", 8));
        assertEquals("aa bb \ncc", wrap("aa bb cc", 7));
    }

    @Test
    void trailingSpacesDoNotCountInTheWidth() {
        // "abcde" fits in 5, its space goes with it
        assertEquals("abcde \nfgh", wrap("abcde fgh", 5));
    }

    @Test
    void cutsAWordLongerThanALine() {
        assertEquals("abc\ndef\nghi\nj", wrap("abcdefghij", 3));
        assertEquals("ab \ncde\nfgh", wrap("ab cdefgh", 3));
    }

    @Test
    void keepsAtLeastOneCharacterPerLine() {
        assertEquals("a\nb\nc", wrap("abc", 0.5f));
    }

    @Test
    void keepsExplicitLineFeeds() {
        assertEquals("aa\nbb \ncc", wrap("aa\nbb cc", 4));
        assertEquals("a\n\nb\n", wrap("a\n\nb\n", 10));
    }

    @Test
    void wrapsEachExplicitLineAlone() {
        assertEquals("aaa \nbbb\nccc \nddd", wrap("aaa bbb\nccc ddd", 4));
    }

    @Test
    void noWidthMeansNoWrapping() {
        assertEquals("aaa bbb ccc", wrap("aaa bbb ccc", 0));
        assertEquals("aaa bbb ccc", wrap("aaa bbb ccc", -1));
    }

    @Test
    void emptyTextStaysEmpty() {
        assertEquals("", wrap("", 5));
        assertEquals("", wrap(null, 5));
    }

    @Test
    void keepsLeadingAndSeveralSpaces() {
        assertEquals("  aa  bb", wrap("  aa  bb", 20));
        assertEquals("  aa  \nbb", wrap("  aa  bb", 6));
    }

    @Test
    void charactersKeepTheirIndexOnceTheBreaksAreIgnored() {
        String[] texts = {"The quick brown fox jumps over the lazy dog", "supercalifragilisticexpialidocious", "a b  c   d", "x\ny z"};
        for (String text : texts) {
            for (float width : new float[]{1, 3, 7, 12}) {
                assertEquals(withoutBreaks(text), withoutBreaks(wrap(text, width)), text + " at " + width);
            }
        }
    }

    @Test
    void noLineIsWiderThanTheMaximumButOnesMadeOfSpacesAtTheEnd() {
        String wrapped = wrap("The quick brown fox jumps over the lazy dog", 10);
        for (String line : wrapped.split("\n"))
            assertTrue(line.stripTrailing().length() <= 10, "'" + line + "'");
    }

    @Test
    void theSpacesAtTheEndOfALineAreNotMeasured() {
        assertEquals(3, TextWrap.TrimmedLength("abc  "));
        assertEquals(5, TextWrap.TrimmedLength("  abc"));
        assertEquals(0, TextWrap.TrimmedLength("   "));
        assertEquals(0, TextWrap.TrimmedLength(""));
        // The space kept before a break of the wrapping
        assertEquals(5, TextWrap.TrimmedLength(wrap("quick brown", 6).split("\n")[0]));
    }
}
