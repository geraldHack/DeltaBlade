package deltablade;

import com.almasb.fxgl.audio.Sound;

import java.util.HashSet;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * Safe audio wrapper: missing files never crash, only log once.
 */
public final class SoundHelper {
    
    private static final Set<String> loggedMissing = new HashSet<>();
    private static final Set<String> loadedSounds = new HashSet<>();
    
    private SoundHelper() {}
    
    /**
     * Play a sound by name (e.g. "money.wav").
     * Loads from assets/sounds/<name>. Logs once if missing, never throws.
     */
    public static void play(String name) {
        if (name == null || name.isEmpty()) return;
        
        try {
            if (!loadedSounds.contains(name)) {
                try {
                    Sound sound = getAssetLoader().loadSound(name);
                    if (sound != null) {
                        loadedSounds.add(name);
                    }
                } catch (Exception e) {
                    if (loggedMissing.add(name)) {
                        System.err.println("[SoundHelper] Sound not found: " + name);
                    }
                    return;
                }
            }
            getAudioPlayer().playSound(getAssetLoader().loadSound(name));
        } catch (Exception e) {
            if (loggedMissing.add(name)) {
                System.err.println("[SoundHelper] Failed to play sound: " + name + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if a sound file exists and can be loaded.
     */
    public static boolean exists(String name) {
        if (name == null || name.isEmpty()) return false;
        if (loadedSounds.contains(name)) return true;
        
        try {
            Sound sound = getAssetLoader().loadSound(name);
            if (sound != null) {
                loadedSounds.add(name);
                return true;
            }
        } catch (Exception e) {
            // Sound doesn't exist
        }
        return false;
    }
}
