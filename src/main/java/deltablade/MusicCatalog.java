package deltablade;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Discovers selectable BGM from {@code assets/music/}.
 * Only files in that folder itself are catalog tracks; anything in a
 * subdirectory (e.g. {@code cues/}) is left for one-shots and overrides.
 */
final class MusicCatalog {

    private static final String RESOURCE_DIR = "/assets/music/";
    private static final String CLASSPATH_PREFIX = "assets/music/";

    private MusicCatalog() {}

    static List<MusicHelper.Track> scan() {
        List<String> files = listTopLevelAudio();
        files.sort(String.CASE_INSENSITIVE_ORDER);

        List<MusicHelper.Track> tracks = new ArrayList<>();
        for (String fileName : files) {
            String id = stripExtension(fileName);
            tracks.add(new MusicHelper.Track(id, fileName, displayName(id)));
        }
        return List.copyOf(tracks);
    }

    static String displayName(String idOrStem) {
        String spaced = idOrStem.replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim();
        if (spaced.isEmpty()) {
            return idOrStem;
        }
        String[] words = spaced.split(" ");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                name.append(word.substring(1));
            }
        }
        return name.toString();
    }

    private static List<String> listTopLevelAudio() {
        List<String> names = new ArrayList<>();

        URL url = MusicCatalog.class.getResource(RESOURCE_DIR);
        if (url == null) {
            url = MusicCatalog.class.getResource("/assets/music");
        }
        if (url != null) {
            collectFromUrl(url, names);
        }
        if (names.isEmpty()) {
            collectFromDevFallback(names);
        }
        return names;
    }

    private static void collectFromUrl(URL url, List<String> names) {
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            collectFromDirectory(urlToPath(url), names);
            return;
        }
        if ("jar".equals(protocol)) {
            collectFromJar(url, names);
        }
    }

    private static void collectFromDirectory(Path dir, List<String> names) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path) && isCatalogAudio(path.getFileName().toString())) {
                    addUnique(names, path.getFileName().toString());
                }
            }
        } catch (IOException e) {
            System.err.println("[MusicCatalog] Could not read " + dir + ": " + e.getMessage());
        }
    }

    private static void collectFromJar(URL url, List<String> names) {
        String raw = url.getPath();
        int bang = raw.indexOf('!');
        if (bang < 0) {
            return;
        }
        String jarPath = URLDecoder.decode(raw.substring("file:".length(), bang), StandardCharsets.UTF_8);
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith(CLASSPATH_PREFIX) || name.endsWith("/")) {
                    continue;
                }
                String relative = name.substring(CLASSPATH_PREFIX.length());
                if (relative.contains("/")) {
                    continue;
                }
                if (isCatalogAudio(relative)) {
                    addUnique(names, relative);
                }
            }
        } catch (IOException e) {
            System.err.println("[MusicCatalog] Could not read jar music folder: " + e.getMessage());
        }
    }

    private static void collectFromDevFallback(List<String> names) {
        for (Path dir : List.of(
                Path.of("src/main/resources/assets/music"),
                Path.of("target/classes/assets/music"))) {
            collectFromDirectory(dir, names);
        }
    }

    private static Path urlToPath(URL url) {
        try {
            URI uri = url.toURI();
            File file = new File(uri);
            return file.toPath();
        } catch (URISyntaxException | IllegalArgumentException e) {
            return Path.of(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8));
        }
    }

    private static boolean isCatalogAudio(String fileName) {
        if (fileName.startsWith(".") || fileName.startsWith("_")) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a");
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static void addUnique(List<String> names, String fileName) {
        if (!names.contains(fileName)) {
            names.add(fileName);
        }
    }

    static TrackLookup lookup(List<MusicHelper.Track> tracks, String id) {
        if (tracks.isEmpty()) {
            return new TrackLookup(null, -1);
        }
        if (id == null || id.isBlank()) {
            return new TrackLookup(tracks.getFirst(), 0);
        }
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).id().equals(id)) {
                return new TrackLookup(tracks.get(i), i);
            }
        }
        for (int i = 0; i < tracks.size(); i++) {
            MusicHelper.Track track = tracks.get(i);
            if (track.id().startsWith(id) || track.fileName().startsWith(id)) {
                return new TrackLookup(track, i);
            }
        }
        return new TrackLookup(tracks.getFirst(), 0);
    }

    record TrackLookup(MusicHelper.Track track, int index) {}
}
