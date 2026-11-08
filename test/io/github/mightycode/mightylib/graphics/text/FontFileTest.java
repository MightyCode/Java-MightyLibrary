package io.github.mightycode.mightylib.graphics.text;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontFileTest {
    // A failure of the data: the preload fails on one line and the fallback font replaces it
    @Test
    void aMissingFileIsAResourceFailure() {
        FontFile file = new FontFile("resources/fonts/missing.fnt");
        ResourceException error = assertThrows(ResourceException.class, file::load);
        assertTrue(error.getMessage().contains("missing.fnt"));
    }
}
