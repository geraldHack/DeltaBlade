package deltablade;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * User music folder: {@code <Music>/DeltaBlade}.
 * macOS/Windows: ~/Music.
 * Linux: XDG_MUSIC_DIR, ~/.config/user-dirs.dirs, xdg-user-dir,
 * then ~/Musik or ~/Music (German locale prefers Musik).
 */
final class MusicLocations {

    private static final String README_NAME = "LIESMICH.txt";
    private static final String README = """
            DeltaBlade — eigene Musik

            Lege MP3-, WAV- oder M4A-Dateien in diesen Ordner.
            Unterordner werden ignoriert.

            Danach im Spiel: OPTIONEN → Titel mit < > wählen.
            Der Ordner lässt sich dort auch mit „ORDNER ÖFFNEN“ anzeigen.
            """;

    private MusicLocations() {}

    static Path userLibraryDir() {
        return musicHome().resolve("DeltaBlade");
    }

    static String displayPath() {
        Path dir = userLibraryDir().toAbsolutePath();
        String home = System.getProperty("user.home", "");
        String abs = dir.toString();
        if (!home.isEmpty() && abs.startsWith(home)) {
            return "~" + abs.substring(home.length());
        }
        return abs;
    }

    private static Path musicHome() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String profile = System.getenv("USERPROFILE");
            return Path.of(profile != null && !profile.isBlank() ? profile : home, "Music");
        }
        if (os.contains("mac")) {
            return Path.of(home, "Music");
        }
        Path xdg = envMusicDir();
        if (xdg != null) {
            return xdg;
        }
        xdg = userDirsMusicDir(home);
        if (xdg != null) {
            return xdg;
        }
        xdg = xdgUserDirCommand();
        if (xdg != null) {
            return xdg;
        }
        Path musik = Path.of(home, "Musik");
        Path music = Path.of(home, "Music");
        if (Files.isDirectory(musik) && !Files.isDirectory(music)) {
            return musik;
        }
        if (Files.isDirectory(music) && !Files.isDirectory(musik)) {
            return music;
        }
        if (Files.isDirectory(musik)) {
            return musik;
        }
        return germanLocale() ? musik : music;
    }

    private static boolean germanLocale() {
        String lang = System.getenv("LANG");
        if (lang != null && lang.toLowerCase(Locale.ROOT).startsWith("de")) {
            return true;
        }
        return Locale.getDefault().getLanguage().equals("de");
    }

    private static Path envMusicDir() {
        String raw = System.getenv("XDG_MUSIC_DIR");
        return expandUserDir(raw, System.getProperty("user.home", "."));
    }

    private static Path userDirsMusicDir(String home) {
        Path file = Path.of(home, ".config", "user-dirs.dirs");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || !trimmed.startsWith("XDG_MUSIC_DIR")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                return expandUserDir(trimmed.substring(eq + 1).trim(), home);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static Path expandUserDir(String raw, String home) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.startsWith("$HOME")) {
            value = home + value.substring("$HOME".length());
        } else if (value.startsWith("~/")) {
            value = home + value.substring(1);
        }
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        return Path.of(value);
    }

    private static Path xdgUserDirCommand() {
        try {
            Process process = new ProcessBuilder("xdg-user-dir", "MUSIC")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(1, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return null;
            }
            String line = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (line.isEmpty()) {
                return null;
            }
            return Path.of(line);
        } catch (Exception ignored) {
            return null;
        }
    }

    static Path ensureUserLibrary() {
        Path dir = userLibraryDir();
        try {
            Files.createDirectories(dir);
            Path readme = dir.resolve(README_NAME);
            if (!Files.exists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
            seedFromBundled(dir);
        } catch (IOException e) {
            System.err.println("[Music] Could not prepare " + dir + ": " + e.getMessage());
        }
        return dir;
    }

    static void revealUserLibrary() {
        Path dir = ensureUserLibrary();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("mac")) {
                new ProcessBuilder("open", dir.toAbsolutePath().toString()).start();
                return;
            }
            if (os.contains("win")) {
                new ProcessBuilder("explorer", dir.toAbsolutePath().toString()).start();
                return;
            }
            new ProcessBuilder("xdg-open", dir.toAbsolutePath().toString()).start();
        } catch (Exception e) {
            System.err.println("[Music] Could not open folder: " + e.getMessage());
        }
    }

    static List<Path> extraScanDirs() {
        List<Path> dirs = new ArrayList<>();
        Path workspace = Path.of("music");
        if (Files.isDirectory(workspace)
                && (Files.isRegularFile(Path.of("pom.xml")) || Files.isDirectory(Path.of("src/main/resources")))) {
            Path user = userLibraryDir().toAbsolutePath().normalize();
            if (!workspace.toAbsolutePath().normalize().equals(user)) {
                dirs.add(workspace);
            }
        }
        return dirs;
    }

    static boolean isCatalogAudio(String fileName) {
        if (fileName == null || fileName.startsWith(".") || fileName.startsWith("_")) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a");
    }

    private static boolean hasSimilarAudio(Path dir, String bundledName) {
        String want = catalogKey(bundledName);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path) && catalogKey(path.getFileName().toString()).equals(want)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static String catalogKey(String fileName) {
        String stem = fileName;
        int dot = stem.lastIndexOf('.');
        if (dot > 0) {
            stem = stem.substring(0, dot);
        }
        return stem.replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    static boolean hasAudio(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path) && isCatalogAudio(path.getFileName().toString())) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static void seedFromBundled(Path dir) {
        URL resourceDir = MusicLocations.class.getResource("/assets/music/");
        if (resourceDir == null) {
            resourceDir = MusicLocations.class.getResource("/assets/music");
        }
        List<String> names = new ArrayList<>();
        if (resourceDir != null) {
            MusicCatalog.collectFromUrl(resourceDir, names);
        }
        if (names.isEmpty()) {
            MusicCatalog.collectFromDevFallback(names);
        }
        for (String name : names) {
            Path dest = dir.resolve(name);
            if (Files.exists(dest) || hasSimilarAudio(dir, name)) {
                continue;
            }
            try (InputStream in = MusicLocations.class.getResourceAsStream("/assets/music/" + name)) {
                if (in != null) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.err.println("[Music] Could not seed " + name + ": " + e.getMessage());
            }
        }
    }
}
