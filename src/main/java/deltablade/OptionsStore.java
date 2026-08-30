package deltablade;

import java.util.prefs.Preferences;

/**
 * Persistent player options. Survives restarts via Java Preferences.
 */
public final class OptionsStore {

    private static final Preferences PREFS = Preferences.userNodeForPackage(OptionsStore.class);

    private static final String KEY_MUSIC_ENABLED = "musicEnabled";
    private static final String KEY_MUSIC_VOLUME = "musicVolume";
    private static final String KEY_MUSIC_TRACK = "musicTrack";

    public static final double DEFAULT_MUSIC_VOLUME = 0.55;

    private OptionsStore() {}

    public static boolean isMusicEnabled() {
        return PREFS.getBoolean(KEY_MUSIC_ENABLED, true);
    }

    public static void setMusicEnabled(boolean enabled) {
        PREFS.putBoolean(KEY_MUSIC_ENABLED, enabled);
    }

    public static double getMusicVolume() {
        return clamp01(PREFS.getDouble(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME));
    }

    public static void setMusicVolume(double volume) {
        PREFS.putDouble(KEY_MUSIC_VOLUME, clamp01(volume));
    }

    public static String getSelectedTrackId() {
        return PREFS.get(KEY_MUSIC_TRACK, "");
    }

    public static void setSelectedTrackId(String trackId) {
        if (trackId == null || trackId.isBlank()) {
            return;
        }
        PREFS.put(KEY_MUSIC_TRACK, trackId);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
