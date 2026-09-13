package deltablade;

import com.almasb.fxgl.audio.Sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.almasb.fxgl.dsl.FXGL.getAssetLoader;
import static com.almasb.fxgl.dsl.FXGL.getAudioPlayer;
import static com.almasb.fxgl.dsl.FXGL.getSettings;

/**
 * WAV effects play through JavaSound so they stay audible next to BGM.
 * JavaFX AudioClip (FXGL's default) is often silent on recent macOS/JDK builds
 * while MediaPlayer music still works. MP3 one-shots stay on FXGL.
 */
public final class SoundHelper {

    private static final Set<String> loggedMissing = new HashSet<>();
    private static final double DEFAULT_MASTER = 0.55;
    private static final double WARP_RATIO = 0.18 / DEFAULT_MASTER;
    private static final int MAX_VOICES = 8;
    private static final double TARGET_RMS = 32767.0 * Math.pow(10.0, -16.0 / 20.0);
    private static final double PEAK_CEILING = 32767.0 * Math.pow(10.0, -1.0 / 20.0);
    private static final String[] WARMUP_SOUNDS = {
            "shot.wav", "enemy_shot.wav", "explode_ship.wav", "explode_boss.wav",
            "money.wav", "scoop.wav", "shield.wav", "ammo.wav", "weapon.wav",
            "autofire.wav", "extra.wav", "extra_life.wav", "extra_time.wav",
            "bonus_round.wav", "bonus_perfect.wav", "get_ready.wav", "rank_wave.wav",
            "rank_up.wav", "rank_marker.wav", "hurry_warning.wav", "ufo_appear.wav",
            "missile_launch.wav"
    };

    private static final Map<String, Sample> samples = new ConcurrentHashMap<>();
    private static final Map<String, List<Clip>> voices = new ConcurrentHashMap<>();
    private static volatile double previewVolume = -1;

    private record Sample(AudioFormat format, byte[] pcm) {}

    private SoundHelper() {}

    public static void warmup() {
        for (String name : WARMUP_SOUNDS) {
            preload(name, name.equals("shot.wav") || name.equals("enemy_shot.wav") ? 4 : 1);
        }
        applyMasterVolume();
    }

    public static void preload(String name) {
        preload(name, 1);
    }

    public static void preload(String name, int voicesToOpen) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Sample sample = samples.computeIfAbsent(name, SoundHelper::loadSample);
        if (sample == null) {
            return;
        }
        try {
            for (int i = 0; i < Math.max(1, voicesToOpen); i++) {
                ensureVoice(name, sample);
            }
        } catch (Exception ignored) {
        }
    }

    public static void applyMasterVolume() {
        try {
            getSettings().setGlobalSoundVolume(masterVolume());
        } catch (Exception ignored) {
        }
        for (List<Clip> clips : voices.values()) {
            synchronized (clips) {
                for (Clip clip : clips) {
                    applyGain(clip);
                }
            }
        }
    }

    public static void playPreview(String name, double volume) {
        previewVolume = Math.max(0.0, Math.min(1.0, volume));
        play(name);
        previewVolume = -1;
    }

    public static void play(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        if (name.toLowerCase().endsWith(".mp3")) {
            playFxgl(name);
            return;
        }
        if (playClip(name)) {
            return;
        }
        playFxgl(name);
    }

    public static void stop(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        List<Clip> clips = voices.get(name);
        if (clips != null) {
            synchronized (clips) {
                for (Clip clip : clips) {
                    try {
                        clip.stop();
                        clip.setFramePosition(0);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        try {
            Sound sound = getAssetLoader().loadSound(name);
            if (sound != null) {
                getAudioPlayer().stopSound(sound);
            }
        } catch (Exception ignored) {
        }
        if ("warp.mp3".equalsIgnoreCase(name)) {
            applyMasterVolume();
        }
    }

    public static boolean exists(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (samples.containsKey(name)) {
            return true;
        }
        return loadSample(name) != null;
    }

    private static boolean playClip(String name) {
        try {
            Sample sample = samples.computeIfAbsent(name, SoundHelper::loadSample);
            if (sample == null) {
                return false;
            }
            Clip clip = voice(name, sample);
            if (clip == null) {
                return false;
            }
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
            return true;
        } catch (Exception e) {
            if (loggedMissing.add("play:" + name)) {
                System.err.println("[SoundHelper] Clip play failed: " + name + " - " + e.getMessage());
            }
            return false;
        }
    }

    private static Clip voice(String name, Sample sample) throws LineUnavailableException {
        List<Clip> clips = voices.computeIfAbsent(name, key -> new ArrayList<>());
        synchronized (clips) {
            for (Clip clip : clips) {
                if (!clip.isRunning()) {
                    return clip;
                }
            }
            if (clips.size() >= MAX_VOICES) {
                Clip steal = clips.get(0);
                steal.stop();
                return steal;
            }
            return ensureVoice(name, sample);
        }
    }

    private static Clip ensureVoice(String name, Sample sample) throws LineUnavailableException {
        List<Clip> clips = voices.computeIfAbsent(name, key -> new ArrayList<>());
        synchronized (clips) {
            if (clips.size() >= MAX_VOICES) {
                return clips.get(0);
            }
            Clip clip = openClip(sample);
            clips.add(clip);
            return clip;
        }
    }

    private static Clip openClip(Sample sample) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(Clip.class, sample.format());
        Clip clip;
        if (AudioSystem.isLineSupported(info)) {
            clip = (Clip) AudioSystem.getLine(info);
        } else {
            clip = AudioSystem.getClip();
        }
        clip.open(sample.format(), sample.pcm(), 0, sample.pcm().length);
        applyGain(clip);
        return clip;
    }

    private static Sample loadSample(String name) {
        String path = "/assets/sounds/" + name;
        try (InputStream raw = SoundHelper.class.getResourceAsStream(path)) {
            if (raw == null) {
                if (loggedMissing.add(name)) {
                    System.err.println("[SoundHelper] Sound not found: " + name);
                }
                return null;
            }
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                AudioFormat source = in.getFormat();
                AudioInputStream pcmStream = in;
                AudioFormat pcm = source;
                if (source.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                        || source.getSampleSizeInBits() != 16) {
                    pcm = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            source.getSampleRate(),
                            16,
                            source.getChannels(),
                            source.getChannels() * 2,
                            source.getSampleRate(),
                            false);
                    pcmStream = AudioSystem.getAudioInputStream(pcm, in);
                }
                AudioFormat outFormat = pcmStream.getFormat();
                byte[] data = pcmStream.readAllBytes();
                if (pcmStream != in) {
                    pcmStream.close();
                }
                if (data.length == 0) {
                    return null;
                }
                return new Sample(outFormat, normalizePcm(data, outFormat));
            }
        } catch (Exception e) {
            if (loggedMissing.add(name)) {
                System.err.println("[SoundHelper] Failed to load: " + name + " - " + e.getMessage());
            }
            return null;
        }
    }

    private static byte[] normalizePcm(byte[] data, AudioFormat format) {
        if (format.getSampleSizeInBits() != 16 || data.length < 2) {
            return data;
        }
        int count = data.length / 2;
        double acc = 0;
        int peak = 1;
        for (int i = 0; i < count; i++) {
            int s = (short) ((data[i * 2] & 0xff) | (data[i * 2 + 1] << 8));
            acc += (double) s * s;
            int abs = Math.abs(s);
            if (abs > peak) {
                peak = abs;
            }
        }
        double rms = Math.sqrt(acc / count);
        double gain = rms > 1 ? TARGET_RMS / rms : 1.0;
        if (peak * gain > PEAK_CEILING) {
            gain = PEAK_CEILING / peak;
        }
        if (Math.abs(gain - 1.0) < 0.03) {
            return data;
        }
        byte[] out = data.clone();
        for (int i = 0; i < count; i++) {
            int s = (short) ((data[i * 2] & 0xff) | (data[i * 2 + 1] << 8));
            int v = (int) Math.round(s * gain);
            if (v > 32767) {
                v = 32767;
            } else if (v < -32768) {
                v = -32768;
            }
            out[i * 2] = (byte) v;
            out[i * 2 + 1] = (byte) (v >> 8);
        }
        return out;
    }

    private static void applyGain(Clip clip) {
        try {
            if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                return;
            }
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float) (20.0 * Math.log10(Math.max(0.0001, masterVolume())));
            db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
            gain.setValue(db);
        } catch (Exception ignored) {
        }
    }

    private static double masterVolume() {
        if (previewVolume >= 0) {
            return previewVolume;
        }
        return OptionsStore.getEffectVolume();
    }

    private static double volumeOf(String name) {
        double master = masterVolume();
        return "warp.mp3".equalsIgnoreCase(name) ? master * WARP_RATIO : master;
    }

    private static void playFxgl(String name) {
        if (System.getProperty("os.name", "").toLowerCase().contains("linux")
                && name.toLowerCase().endsWith(".mp3")) {
            java.net.URL url = SoundHelper.class.getResource("/assets/sounds/" + name);
            if (url != null) {
                JavaSoundMusic.playOnce(url, volumeOf(name));
                return;
            }
        }
        try {
            getSettings().setGlobalSoundVolume(volumeOf(name));
            Sound sound = getAssetLoader().loadSound(name);
            if (sound == null) {
                if (loggedMissing.add(name)) {
                    System.err.println("[SoundHelper] Sound not found: " + name);
                }
                return;
            }
            getAudioPlayer().playSound(sound);
        } catch (Exception e) {
            if (loggedMissing.add(name)) {
                System.err.println("[SoundHelper] Failed to play sound: " + name + " - " + e.getMessage());
            }
        }
    }
}
