package deltablade;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Embedded PNG textures as Base64 byte arrays.
 * This ensures textures work regardless of classpath/Maven issues.
 * Original PNGs remain in resources for attribution.
 * 
 * PNG decoding uses only java.util.zip.Inflater and PixelWriter.
 * No AWT, no ImageIO, no JavaFX Image(InputStream).
 */
public final class EmbeddedTextures {

    private static final Map<String, String> TEXTURE_DATA = new HashMap<>();
    private static final Map<String, WritableImage> IMAGE_CACHE = new HashMap<>();
    private static String preloadError = null;

    static {
        TEXTURE_DATA.put("player.png",
            "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAOBJREFUWIXtlKEOgkAcxj9tTmc2E0xE3sSmvoA0C8/gJJiwWCVK8lFIBKqymZzOxmdBBhP0zhvOufvFj919v/sfAGg0Go1GEfN4pcr6tlK7l1BV4nOBrLw/nUNFQmkCw/UERYmvCwDA6RArrZcWGM8W+agj2wcAnLeryueNCADl+45sH+Hu8pSL0pJu9xKaox4A5MUA8MgAIBx0hfeVm0D25ldRkpGYhLjAi/IqVH9QtbibPUnS3ewpkv8XN8dgGlgkyTSweHMMvspFkP4Kipt3lnHrXd4IdSeUOblGo/kZ7lHQeVbiboHZAAAAAElFTkSuQmCC");
        TEXTURE_DATA.put("enemy_basic.png",
            "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAJ5JREFUWIVjYBgFo2AUjIKhCLpnb/mPTfzA6TtYxfEBJmo4hByLqeIAagCyHYAeDeSGAsUhQEnwk+UAXAkQBkh10NBNAzBw+sKNgXPA7AkTGNav3MRQVdlFPweUpvowwixHBjBHOJiqMJLtGlJA9+wt/9W0Xf6rabv8t3JJ+09ubqDItd2zt/w3NdBgYGCgo8/RAaXlwCgYBaNgFIwCAJXhO9SLoAImAAAAAElFTkSuQmCC");
        TEXTURE_DATA.put("enemy_fast.png",
            "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAABCUlEQVR4nO2VvQqDMBDH/1YQshQczeSkq4/QR+mj9VH6CF27OdnR0SLCdUoIMZovSynkN2m4j3/uzhNIJBKRNEtFMf6nI5LHiAgW0CwVnXOGJuc45yy6Ej8jC3VsiRMAMBTy7JH13vGCWiCS63RUe7fBWYBvcFd7JwF6sGc2yFJPmDFhBrBugYsIqwBTELUFDIWcA5OtTcTu0LTESR2yCTMYCnljIUBH2KlsDai1AmoyE8N9DPJzFqAHU59ft3ElYss2SIA6bOJdPzP56DZ7+8FaAVvCd79/S9tycmrBlojqWgIA+KX08oumJU7iUxSfWUc1fXUTbhGy/w9B/x+E3B44oAKJROLv+QDZ1nI3Y7YusQAAAABJRU5ErkJggg==");
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
            WritableImage img = getImageNative(name);
            if (img == null || img.getWidth() == 0 || img.getHeight() == 0) {
                if (errors.length() > 0) errors.append(", ");
                errors.append(name);
                if (preloadError != null && !preloadError.isEmpty()) {
                    errors.append(": ").append(preloadError);
                }
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
     * Get a WritableImage at native PNG size from embedded data, with caching.
     * Uses pure Inflater+PixelWriter decoding, no AWT/ImageIO.
     * Returns null only if the texture name is not in embedded data or decode fails.
     */
    public static WritableImage getImageNative(String name) {
        String cacheKey = name + "_native";
        if (IMAGE_CACHE.containsKey(cacheKey)) {
            return IMAGE_CACHE.get(cacheKey);
        }

        String base64 = TEXTURE_DATA.get(name);
        if (base64 == null) {
            preloadError = "missing key";
            return null;
        }

        try {
            byte[] pngBytes = Base64.getDecoder().decode(base64);
            WritableImage img = decodePng(pngBytes, name);
            if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
                IMAGE_CACHE.put(cacheKey, img);
                return img;
            }
        } catch (Exception e) {
            preloadError = e.getMessage();
        }
        return null;
    }

    /**
     * Get an Image from embedded data, with caching.
     * The w/h parameters are only used for ImageView sizing, not for loading.
     * Returns null only if the texture name is not in embedded data.
     */
    public static WritableImage getImage(String name, int w, int h) {
        return getImageNative(name);
    }

    /**
     * Decode a PNG byte array to WritableImage at native size using only Inflater.
     * Supports 8-bit RGBA (color type 6), non-interlaced, filters 0-4.
     */
    private static WritableImage decodePng(byte[] png, String name) {
        int offset = 0;

        if (png.length < 8 || 
            png[0] != (byte)0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G' ||
            png[4] != 0x0D || png[5] != 0x0A || png[6] != 0x1A || png[7] != 0x0A) {
            preloadError = "invalid PNG signature";
            return null;
        }
        offset = 8;

        int width = 0, height = 0, bitDepth = 0, colorType = 0, interlace = 0;
        byte[] compressedData = new byte[png.length];
        int compressedLen = 0;

        while (offset + 8 <= png.length) {
            int chunkLen = readInt(png, offset);
            offset += 4;
            String chunkType = new String(png, offset, 4);
            offset += 4;

            if (offset + chunkLen + 4 > png.length) break;

            if ("IHDR".equals(chunkType) && chunkLen >= 13) {
                width = readInt(png, offset);
                height = readInt(png, offset + 4);
                bitDepth = png[offset + 8] & 0xFF;
                colorType = png[offset + 9] & 0xFF;
                interlace = png[offset + 12] & 0xFF;

                if (bitDepth != 8 || colorType != 6 || interlace != 0) {
                    preloadError = "unsupported format (need 8-bit RGBA non-interlaced)";
                    return null;
                }
            } else if ("IDAT".equals(chunkType)) {
                System.arraycopy(png, offset, compressedData, compressedLen, chunkLen);
                compressedLen += chunkLen;
            } else if ("IEND".equals(chunkType)) {
                break;
            }

            offset += chunkLen + 4;
        }

        if (width == 0 || height == 0) {
            preloadError = "no IHDR found";
            return null;
        }

        byte[] inflated;
        try {
            inflated = inflate(compressedData, compressedLen, width, height);
        } catch (DataFormatException e) {
            preloadError = "inflate failed: " + e.getMessage();
            return null;
        }

        int bytesPerPixel = 4;
        int scanlineLen = width * bytesPerPixel;
        int expectedLen = height * (scanlineLen + 1);
        if (inflated.length < expectedLen) {
            preloadError = "inflated data too short";
            return null;
        }

        byte[] unfiltered = new byte[height * scanlineLen];
        try {
            unfilter(inflated, unfiltered, width, height, bytesPerPixel);
        } catch (Exception e) {
            preloadError = "unfilter failed: " + e.getMessage();
            return null;
        }

        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();

        for (int y = 0; y < height; y++) {
            int rowStart = y * scanlineLen;
            for (int x = 0; x < width; x++) {
                int px = rowStart + x * 4;
                int r = unfiltered[px] & 0xFF;
                int g = unfiltered[px + 1] & 0xFF;
                int b = unfiltered[px + 2] & 0xFF;
                int a = unfiltered[px + 3] & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                pw.setArgb(x, y, argb);
            }
        }

        return img;
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
               ((data[offset + 1] & 0xFF) << 16) |
               ((data[offset + 2] & 0xFF) << 8) |
               (data[offset + 3] & 0xFF);
    }

    private static byte[] inflate(byte[] compressed, int len, int width, int height) throws DataFormatException {
        int bytesPerPixel = 4;
        int maxOutput = height * (width * bytesPerPixel + 1) + 1024;
        byte[] output = new byte[maxOutput];

        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed, 0, len);
            int total = 0;
            while (!inflater.finished()) {
                int count = inflater.inflate(output, total, output.length - total);
                if (count == 0 && inflater.needsInput()) break;
                total += count;
            }
            byte[] result = new byte[total];
            System.arraycopy(output, 0, result, 0, total);
            return result;
        } finally {
            inflater.end();
        }
    }

    private static void unfilter(byte[] src, byte[] dst, int width, int height, int bpp) {
        int scanlineLen = width * bpp;
        int srcOff = 0;
        int dstOff = 0;

        for (int y = 0; y < height; y++) {
            int filterType = src[srcOff++] & 0xFF;
            int prevRow = (y == 0) ? -1 : dstOff - scanlineLen;

            for (int x = 0; x < scanlineLen; x++) {
                int raw = src[srcOff++] & 0xFF;
                int a = (x >= bpp) ? (dst[dstOff - bpp] & 0xFF) : 0;
                int b = (prevRow >= 0) ? (dst[prevRow + x] & 0xFF) : 0;
                int c = (x >= bpp && prevRow >= 0) ? (dst[prevRow + x - bpp] & 0xFF) : 0;

                int result;
                switch (filterType) {
                    case 0: result = raw; break;
                    case 1: result = raw + a; break;
                    case 2: result = raw + b; break;
                    case 3: result = raw + ((a + b) / 2); break;
                    case 4: result = raw + paeth(a, b, c); break;
                    default: result = raw;
                }
                dst[dstOff++] = (byte)(result & 0xFF);
            }
        }
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) return a;
        if (pb <= pc) return b;
        return c;
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
