package deltablade;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getAudioPlayer;
import static com.almasb.fxgl.dsl.FXGL.getSettings;

/**
 * Background music: loop the selected catalog track, honor on/off and volume.
 * Player tracks live in the OS music folder ({@code DeltaBlade}); bundled files seed that folder.
 */
public final class MusicHelper {

    public record Track(String id, String fileName, String displayName, String sourceUrl) {}

    private static List<Track> catalog;
    private static MediaPlayer player;
    private static String currentTrackId;
    private static boolean playing;
    private static boolean loggedMissing;

    private MusicHelper() {}

    public static List<Track> tracks() {
        return catalog();
    }

    public static void rescan() {
        catalog = MusicCatalog.scan();
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
        double clamped = Math.max(0.0, Math.min(1.0, volume));
        try {
            getSettings().setGlobalMusicVolume(clamped);
        } catch (Exception ignored) {
        }
        if (player != null) {
            player.setVolume(clamped);
        }
        JavaSoundMusic.setVolume(clamped);
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
        URL resource = MusicHelper.class.getResource("/assets/music/" + fileName);
        if (resource == null) {
            logMissing(fileName);
            applyFromStore();
            return;
        }
        playUrl(resource.toExternalForm(), "override:" + fileName);
    }

    public static void play(Track track) {
        if (track == null) {
            return;
        }
        if (playing && track.id().equals(currentTrackId)) {
            setVolume(OptionsStore.getMusicVolume());
            return;
        }
        String url = track.sourceUrl();
        if (url == null || url.isBlank()) {
            URL resource = MusicHelper.class.getResource("/assets/music/" + track.fileName());
            if (resource == null) {
                logMissing(track.fileName());
                return;
            }
            url = resource.toExternalForm();
        }
        playUrl(url, track.id());
    }

    public static void stop() {
        JavaSoundMusic.stop();
        try {
            if (player != null) {
                player.stop();
                player.dispose();
            }
        } catch (Exception ignored) {
        }
        player = null;
        try {
            getAudioPlayer().stopAllMusic();
        } catch (Exception ignored) {
        }
        currentTrackId = null;
        playing = false;
    }

    public static boolean isPlaying() {
        return playing || JavaSoundMusic.isPlaying();
    }

    private static boolean preferJavaSound() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux");
    }

    private static void playUrl(String url, String trackId) {
        stop();
        if (preferJavaSound() && JavaSoundMusic.looksSupported(url)) {
            JavaSoundMusic.setVolume(OptionsStore.getMusicVolume());
            JavaSoundMusic.playLoop(url);
            currentTrackId = trackId;
            playing = true;
            return;
        }
        try {
            Media media = new Media(url);
            MediaPlayer next = new MediaPlayer(media);
            next.setCycleCount(MediaPlayer.INDEFINITE);
            next.setVolume(OptionsStore.getMusicVolume());
            next.setOnError(() -> {
                if (JavaSoundMusic.looksSupported(url)) {
                    JavaSoundMusic.setVolume(OptionsStore.getMusicVolume());
                    JavaSoundMusic.playLoop(url);
                    currentTrackId = trackId;
                    playing = true;
                    return;
                }
                logMissing(url + " - " + String.valueOf(next.getError()));
                if (trackId != null && trackId.startsWith("override:")) {
                    applyFromStore();
                }
            });
            next.play();
            player = next;
            currentTrackId = trackId;
            playing = true;
        } catch (Exception e) {
            if (JavaSoundMusic.looksSupported(url)) {
                JavaSoundMusic.setVolume(OptionsStore.getMusicVolume());
                JavaSoundMusic.playLoop(url);
                currentTrackId = trackId;
                playing = true;
                return;
            }
            logMissing(url + " - " + e.getMessage());
        }
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
