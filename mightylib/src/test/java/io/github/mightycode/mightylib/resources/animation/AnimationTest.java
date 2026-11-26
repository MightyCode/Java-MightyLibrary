package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.main.utils.GameTimeControl;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.mightycode.mightylib.resources.animation.AnimationResources.*;
import static org.junit.jupiter.api.Assertions.*;

// The player of an animation without engine: frames, speed, loop, end, setFrame, fallback while loading
class AnimationTest {
    @TempDir
    Path folder;

    private AnimationResources resources;

    @BeforeEach
    void setUp() throws IOException {
        resources = AnimationResources.create(folder);
    }

    private Animation walk(boolean looping) throws InterruptedException {
        return resources.load().animation(WALK, looping);
    }

    @Test
    void framesFollowOneAnother() throws InterruptedException {
        Animation animation = walk(false);
        assertEquals(0, animation.getCurrentFrame());

        animation.update(FIRST / 2);
        assertEquals(0, animation.getCurrentFrame());
        animation.update(FIRST / 2);
        assertEquals(1, animation.getCurrentFrame());
        animation.update(SECOND / 2);
        assertEquals(1, animation.getCurrentFrame());
        animation.update(SECOND / 2);
        assertEquals(2, animation.getCurrentFrame());
        assertFalse(animation.isAnimationFinished());
    }

    @Test
    void oneUpdateCoversSeveralFrames() throws InterruptedException {
        Animation animation = walk(false);
        animation.update(FIRST + SECOND);
        assertEquals(2, animation.getCurrentFrame());
    }

    @Test
    void theSpeedMultipliesTheTime() throws InterruptedException {
        Animation animation = walk(false);
        animation.setAnimationSpeed(2);
        assertEquals(2, animation.getAnimationSpeed());

        animation.update(FIRST / 2);
        assertEquals(1, animation.getCurrentFrame());
    }

    @Test
    void aLoopGoesBackToTheFirstFrameAndNeverEnds() throws InterruptedException {
        Animation animation = walk(true);
        animation.update(WALK_TIME);
        assertEquals(0, animation.getCurrentFrame());
        assertFalse(animation.isAnimationFinished());

        animation.update(FIRST);
        assertEquals(1, animation.getCurrentFrame());
        animation.update(SECOND + THIRD);
        assertEquals(0, animation.getCurrentFrame());
        animation.update(100 * WALK_TIME);
        assertFalse(animation.isAnimationFinished());
    }

    @Test
    void theTimePastTheEndOfALoopGoesOnInTheNextOne() throws InterruptedException {
        Animation animation = walk(true);
        // Three loops and the first frame in one update
        animation.update(3 * WALK_TIME + FIRST);
        assertEquals(1, animation.getCurrentFrame());

        // Past the end by half the first frame: halfway through it
        animation.update(SECOND + THIRD + FIRST / 2);
        assertEquals(0, animation.getCurrentFrame());
        animation.update(FIRST / 2);
        assertEquals(1, animation.getCurrentFrame());
    }

    @Test
    void withoutLoopItEndsOnTheLastFrame() throws InterruptedException {
        Animation animation = walk(false);
        animation.update(FIRST + SECOND + THIRD / 2);
        assertEquals(2, animation.getCurrentFrame());
        assertFalse(animation.isAnimationFinished());

        animation.update(THIRD / 2);
        assertEquals(2, animation.getCurrentFrame());
        assertTrue(animation.isAnimationFinished());

        // Nothing moves any more
        animation.update(10 * WALK_TIME);
        assertEquals(2, animation.getCurrentFrame());
        assertTrue(animation.isAnimationFinished());
    }

    @Test
    void anUpdateLongerThanTheAnimationEndsIt() throws InterruptedException {
        Animation animation = walk(false);
        animation.update(10 * WALK_TIME);
        assertEquals(2, animation.getCurrentFrame());
        assertTrue(animation.isAnimationFinished());
    }

    @Test
    void restartGoesBackToTheStart() throws InterruptedException {
        Animation animation = walk(false);
        animation.update(WALK_TIME);
        animation.lateUpdate();
        assertFalse(animation.isCurrentFrameChanged());

        animation.restart();
        assertEquals(0, animation.getCurrentFrame());
        assertFalse(animation.isAnimationFinished());
        assertTrue(animation.isCurrentFrameChanged());
        // From the start of the first frame
        animation.update(FIRST / 2);
        assertEquals(0, animation.getCurrentFrame());
    }

    @Test
    void setFrameStartsThatFrame() throws InterruptedException {
        Animation animation = walk(false);
        animation.update(FIRST / 2);

        animation.setFrame(1);
        assertEquals(1, animation.getCurrentFrame());
        // From its start: the time of the first frame is gone
        animation.update(SECOND - FIRST / 4);
        assertEquals(1, animation.getCurrentFrame());
        animation.update(FIRST / 4);
        assertEquals(2, animation.getCurrentFrame());
    }

    @Test
    void setFrameAfterTheEndPlaysAgain() throws InterruptedException {
        Animation animation = walk(false);
        animation.update(WALK_TIME);
        assertTrue(animation.isAnimationFinished());

        animation.setFrame(0);
        assertFalse(animation.isAnimationFinished());
        animation.update(FIRST);
        assertEquals(1, animation.getCurrentFrame());
    }

    @Test
    void setFrameOutsideTheFramesIsRefused() throws InterruptedException {
        Animation animation = walk(false);
        assertThrows(IndexOutOfBoundsException.class, () -> animation.setFrame(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> animation.setFrame(3));
        assertEquals(0, animation.getCurrentFrame());
    }

    @Test
    void theFallbackPlaysUntilTheDataIsLoaded() throws InterruptedException {
        // Not loaded yet: the single frame of the fallback (1 s)
        Animation animation = new Animation(resources.resources());
        AnimationData walk = resources.data(WALK);
        animation.init(walk, false);
        assertFalse(walk.isLoaded());
        assertTrue(resources.resources().isFallback(animation.getData()));
        assertEquals(1, animation.getData().frameNumber());
        animation.update(FIRST);
        assertEquals(0, animation.getCurrentFrame());

        resources.load();

        // The loaded data restarts the animation, then this update moves it
        animation.update(FIRST);
        assertSame(walk, animation.getData());
        assertEquals(1, animation.getCurrentFrame());
    }

    @Test
    void anUnknownAnimationPlaysTheFallback() throws InterruptedException {
        resources.load();
        Animation animation = new Animation(resources.resources());
        animation.init(null, true);

        assertTrue(resources.resources().isFallback(animation.getData()));
        animation.update(10);
        assertEquals(0, animation.getCurrentFrame());
        assertFalse(animation.isAnimationFinished());
    }

    @Test
    void updateWithoutTimeReadsTheTimeOfTheTick() throws InterruptedException {
        Animation animation = walk(false);
        GameTimeControl.setDeltaTime(FIRST);
        try {
            animation.update();
        } finally {
            GameTimeControl.reset();
        }
        assertEquals(1, animation.getCurrentFrame());
    }

    @Test
    void theFrameIsReadInTheSheetOfTheGivenResources() throws InterruptedException {
        Animation animation = walk(false);
        animation.update(FIRST + SECOND);

        // Frame 16,0,8,16 of a sheet of 32 x 16, half a texel inside each side
        Vector4f position = animation.currentTexturePosition();
        assertEquals(16.5f / SHEET_WIDTH, position.x, 1e-6f);
        assertEquals(23.5f / SHEET_WIDTH, position.y, 1e-6f);
        assertEquals(0.5f / SHEET_HEIGHT, position.z, 1e-6f);
        assertEquals(15.5f / SHEET_HEIGHT, position.w, 1e-6f);

        assertEquals(new Vector2i(8, 16), animation.getFrameSize());
        assertEquals(new Vector2i(5, 6), animation.currentHotPoint());
        assertEquals(new Vector2i(3, 10), animation.currentHotPoint(true, true));
    }
}
