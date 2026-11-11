package io.github.mightycode.mightylib.physics.tweenings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// fromString is an instance method of the enums (any constant can be used to call it)
class TweeningEnumsTest {
    @Test
    void typesAreParsedWithoutCaseNorSpaces() {
        ETweeningType any = ETweeningType.Linear;
        for (ETweeningType type : ETweeningType.values()) {
            assertSame(type, any.fromString(type.name()), type.toString());
            assertSame(type, any.fromString("  " + type.name().toUpperCase() + " "), type.toString());
            assertSame(type, any.fromString(type.name().toLowerCase()), type.toString());
        }
    }

    @Test
    void unknownNamesGiveNull() {
        assertNull(ETweeningType.Linear.fromString("wobbly"));
        assertNull(ETweeningType.Linear.fromString(""));
        assertNull(ETweeningBehaviour.In.fromString("sideways"));
        assertNull(ETweeningOption.Direct.fromString("never"));
    }

    @Test
    void behaviourIn() {
        assertSame(ETweeningBehaviour.In, ETweeningBehaviour.In.fromString(" IN "));
    }

    @Test
    void behavioursAreParsed() {
        for (ETweeningBehaviour behaviour : ETweeningBehaviour.values())
            assertSame(behaviour, ETweeningBehaviour.In.fromString(behaviour.name()), behaviour.toString());
    }

    @Test
    void directOptionsAreParsed() {
        ETweeningOption any = ETweeningOption.Direct;
        assertSame(ETweeningOption.Direct, any.fromString("direct"));
        assertSame(ETweeningOption.DirectMirrored, any.fromString("DirectMirrored"));
        assertSame(ETweeningOption.DirectReversed, any.fromString(" directreversed"));
    }

    @Test
    void loopOptionsAreParsed() {
        ETweeningOption any = ETweeningOption.Direct;
        assertSame(ETweeningOption.Loop, any.fromString("loop"));
        assertSame(ETweeningOption.LoopMirrored, any.fromString("loopmirrored"));
        assertSame(ETweeningOption.LoopReversed, any.fromString("loopreversed"));
    }
}
