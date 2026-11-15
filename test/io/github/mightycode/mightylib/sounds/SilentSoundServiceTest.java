package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.TestFiles;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The sound without device: sources, their life, the gain tree
class SilentSoundServiceTest {
    private static final String GAIN_TREE = TestFiles.file("sounds/sounds.gaintree");

    private static SoundSourceCreationInfo info(String name, boolean owned) {
        SoundSourceCreationInfo info = new SoundSourceCreationInfo();
        info.name = name;
        info.owned = owned;
        info.gain = 0.5f;
        info.gainNode = "noise";
        info.position = new Vector3f(1, 2, 3);
        return info;
    }

    @Test
    void aSourceWaitsForTheUpdateThenKeepsItsSettings() {
        SilentSoundService service = new SilentSoundService(GAIN_TREE);
        ISoundSource source = service.createSoundSource(info("music", true));
        assertFalse(source.isReady());
        assertEquals("waiting", source.state());
        assertEquals(new Vector3f(), source.getPosition());

        service.lateUpdate();
        assertTrue(source.isReady());
        assertEquals("playing", source.state());
        assertEquals(new Vector3f(1, 2, 3), source.getPosition());
        // 0.5 * noise 0.7 * global 0.7
        assertEquals(0.5f * 0.7f * 0.7f, source.getFinalGain(), 1e-6);

        source.pause();
        assertTrue(source.inPause());
        source.stop();
        assertTrue(source.isStopped());
    }

    @Test
    void anOwnedSourceLivesUntilReleasedTheOthersEndAtOnce() {
        SilentSoundService service = new SilentSoundService(null);
        ISoundSource owned = service.createSoundSource(info("music", true));
        ISoundSource shot = service.createSoundSource(info("shot", false));

        service.lateUpdate();
        assertEquals(2, service.getSources().size());
        service.lateUpdate();
        assertEquals(1, service.getSources().size());
        assertSame(owned, service.getSources().get(0));
        assertTrue(shot.isPlaying());

        owned.release();
        assertEquals("released", owned.state());
        service.lateUpdate();
        assertTrue(service.getSources().isEmpty());
    }

    @Test
    void clearAwaitedSongForgetsTheWaitingSources() {
        SilentSoundService service = new SilentSoundService(null);
        ISoundSource playing = service.createSoundSource(info("music", true));
        service.lateUpdate();
        SoundSourceCreationInfo delayed = info("later", true);
        delayed.delay = 10;
        service.createSoundSource(delayed);

        service.clearAwaitedSong();
        assertEquals(1, service.getSources().size());
        assertSame(playing, service.getSources().get(0));
    }

    @Test
    void changeGainUpdatesTheSourcesAndRefusesAnUnknownNode() {
        SilentSoundService service = new SilentSoundService(GAIN_TREE);
        ISoundSource source = service.createSoundSource(info("music", true));
        service.lateUpdate();

        assertFalse(service.changeGain("absent", 0.5f));
        assertFalse(service.changeGain("noise", 0.7f));
        assertTrue(service.changeGain("global", 2f));
        assertEquals(1f, service.getNodeGain("global"));
        assertEquals(0.5f * 0.7f, source.getFinalGain(), 1e-6);
        assertEquals(1f, service.getNodeGain("absent"));
        assertEquals(3, service.getGainNodes().size());
    }

    @Test
    void buffersAreNamesOnlyAndUnloadDeletesTheSources() {
        SilentSoundService service = new SilentSoundService(null);
        int first = service.createBuffer(null);
        assertTrue(first > 0);
        assertNotEquals(first, service.createBuffer(null));
        service.deleteBuffer(first);

        service.createSoundSource(info("music", true));
        service.unload();
        assertTrue(service.getSources().isEmpty());
        assertTrue(service.getGainNodes().isEmpty());
    }
}
