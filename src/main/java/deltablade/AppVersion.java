package deltablade;

import java.io.InputStream;
import java.util.Properties;

/**
 * Release version from the Maven build, e.g. 1.0.0.
 */
public final class AppVersion {

    private static final String FALLBACK = "1.0.0";
    private static final String VALUE = load();

    private AppVersion() {}

    public static String current() {
        return VALUE;
    }

    public static String label() {
        return "v" + VALUE;
    }

    private static String load() {
        try (InputStream in = AppVersion.class.getResourceAsStream("/deltablade/version.properties")) {
            if (in == null) {
                return FALLBACK;
            }
            Properties properties = new Properties();
            properties.load(in);
            String version = properties.getProperty("version", FALLBACK).trim();
            return version.isEmpty() || version.contains("${") ? FALLBACK : version;
        } catch (Exception e) {
            return FALLBACK;
        }
    }
}
