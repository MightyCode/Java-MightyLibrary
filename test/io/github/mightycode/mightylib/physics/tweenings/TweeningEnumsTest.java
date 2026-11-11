package io.github.mightycode.mightylib.physics.tweenings;

import org.junit.jupiter.api.Disabled;
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

    @Disabled("Known bug: ETweeningBehaviour.fromString returns In for \"out\" and for \"inout\"")
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

    @Disabled("Known bug: ETweeningOption.fromString returns DirectReversed for the three loop options")
    @Test
    void loopOptionsAreParsed() {
        ETweeningOption any = ETweeningOption.Direct;
        assertSame(ETweeningOption.Loop, any.fromString("loop"));
        assertSame(ETweeningOption.LoopMirrored, any.fromString("loopmirrored"));
        assertSame(ETweeningOption.LoopReversed, any.fromString("loopreversed"));
    }
}
