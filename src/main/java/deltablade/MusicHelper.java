package deltablade;

import com.almasb.fxgl.audio.Music;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getAssetLoader;
import static com.almasb.fxgl.dsl.FXGL.getAudioPlayer;
import static com.almasb.fxgl.dsl.FXGL.getSettings;

/**
 * Background music: loop the selected catalog track, honor on/off and volume.
 * The catalog is scanned from {@code assets/music/}, not hardcoded here.
 */
public final class MusicHelper {

    public record Track(String id, String fileName, String displayName) {}

    private static List<Track> catalog;
    private static Music currentMusic;
    private static String currentTrackId;
    private static boolean playing;
    private static boolean loggedMissing;

    private MusicHelper() {}

    public static List<Track> tracks() {
        return catalog();
    }

    public static Track find(String id) {
        return MusicCatalog.lookup(catalog(), id).track();
    }

    public static Track currentTrack() {
        return find(currentTrackId != null ? currentTrackId : OptionsStore.getSelectedTrackId());
    }

    public static int indexOf(String id) {
        return Math.max(0, MusicCatalog.lookup(catalog(), id).index());
    }

    /**
     * Apply stored options: volume always, start or stop the loop as needed.
     */
    public static void applyFromStore() {
        setVolume(OptionsStore.getMusicVolume());
        if (!OptionsStore.isMusicEnabled()) {
            stop();
            return;
        }
        Track track = find(OptionsStore.getSelectedTrackId());
        if (track == null) {
            stop();
            return;
        }
        play(track);
    }

    public static void setVolume(double volume) {
        getSettings().setGlobalMusicVolume(Math.max(0.0, Math.min(1.0, volume)));
    }

    public static void playOverride(String fileName) {
        setVolume(OptionsStore.getMusicVolume());
        if (!OptionsStore.isMusicEnabled()) {
            stop();
            return;
        }
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        stop();
        try {
            Music music = getAssetLoader().loadMusic(fileName);
            if (music == null) {
                logMissing(fileName);
                applyFromStore();
                return;
            }
            getAudioPlayer().loopMusic(music);
            currentMusic = music;
            currentTrackId = "override:" + fileName;
            playing = true;
        } catch (Exception e) {
            logMissing(fileName + " - " + e.getMessage());
            applyFromStore();
        }
    }

    public static void play(Track track) {
        if (track == null) {
            return;
        }
        if (playing && track.id().equals(currentTrackId)) {
            setVolume(OptionsStore.getMusicVolume());
            return;
        }
        stop();
        try {
            Music music = getAssetLoader().loadMusic(track.fileName());
            if (music == null) {
                logMissing(track.fileName());
                return;
            }
            getAudioPlayer().loopMusic(music);
            currentMusic = music;
            currentTrackId = track.id();
            playing = true;
        } catch (Exception e) {
            logMissing(track.fileName() + " - " + e.getMessage());
        }
    }

    public static void stop() {
        try {
            if (currentMusic != null) {
                getAudioPlayer().stopMusic(currentMusic);
            }
            getAudioPlayer().stopAllMusic();
        } catch (Exception ignored) {
            // Audio stack may not be ready during shutdown.
        }
        currentMusic = null;
        currentTrackId = null;
        playing = false;
    }

    public static boolean isPlaying() {
        return playing;
    }

    private static List<Track> catalog() {
        if (catalog == null) {
            catalog = MusicCatalog.scan();
        }
        return catalog;
    }

    private static void logMissing(String detail) {
        if (loggedMissing) {
            return;
        }
        loggedMissing = true;
        System.err.println("[MusicHelper] Music not available: " + detail);
    }
}
