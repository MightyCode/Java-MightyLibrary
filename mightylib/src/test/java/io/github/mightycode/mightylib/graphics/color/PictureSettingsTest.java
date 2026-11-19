package io.github.mightycode.mightylib.graphics.color;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.resources.EngineResources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Settings of the final pass: bounds, defaults of the service, overrides of a view
class PictureSettingsTest {
    @Test
    void defaultChangesNothing() {
        assertEquals(1f, PictureSettings.DEFAULT.gamma());
        assertEquals(1f, PictureSettings.DEFAULT.brightness());
        assertEquals(1f, PictureSettings.DEFAULT.contrast());
        assertEquals(PictureSettings.DEFAULT, PictureSettings.of(1f, 1f, 1f));
    }

    @Test
    void boundsAreChecked() {
        PictureSettings.of(PictureSettings.MIN, PictureSettings.MAX, 1f);
        assertThrows(IllegalArgumentException.class, () -> PictureSettings.of(0.2f, 1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> PictureSettings.of(1f, 4.5f, 1f));
        assertThrows(IllegalArgumentException.class, () -> PictureSettings.of(1f, 1f, 0f));
        assertThrows(IllegalArgumentException.class, () -> PictureSettings.of(Float.NaN, 1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> PictureSettings.DEFAULT.withGamma(5f));
    }

    @Test
    void withChangesOneValue() {
        PictureSettings settings = PictureSettings.DEFAULT.withGamma(1.5f).withBrightness(0.8f).withContrast(1.2f);
        assertEquals(PictureSettings.of(1.5f, 0.8f, 1.2f), settings);
        assertEquals(1f, PictureSettings.DEFAULT.gamma());
        assertEquals("gamma 1.50, brightness 0.80, contrast 1.20", settings.toString());
    }

    @Test
    void serviceDefaultsAndViewOverrides() {
        MainThreadTestAccess.setCurrentAsMain();
        ColorService service = new ColorService(ERenderColorSpace.GAMMA);
        assertEquals(EngineResources.POST_PROCESSING, service.finalPassShader());
        assertSame(PictureSettings.DEFAULT, service.picture());

        PictureSettings view = PictureSettings.of(2f, 1f, 1f);
        assertSame(view, service.resolvePicture(view));
        assertSame(PictureSettings.DEFAULT, service.resolvePicture(null));
        assertEquals("mine", service.resolveFinalPassShader("mine"));
        assertEquals(EngineResources.POST_PROCESSING, service.resolveFinalPassShader(null));

        service.setPicture(view);
        service.setFinalPassShader("other");
        assertSame(view, service.resolvePicture(null));
        assertEquals("other", service.resolveFinalPassShader(null));

        assertThrows(IllegalArgumentException.class, () -> service.setPicture(null));
        assertThrows(IllegalArgumentException.class, () -> service.setFinalPassShader(""));
        assertThrows(IllegalArgumentException.class, () -> service.setFinalPassShader(null));
    }
}
