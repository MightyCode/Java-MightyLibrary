package io.github.mightycode.mightylib.resources.animation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.mightycode.mightylib.resources.animation.AnimationResources.*;
import static org.junit.jupiter.api.Assertions.*;

// The animations of an Animator without engine: current animation, names, changes, speeds
class AnimatorTest {
    @TempDir
    Path folder;

    private AnimationResources resources;
    private Animator animator;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        resources = AnimationResources.create(folder).load();
        animator = new Animator(resources.resources());
    }

    private void addBoth() {
        animator.addAndInitAnimation(WALK, resources.data(WALK), false);
        animator.addAndInitAnimation(RUN, resources.data(RUN), true);
    }

    @Test
    void anEmptyAnimatorHasNoAnimation() {
        animator.update(1);
        animator.lateUpdate();

        assertNull(animator.getCurrentAnimation());
        assertNull(animator.getCurrentName());
        assertFalse(animator.isCurrentAnimationFinished());
        assertFalse(animator.isFrameChanged());
        assertEquals(0f, animator.getCurrentAnimationSpeed());
        animator.setCurrentAnimationSpeed(2);
        assertEquals(0f, animator.getCurrentAnimationSpeed());
    }

    @Test
    void theFirstAnimationIsTheCurrentOne() {
        addBoth();
        assertEquals(WALK, animator.getCurrentName());
        assertSame(resources.data(WALK), animator.getCurrentAnimation().getData());
        assertTrue(animator.animationChanged());
    }

    @Test
    void aNameAddedTwiceKeepsTheFirstAnimation() {
        animator.addAndInitAnimation(WALK, resources.data(WALK), false);
        Animation first = animator.getCurrentAnimation();

        animator.addAndInitAnimation(WALK, resources.data(RUN), true);
        animator.setCurrentAnimation(WALK, false);
        assertSame(first, animator.getCurrentAnimation());
        assertSame(resources.data(WALK), animator.getCurrentAnimation().getData());
    }

    @Test
    void onlyTheCurrentAnimationMoves() {
        addBoth();
        Animation walk = animator.getCurrentAnimation();
        animator.update(FIRST);
        assertEquals(1, walk.getCurrentFrame());

        animator.setCurrentAnimation(RUN, false);
        Animation run = animator.getCurrentAnimation();
        animator.update(SECOND);
        assertEquals(1, run.getCurrentFrame());
        assertEquals(1, walk.getCurrentFrame());
    }

    @Test
    void changingWithResetRestartsTheOldAnimation() {
        addBoth();
        Animation walk = animator.getCurrentAnimation();
        animator.update(FIRST);

        animator.setCurrentAnimation(RUN, true);
        assertEquals(RUN, animator.getCurrentName());
        assertEquals(0, walk.getCurrentFrame());
    }

    @Test
    void changingWithoutResetKeepsTheOldAnimationWhereItWas() {
        addBoth();
        Animation walk = animator.getCurrentAnimation();
        animator.update(FIRST);

        animator.setCurrentAnimation(RUN, false);
        animator.setCurrentAnimation(WALK, false);
        assertSame(walk, animator.getCurrentAnimation());
        assertEquals(1, walk.getCurrentFrame());
    }

    @Test
    void anUnknownNameChangesNothing() {
        addBoth();
        animator.lateUpdate();
        Animation walk = animator.getCurrentAnimation();

        animator.setCurrentAnimation("jump", true);
        assertSame(walk, animator.getCurrentAnimation());
        assertEquals(WALK, animator.getCurrentName());
        assertFalse(animator.animationChanged());
    }

    @Test
    void aChangeOfAnimationIsSeenUntilTheLateUpdate() {
        addBoth();
        animator.lateUpdate();
        assertFalse(animator.animationChanged());

        animator.setCurrentAnimation(RUN, false);
        assertTrue(animator.animationChanged());
        animator.lateUpdate();
        assertFalse(animator.animationChanged());
    }

    @Test
    void theEndOfTheCurrentAnimationIsSeen() {
        addBoth();
        animator.update(WALK_TIME);
        assertTrue(animator.isCurrentAnimationFinished());

        // A loop never ends
        animator.setCurrentAnimation(RUN, false);
        animator.update(10);
        assertFalse(animator.isCurrentAnimationFinished());
    }

    @Test
    void speedsOfTheCurrentOrOfAllAnimations() {
        addBoth();
        animator.setCurrentAnimationSpeed(2);
        assertEquals(2f, animator.getCurrentAnimationSpeed());
        animator.update(FIRST / 2);
        assertEquals(1, animator.getCurrentAnimation().getCurrentFrame());

        animator.setAllAnimationSpeed(3);
        assertEquals(3f, animator.getCurrentAnimationSpeed());
        animator.setCurrentAnimation(RUN, false);
        assertEquals(3f, animator.getCurrentAnimationSpeed());
    }

    @Test
    void aFrameChangeIsSeenOnlyUntilTheLateUpdate() {
        addBoth();
        // The first frame, seen once
        assertTrue(animator.isFrameChanged());
        animator.lateUpdate();
        assertFalse(animator.isFrameChanged());

        animator.update(FIRST / 2);
        assertFalse(animator.isFrameChanged());
        animator.update(FIRST / 2);
        assertTrue(animator.isFrameChanged());
        animator.lateUpdate();
        assertFalse(animator.isFrameChanged());

        // Back to the first frame of a loop: a change
        animator.setCurrentAnimation(RUN, false);
        animator.update(SECOND);
        animator.lateUpdate();
        animator.update(SECOND);
        assertEquals(0, animator.getCurrentAnimation().getCurrentFrame());
        assertTrue(animator.isFrameChanged());
    }

    @Test
    void clearRemovesTheAnimations() {
        addBoth();
        animator.clear();

        assertNull(animator.getCurrentAnimation());
        assertNull(animator.getCurrentName());
        animator.setCurrentAnimation(WALK, false);
        assertNull(animator.getCurrentAnimation());
    }
}
