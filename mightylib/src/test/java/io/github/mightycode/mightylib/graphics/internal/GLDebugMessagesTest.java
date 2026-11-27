package io.github.mightycode.mightylib.graphics.internal;

import io.github.mightycode.mightylib.utils.ELogLevel;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL43;

import static org.junit.jupiter.api.Assertions.*;

// Level of the OpenGL driver's messages in the problem channel
class GLDebugMessagesTest {
    @Test
    void errorTypeOrHighSeverityIsAnError() {
        assertEquals(ELogLevel.ERROR, GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_ERROR, GL43.GL_DEBUG_SEVERITY_LOW));
        assertEquals(ELogLevel.ERROR, GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_ERROR, GL43.GL_DEBUG_SEVERITY_NOTIFICATION));
        assertEquals(ELogLevel.ERROR, GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_OTHER, GL43.GL_DEBUG_SEVERITY_HIGH));
        assertEquals(ELogLevel.ERROR, GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_PERFORMANCE, GL43.GL_DEBUG_SEVERITY_HIGH));
    }

    @Test
    void mediumLowAndPerformanceAreWarnings() {
        assertEquals(ELogLevel.WARNING, GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_OTHER, GL43.GL_DEBUG_SEVERITY_MEDIUM));
        assertEquals(ELogLevel.WARNING, GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_PORTABILITY, GL43.GL_DEBUG_SEVERITY_LOW));
        assertEquals(ELogLevel.WARNING, GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_PERFORMANCE, GL43.GL_DEBUG_SEVERITY_NOTIFICATION));
    }

    @Test
    void notificationsAreIgnored() {
        assertNull(GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_OTHER, GL43.GL_DEBUG_SEVERITY_NOTIFICATION));
        assertNull(GLContext.DebugMessageLevel(GL43.GL_DEBUG_TYPE_MARKER, GL43.GL_DEBUG_SEVERITY_NOTIFICATION));
    }

    @Test
    void arbConstantsHaveTheSameLevels() {
        assertEquals(ELogLevel.ERROR, GLContext.DebugMessageLevel(ARBDebugOutput.GL_DEBUG_TYPE_ERROR_ARB, ARBDebugOutput.GL_DEBUG_SEVERITY_LOW_ARB));
        assertEquals(ELogLevel.ERROR, GLContext.DebugMessageLevel(ARBDebugOutput.GL_DEBUG_TYPE_OTHER_ARB, ARBDebugOutput.GL_DEBUG_SEVERITY_HIGH_ARB));
        assertEquals(ELogLevel.WARNING, GLContext.DebugMessageLevel(ARBDebugOutput.GL_DEBUG_TYPE_OTHER_ARB, ARBDebugOutput.GL_DEBUG_SEVERITY_MEDIUM_ARB));
    }

    @Test
    void readableNames() {
        assertEquals("SHADER COMPILER", GLContext.DebugSourceName(GL43.GL_DEBUG_SOURCE_SHADER_COMPILER));
        assertEquals("UNDEFINED BEHAVIOR", GLContext.DebugTypeName(GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR));
        assertEquals("MEDIUM", GLContext.DebugSeverityName(GL43.GL_DEBUG_SEVERITY_MEDIUM));
        assertEquals("0x1", GLContext.DebugSeverityName(1));
    }
}
