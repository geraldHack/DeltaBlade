package deltablade;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/**
 * MP3/WAV playback through JavaSound so Linux works without GStreamer.
 */
final class JavaSoundMusic {

    private static final Object LOCK = new Object();
    private static Thread worker;
    private static volatile boolean stopRequested;
    private static volatile double volume = 1.0;
    private static volatile SourceDataLine line;
    private static volatile boolean playing;

    private JavaSoundMusic() {}

    static boolean isPlaying() {
        return playing;
    }

    static void setVolume(double value) {
        volume = Math.max(0.0, Math.min(1.0, value));
        SourceDataLine current = line;
        if (current != null) {
            applyGain(current, volume);
        }
    }

    static void stop() {
        stopRequested = true;
        SourceDataLine current = line;
        if (current != null) {
            try {
                current.stop();
                current.flush();
                current.close();
            } catch (Exception ignored) {
            }
        }
        line = null;
        playing = false;
        Thread thread = worker;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(400);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        worker = null;
    }

    static void playLoop(String sourceUrl) {
        URL url = toUrl(sourceUrl);
        if (url == null) {
            return;
        }
        synchronized (LOCK) {
            stop();
            stopRequested = false;
            playing = true;
            worker = new Thread(() -> runLoop(url), "deltablade-music");
            worker.setDaemon(true);
            worker.start();
        }
    }

    static void playOnce(URL url, double gain) {
        if (url == null) {
            return;
        }
        Thread thread = new Thread(() -> playStandalone(url, gain), "deltablade-sfx");
        thread.setDaemon(true);
        thread.start();
    }

    static boolean looksSupported(String sourceUrl) {
        if (sourceUrl == null) {
            return false;
        }
        String lower = sourceUrl.toLowerCase(Locale.ROOT);
        return lower.contains(".mp3") || lower.contains(".wav");
    }

    private static void runLoop(URL url) {
        try {
            while (!stopRequested) {
                if (!decodeOnce(url)) {
                    break;
                }
            }
        } finally {
            playing = false;
            line = null;
        }
    }

    private static void playStandalone(URL url, double gain) {
        String path = url.getPath() == null ? "" : url.getPath().toLowerCase(Locale.ROOT);
        try (InputStream raw = new BufferedInputStream(url.openStream())) {
            if (path.endsWith(".wav")) {
                playWav(raw, gain, false);
            } else {
                playMp3(raw, gain, false);
            }
        } catch (Exception e) {
            System.err.println("[Music] JavaSound one-shot failed: " + e.getMessage());
        }
    }

    private static boolean decodeOnce(URL url) {
        String path = url.getPath() == null ? "" : url.getPath().toLowerCase(Locale.ROOT);
        try (InputStream raw = new BufferedInputStream(url.openStream())) {
            if (path.endsWith(".wav")) {
                return playWav(raw, volume, true);
            }
            return playMp3(raw, volume, true);
        } catch (Exception e) {
            System.err.println("[Music] JavaSound failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean playWav(InputStream raw, double gain, boolean bind) throws Exception {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(raw)) {
            AudioFormat base = source.getFormat();
            AudioFormat pcm = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(),
                    16,
                    base.getChannels(),
                    base.getChannels() * 2,
                    base.getSampleRate(),
                    false);
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, source);
                 SourceDataLine out = openLine(pcm)) {
                if (bind) {
                    line = out;
                }
                applyGain(out, gain);
                out.start();
                byte[] buf = new byte[4096];
                int read;
                while ((!bind || !stopRequested) && (read = decoded.read(buf)) >= 0) {
                    out.write(buf, 0, read);
                }
                if (!bind || !stopRequested) {
                    out.drain();
                }
            } finally {
                if (bind) {
                    line = null;
                }
            }
        }
        return !stopRequested;
    }

    private static boolean playMp3(InputStream raw, double gain, boolean bind) throws Exception {
        Bitstream bitstream = new Bitstream(raw);
        Decoder decoder = new Decoder();
        SourceDataLine out = null;
        try {
            while (!bind || !stopRequested) {
                Header header = bitstream.readFrame();
                if (header == null) {
                    break;
                }
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                if (out == null) {
                    AudioFormat format = new AudioFormat(
                            samples.getSampleFrequency(),
                            16,
                            samples.getChannelCount(),
                            true,
                            false);
                    out = openLine(format);
                    if (bind) {
                        line = out;
                    }
                    applyGain(out, gain);
                    out.start();
                }
                writePcm(out, samples);
                bitstream.closeFrame();
            }
            if (out != null && (!bind || !stopRequested)) {
                out.drain();
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
            if (bind) {
                line = null;
            }
            try {
                bitstream.close();
            } catch (Exception ignored) {
            }
        }
        return !stopRequested;
    }

    private static void writePcm(SourceDataLine out, SampleBuffer samples) {
        short[] src = samples.getBuffer();
        int count = samples.getBufferLength();
        byte[] dst = new byte[count * 2];
        for (int i = 0; i < count; i++) {
            int value = src[i];
            dst[i * 2] = (byte) value;
            dst[i * 2 + 1] = (byte) (value >> 8);
        }
        out.write(dst, 0, dst.length);
    }

    private static SourceDataLine openLine(AudioFormat format) throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine out = (SourceDataLine) AudioSystem.getLine(info);
        out.open(format);
        return out;
    }

    private static void applyGain(SourceDataLine out, double gain) {
        try {
            if (!out.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                return;
            }
            FloatControl control = (FloatControl) out.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float) (20.0 * Math.log10(Math.max(0.0001, gain)));
            db = Math.max(control.getMinimum(), Math.min(control.getMaximum(), db));
            control.setValue(db);
        } catch (Exception ignored) {
        }
    }

    private static URL toUrl(String sourceUrl) {
        try {
            return URI.create(sourceUrl).toURL();
        } catch (Exception e) {
            return null;
        }
    }
}
