package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.joml.Vector2i;
import org.joml.Vector4i;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Animation files and their frames (the Animation player needs the engine: not covered here)
class AnimationDataTest {
    @TempDir
    Path folder;

    private AnimationData load(String content) throws IOException {
        Path file = folder.resolve("walk.anim");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        AnimationData data = new AnimationData("walk", file.toString());
        new AnimationDataLoader().initWithFile(data);
        return data;
    }

    private static FrameData frame(String text) {
        FrameData frame = new FrameData();
        frame.init(text, "test", 1);
        return frame;
    }

    @Test
    void frameValuesAreReadInOrder() {
        FrameData frame = frame("16,32,8,24,4,5,0.25");

        assertEquals(new Vector4i(16, 32, 8, 24), frame.getFramePositionCopy());
        assertEquals(new Vector2i(8, 24), frame.getSize());
        assertEquals(new Vector2i(4, 5), frame.getHotPointCopy());
        assertEquals(0.25f, frame.getFrameTime());
    }

    @Test
    void aNewFrameIsEmpty() {
        FrameData frame = new FrameData();
        assertEquals(new Vector4i(0, 0, 0, 0), frame.getFramePositionCopy());
        assertEquals(new Vector2i(0, 0), frame.getHotPointCopy());
        assertEquals(0f, frame.getFrameTime());
    }

    @Test
    void copiesAreIndependentAndReferencesAreNot() {
        FrameData frame = frame("1,2,3,4,5,6,0.5");

        frame.getFramePositionCopy().set(9, 9, 9, 9);
        frame.getHotPointCopy().set(9, 9);
        assertEquals(new Vector4i(1, 2, 3, 4), frame.getFramePositionReference());
        assertEquals(new Vector2i(5, 6), frame.getHotPointReference());

        frame.getHotPointReference().set(7, 7);
        assertEquals(new Vector2i(7, 7), frame.getHotPointCopy());
    }

    @Test
    void aFrameNeedsExactlySevenValues() {
        for (String bad : new String[]{"", "1,2,3", "1,2,3,4,5,6", "1,2,3,4,5,6,0.5,9"}) {
            ResourceException error = assertThrows(ResourceException.class, () -> frame(bad), bad);
            assertTrue(error.getMessage().contains("test"), error.getMessage());
            assertTrue(error.getMessage().contains("line 1"), error.getMessage());
        }
    }

    @Test
    void aFrameValueThatIsNotANumberIsRefused() {
        assertThrows(ResourceException.class, () -> frame("a,2,3,4,5,6,0.5"));
        assertThrows(ResourceException.class, () -> frame("1,2,3,4,5,6,fast"));
    }

    @Test
    void animationFileGivesTheTextureAndTheFrames() throws IOException {
        AnimationData data = load("hero\n2\n0,0,16,16,8,15,0.1\n16,0,16,16,8,14,0.2\n");

        assertEquals("hero", data.getTextureName());
        assertEquals(2, data.frameNumber());
        assertEquals(new Vector4i(0, 0, 16, 16), data.getFrame(0).getFramePositionCopy());
        assertEquals(0.1f, data.getFrame(0).getFrameTime());
        assertEquals(new Vector2i(8, 14), data.getFrame(1).getHotPointCopy());
        assertEquals(new Vector4i(16, 0, 16, 16), data.getFrame(1).getFramePositionCopy());
    }

    @Test
    void windowsLineEndingsAndSpacesAreAccepted() throws IOException {
        AnimationData data = load("hero \r\n1\r\n 0,0,16,16,8,15,0.1 \r\n");

        assertEquals("hero", data.getTextureName());
        assertEquals(1, data.frameNumber());
    }

    @Test
    void anAnimationMayHaveNoFrame() throws IOException {
        AnimationData data = load("hero\n0\n");
        assertEquals(0, data.frameNumber());
    }

    @Test
    void aNegativeNumberOfFramesIsRefused() {
        assertThrows(ResourceException.class, () -> load("hero\n-1\n"));
    }

    @Test
    void extraLinesAfterTheFramesAreIgnored() throws IOException {
        AnimationData data = load("hero\n1\n0,0,1,1,0,0,0.5\nnot a frame\n");
        assertEquals(1, data.frameNumber());
    }

    @Test
    void aNewAnimationHasNoFrameAndTheErrorTexture() {
        AnimationData data = new AnimationData("walk", "walk.anim");
        assertEquals("error", data.getTextureName());
        assertNull(data.getFrame(0));
    }

    @Test
    void anIndexBeforeTheFirstFrameGivesNoFrame() throws IOException {
        assertNull(load("hero\n1\n0,0,1,1,0,0,0.5\n").getFrame(-1));
    }

    @Test
    void anIndexPastTheLastFrameGivesNoFrame() throws IOException {
        assertNull(load("hero\n1\n0,0,1,1,0,0,0.5\n").getFrame(1));
    }

    @Test
    void unloadingEmptiesTheAnimation() throws IOException {
        AnimationData data = load("hero\n1\n0,0,1,1,0,0,0.5\n");

        data.internUnload();

        assertEquals("error", data.getTextureName());
        assertNull(data.getFrame(0));
    }

    @Test
    void reloadExchangesTheContent() throws IOException {
        AnimationData current = load("hero\n1\n0,0,1,1,0,0,0.5\n");
        AnimationData newer = new AnimationData("walk", "walk.anim");
        newer.setTexture("villain");
        newer.setFramesData(new FrameData[]{frame("0,0,2,2,0,0,1"), frame("2,0,2,2,0,0,1")});

        current.swapContent(newer);

        assertEquals("villain", current.getTextureName());
        assertEquals(2, current.frameNumber());
        assertEquals("hero", newer.getTextureName());
        assertEquals(1, newer.frameNumber());
    }

    @Test
    void reloadCopyIsAnAnimationOfTheSameSource() {
        AnimationData copy = (AnimationData) new AnimationData("walk", "res/walk.anim").createReloadCopy();
        assertEquals("walk", copy.getDataName());
        assertEquals("res/walk.anim", copy.path());
    }

    @Test
    void notLoadableWithoutFrames() {
        // internLoad asks Resources for the texture only when there are frames
        assertFalse(new AnimationData("walk", "walk.anim").internLoad());
    }

    @Test
    void loaderRecognisesAnimationFiles() {
        AnimationDataLoader loader = new AnimationDataLoader();
        assertEquals("walk", loader.filterFile("res/anim/walk.anim"));
        assertNull(loader.filterFile("res/anim/walk.png"));
        assertEquals(AnimationData.class, loader.getType());
    }
}
