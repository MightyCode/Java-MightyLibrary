package io.github.mightycode.mightylib.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContextManagerTest {
    @Test
    void theMainWindowIsRequiredOnlyOnceTheEngineIsStarted() {
        ContextManager contexts = new ContextManager();

        assertNull(contexts.getMainWindow());
        assertThrows(IllegalStateException.class, contexts::requireMainWindow);
    }
}
