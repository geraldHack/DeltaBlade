package deltablade.tools;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Renames jpackage output to DeltaBlade-&lt;version&gt;-&lt;os&gt;-&lt;arch&gt;.
 */
public final class NameDist {

    private NameDist() {}

    public static void main(String[] args) throws Exception {
        String version = args.length > 0 ? args[0] : "1.0.0";
        Path dist = Path.of(args.length > 1 ? args[1] : "target/dist");
        if (!Files.isDirectory(dist)) {
            System.err.println("No dist directory: " + dist.toAbsolutePath());
            System.exit(1);
        }

        String plat = platform();
        String arch = arch();
        String base = "DeltaBlade-" + version + "-" + plat + "-" + arch;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dist)) {
            for (Path item : stream) {
                String name = item.getFileName().toString();
                if (name.startsWith(".")) {
                    continue;
                }
                Path target;
                if (Files.isDirectory(item)) {
                    target = dist.resolve(base);
                } else {
                    String ext = extension(name);
                    target = dist.resolve(base + ext);
                }
                if (item.equals(target)) {
                    continue;
                }
                if (Files.exists(target)) {
                    if (Files.isDirectory(target)) {
                        deleteRecursive(target);
                    } else {
                        Files.delete(target);
                    }
                }
                Files.move(item, target);
                System.out.println("Packaged: " + target.getFileName());
            }
        }
    }

    private static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return "mac";
        }
        if (os.contains("win")) {
            return "win";
        }
        return "linux";
    }

    private static String arch() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "aarch64";
        }
        return "x64";
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private static void deleteRecursive(Path path) throws Exception {
        if (!Files.isDirectory(path)) {
            Files.deleteIfExists(path);
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path child : stream) {
                deleteRecursive(child);
            }
        }
        Files.deleteIfExists(path);
    }
}
