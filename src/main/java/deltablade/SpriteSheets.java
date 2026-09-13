package deltablade;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SpriteSheets {

    public static final String SHIPS = "fightersxy.png";
    public static final String MONSTERS = "fightersalienz.png";
    public static final int CELL = 64;
    public static final int SHIP_TILES = 24;
    public static final int MONSTER_TILES = 5;

    private static final Map<String, Image> TILES = new ConcurrentHashMap<>();

    private SpriteSheets() {}

    public static Image tile(String sheet, int index) {
        String key = sheet + "#" + index;
        Image cached = TILES.get(key);
        if (cached != null) {
            return cached;
        }
        Image src = EmbeddedTextures.getImage(sheet, 512, 512);
        if (src == null || src.getPixelReader() == null) {
            return null;
        }
        int col = Math.floorMod(index, 8);
        int row = Math.floorDiv(index, 8);
        PixelReader reader = src.getPixelReader();
        WritableImage tile = new WritableImage(reader, col * CELL, row * CELL, CELL, CELL);
        TILES.put(key, tile);
        return tile;
    }

    public static int shipTileFor(int variant) {
        return shipTileFor(deltablade.components.EnemyComponent.EnemyType.BASIC, variant);
    }

    public static int shipTileFor(deltablade.components.EnemyComponent.EnemyType type, int variant) {
        int[] tiles = switch (type) {
            case FAST -> new int[] {16, 17, 21, 22};
            case TOUGH -> new int[] {18, 19, 20, 23};
            default -> new int[] {16, 17, 18, 19, 20, 21, 22, 23};
        };
        return tiles[Math.floorMod(variant, tiles.length)];
    }

    public static int monsterTileFor(int variant) {
        return Math.floorMod(variant, MONSTER_TILES);
    }
}
