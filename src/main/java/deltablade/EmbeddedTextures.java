package deltablade;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Embedded PNG textures as Base64 byte arrays.
 * This ensures textures work regardless of classpath/Maven issues.
 * Original PNGs remain in resources for attribution.
 */
public final class EmbeddedTextures {

    private static final Map<String, String> TEXTURE_DATA = new HashMap<>();
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();
    private static String preloadError = null;

    static {
        TEXTURE_DATA.put("player.png",
            "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAOBJREFUWIXtlKEOgkAcxj9tTmc2E0xE3sSmvoA0C8/gJJiwWCVK8lFIBKqymZzOxmdBBhP0zhvOufvFj919v/sfAGg0Go1GEfN4pcr6tlK7l1BV4nOBrLw/nUNFQmkCw/UERYmvCwDA6RArrZcWGM8W+agj2wcAnLeryueNCADl+45sH+Hu8pSL0pJu9xKaox4A5MUA8MgAIBx0hfeVm0D25ldRkpGYhLjAi/IqVH9QtbibPUnS3ewpkv8XN8dgGlgkyTSweHMMvspFkP4Kipt3lnHrXd4IdSeUOblGo/kZ7lHQeVbiboHZAAAAAElFTkSuQmCC");
        TEXTURE_DATA.put("enemy_basic.png",
            "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAJ5JREFUWIVjYBgFo2AUjIKhCLpnb/mPTfzA6TtYxfEBJmo4hByLqeIAagCyHYAeDeSGAsUhQEnwk+UAXAkQBkh10NBNAzBw+sKNgXPA7AkTGNav3MRQVdlFPweUpvowwixHBjBHOJiqMJLtGlJA9+wt/9W0Xf6rabv8t3JJ+09ubqDItd2zt/w3NdBgYGCgo8/RAaXlwCgYBaNgFIwCAJXhO9SLoAImAAAAAElFTkSuQmCC");
        TEXTURE_DATA.put("enemy_fast.png",
            "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAURJREFUWIVjYBgFo2AUjHTASK7GrQEd/9HFvDdUkGweyRqwWUyJQ5hIsTzDIoeg5QwMxDkSBoh2KbLlvhIyDF03VjFQUFBQvD1wmMEmoBwujgyICQmiHIDu8yMbOhl+OftjqGPbu5GhM2MySY5gIcYByOD6h2MMb3QMGObaJGPIFTr7M2zm1cAICXyAYAgg+/76h2MMN998ZghQcWdgYMAMcgYGBobC23sYnNAcgS8USEqEtAAkOeDtgcMo/M0vnmBVd2RDJ20cIOxgiyGGzRGwXEF1B+ACm188wRkahADJ2RA9ISKDfZ9vkJQAGRjIyIaaAlYMbw90MmzQMcCQE7lygcEXrRwgBMgqCRkYICHBwABJmLC0oSlgRZLvSXIANkfgAr4SMkRXSCTXhsQ4YsaJKUSbS3Z7AJtDSLF4FIyCUTBoAAA+bW7iWQPx1AAAAABJRU5ErkJggg==");
        TEXTURE_DATA.put("enemy_tough.png",
            "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAQ9JREFUWIXtkyEOwjAUhv8RgkISmCYEBclOQDIUHssJ4ACcgaBR2B1hYmoknIAERwgGMxIUEvMQUNKVva4bGJJ+but77//7twUsFovl71ld6Jt2h1sYLEYEANt5xNakxGetUrMqeQ5Fc5Z4b1wHTZvojetsEmz/C9Z1dzIkt19L/dOmkSOc7O84BLF5AocgdpL9/WOoPHi5DomIaLkOiavRiQOaBIBnCgCgJlEEsYlSBmQTZYzkiQNAtehAYUI9Htlg1hqH0aWSU5C5RWe0PR8AcHWPmb263QMGz5AbcovOqe9G0iksbmxAN+y02xSqVzF+1zLiSEQKbc9/H4Gp8FcGVCNlhH8Gd0EtFovlb3gA/8d9jL+0nwoAAAAASUVORK5CYII=");
        TEXTURE_DATA.put("bullet_player.png",
            "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAEJJREFUOI1jYBgFAw8Y8Ul+L1P6D2Nzdt3DqpYFn2b2QnsEn4HhPzZDmAg6UWIeXnmCBvx/kYTfAnySxITBKKACAACzBxG4wYAunwAAAABJRU5ErkJggg==");
        TEXTURE_DATA.put("bullet_enemy.png",
            "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAEBJREFUOI1jYBgFAw8Y8cpOffkfzs4Wx6qWBZ9mnWAeOPcKw8v/2AxhIuTEy+JceOUJGqD78hteeYrDYBRQAQAAKT4PUllGJG4AAAAASUVORK5CYII=");
        TEXTURE_DATA.put("pickup_weapon.png",
            "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAALpJREFUOI1jYBjygBFd4MDpO/+R+VWVXQxt7WUoahxMVeD6mJAlumdv+X/6wg2CtiJbguKC7tlb/jMwMDCsX7kJp2aYa2CuYMFn05HFv+Fsm1hWrGqYsIoiaWaUmIdhGDJAcQG602GaGSXmMfx/kcTAwAAJVJwGBIb7oRj0/0USimYGBqQw2DMLvxdgfoZpxhUGeAMRlyaCBsC8wsAA8Q4y//SFGwymBhpwPooXSlN9MFImNoCcEgceAADwuD7Gc/bdhQAAAABJRU5ErkJggg==");
        TEXTURE_DATA.put("pickup_ammo.png",
            "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAJxJREFUOI1jYBjygBFd4MDpO/+R+VWVXQxt7WUoahxMVeD6mJAlumdv+X/6wg2CtiJbguKC7tlb/jMwMDCsX7kJp2aYa2CuYMKl8Mji31hpdIDTAGIBCzIHn9NhoKqyC7cBgeF+BA2Ch8GeWQwMDFTwAk4DbGJZsdLogAWbIMwrDAwQ7yDzT1+4wWBqoIHdBaWpPhgpExtATokDDwDmXDIkOfyITwAAAABJRU5ErkJggg==");
    }

    private EmbeddedTextures() {}

    /**
     * Preload all embedded textures into cache at native size.
     * Call during title screen to detect problems early.
     * @return error message if any texture failed, null if all OK
     */
    public static String preloadAll() {
        StringBuilder errors = new StringBuilder();
        for (String name : TEXTURE_DATA.keySet()) {
            Image img = getImageNative(name);
            if (img == null || img.isError()) {
                if (errors.length() > 0) errors.append(", ");
                errors.append(name);
            }
        }
        if (errors.length() > 0) {
            preloadError = "Fehler beim Laden: " + errors;
            return preloadError;
        }
        preloadError = null;
        return null;
    }

    /**
     * Get preload error if any occurred during preloadAll()
     */
    public static String getPreloadError() {
        return preloadError;
    }

    /**
     * Check if a texture name exists in embedded data
     */
    public static boolean hasTexture(String name) {
        return TEXTURE_DATA.containsKey(name);
    }

    /**
     * Get an Image at native size from embedded data, with caching.
     * Uses ImageIO (AWT) to decode, then copies pixels to WritableImage.
     * Returns null only if the texture name is not in embedded data.
     */
    public static Image getImageNative(String name) {
        String cacheKey = name + "_native";
        if (IMAGE_CACHE.containsKey(cacheKey)) {
            return IMAGE_CACHE.get(cacheKey);
        }

        String base64 = TEXTURE_DATA.get(name);
        if (base64 == null) {
            return null;
        }

        byte[] bytes = Base64.getDecoder().decode(base64);

        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bi != null) {
                WritableImage wi = convertToWritableImage(bi);
                IMAGE_CACHE.put(cacheKey, wi);
                return wi;
            }
        } catch (Exception e) {
            // ImageIO failed, try JavaFX fallback
        }

        try {
            Image img = new Image(new ByteArrayInputStream(bytes));
            if (!img.isError()) {
                IMAGE_CACHE.put(cacheKey, img);
                return img;
            }
        } catch (Exception e) {
            // Both methods failed
        }
        return null;
    }

    /**
     * Get an Image from embedded data, with caching.
     * The w/h parameters are only used for ImageView sizing, not for loading.
     * Returns null only if the texture name is not in embedded data.
     */
    public static Image getImage(String name, int w, int h) {
        return getImageNative(name);
    }

    /**
     * Convert a BufferedImage to a WritableImage by copying ARGB pixels.
     * Does NOT use SwingFXUtils (requires javafx.swing module).
     */
    private static WritableImage convertToWritableImage(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        WritableImage wi = new WritableImage(width, height);
        PixelWriter pw = wi.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bi.getRGB(x, y);
                pw.setArgb(x, y, argb);
            }
        }
        return wi;
    }

    /**
     * Create a simple triangle ship polygon as fallback.
     * NEVER returns a square/rectangle - this is the visual bug indicator.
     */
    public static Polygon createFallbackShip(int size, Color color) {
        double half = size / 2.0;
        double top = 0;
        double bottom = size;
        double left = 0;
        double right = size;
        double midX = half;
        double wingY = size * 0.7;

        Polygon ship = new Polygon(
            midX, top,
            right, wingY,
            midX, bottom * 0.85,
            left, wingY
        );
        ship.setFill(color);
        ship.setStroke(color.brighter());
        ship.setStrokeWidth(1);
        return ship;
    }
}
