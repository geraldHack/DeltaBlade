package deltablade;

import java.awt.Desktop;
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

/**
 * User-facing music folder: {@code ~/Music/DeltaBlade} (Finder: Musik/DeltaBlade).
 * Bundled default tracks are copied there on first launch so the folder is never empty.
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
        return Path.of(System.getProperty("user.home"), "Music", "DeltaBlade");
    }

    static String displayPath() {
        return "~/Music/DeltaBlade";
    }

    static Path ensureUserLibrary() {
        Path dir = userLibraryDir();
        try {
            Files.createDirectories(dir);
            Path readme = dir.resolve(README_NAME);
            if (!Files.exists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
            if (!hasAudio(dir)) {
                seedFromBundled(dir);
            }
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
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir.toFile());
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
            if (Files.exists(dest)) {
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
