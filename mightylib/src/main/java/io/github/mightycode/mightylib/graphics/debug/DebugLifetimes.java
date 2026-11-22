package io.github.mightycode.mightylib.graphics.debug;

import io.github.mightycode.mightylib.main.utils.GameTime;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/** The lifetimes of the drawings of DebugDraw2D (IDebugLifetime). */
public final class DebugLifetimes {
    // Shared: the drawing is drawn in the frame it was asked, then dropped (no object per drawing)
    private static final IDebugLifetime ONE_FRAME = new IDebugLifetime() {
        @Override
        public boolean alive() {
            return true;
        }

        @Override
        public String name() {
            return "oneFrame";
        }
    };

    private static final IDebugLifetime UNTIL_REMOVED = new IDebugLifetime() {
        @Override
        public boolean alive() {
            return true;
        }

        @Override
        public String name() {
            return "untilRemoved";
        }
    };

    private DebugLifetimes() {}

    /** Drawn in the frame it is asked (the default): a drawing asked again at each frame follows what it shows. */
    public static IDebugLifetime oneFrame() {
        return ONE_FRAME;
    }

    // Only the shared oneFrame is dropped after its first frame
    static boolean isOneFrame(IDebugLifetime lifetime) {
        return lifetime == ONE_FRAME;
    }

    /**
     * Drawn in count frames, the one it is asked in included (at least one). One per drawing: it counts the frames
     * each time it is asked, so shared by two drawings it ends twice as fast (like seconds).
     */
    public static IDebugLifetime frames(int count) {
        return new Frames(count);
    }

    /**
     * Drawn while seconds of the time of the game (GameTime.DeltaTime) have not passed since it was asked. One per
     * drawing (it counts the time each time it is asked); oneFrame, untilRemoved and whileTrue can be shared.
     */
    public static IDebugLifetime seconds(float seconds) {
        return new Seconds(seconds, GameTime::DeltaTime);
    }

    // The same with another source of the time of a frame (tests)
    static IDebugLifetime seconds(float seconds, DoubleSupplier frameDuration) {
        return new Seconds(seconds, frameDuration);
    }

    /** Drawn while the condition is true (asked once per frame). */
    public static IDebugLifetime whileTrue(BooleanSupplier condition) {
        Objects.requireNonNull(condition, "condition");
        return new IDebugLifetime() {
            @Override
            public boolean alive() {
                return condition.getAsBoolean();
            }

            @Override
            public String name() {
                return "whileTrue";
            }
        };
    }

    /** Drawn until the handle given back by the drawing is removed (DebugDrawHandle.remove). */
    public static IDebugLifetime untilRemoved() {
        return UNTIL_REMOVED;
    }

    private static final class Frames implements IDebugLifetime {
        private int remaining;

        Frames(int count) {
            remaining = Math.max(count, 1);
        }

        @Override
        public boolean alive() {
            return remaining-- > 0;
        }

        @Override
        public String name() {
            return "frames";
        }
    }

    private static final class Seconds implements IDebugLifetime {
        private final DoubleSupplier frameDuration;
        private double remaining;
        // The first frame is the one it was asked in: its time is not counted
        private boolean started;

        Seconds(float seconds, DoubleSupplier frameDuration) {
            this.frameDuration = Objects.requireNonNull(frameDuration, "frameDuration");
            remaining = seconds;
        }

        @Override
        public boolean alive() {
            if (started)
                remaining -= frameDuration.getAsDouble();
            started = true;
            return remaining > 0;
        }

        @Override
        public String name() {
            return "seconds";
        }
    }
}
