package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.texture.Texture;
import deltablade.components.BulletComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlayerComponent;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.texture;

public class DeltaBladeFactory implements EntityFactory {

    private static final int SHIP_SIZE = 48;
    private static final int BULLET_W = 12;
    private static final int BULLET_H = 20;
    private static final int PICKUP_SIZE = 28;

    /**
     * Load a texture resized to w×h. If FXGL cannot load the asset, return a
     * solid colored shape so we never show the magenta/black missing placeholder.
     */
    private static Node safeTexture(String name, int w, int h, Color fallback) {
        try {
            Texture t = texture(name, w, h);
            if (t == null) {
                return new Rectangle(w, h, fallback);
            }
            return t;
        } catch (Exception e) {
            return new Rectangle(w, h, fallback);
        }
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(safeTexture("player.png", SHIP_SIZE, SHIP_SIZE, Color.DODGERBLUE))
                .zIndex(100)
                .collidable()
                .with(new PlayerComponent())
                .build();
    }

    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        EnemyComponent.EnemyType type = data.get("enemyType");
        int level = data.get("level");

        // Use only textures that reliably load. Variants via multiplyColor.
        // (enemy_fast/tough were showing as magenta missing-texture checkers on Mac.)
        Color fallback = switch (type) {
            case FAST -> Color.LIME;
            case TOUGH -> Color.MEDIUMPURPLE;
            default -> Color.CRIMSON;
        };
        Color tint = switch (type) {
            case FAST -> Color.LIGHTGREEN;
            case TOUGH -> Color.VIOLET;
            default -> Color.WHITE;
        };

        Node view;
        try {
            Texture t = texture("enemy_basic.png", SHIP_SIZE, SHIP_SIZE);
            if (type != EnemyComponent.EnemyType.BASIC) {
                t = t.multiplyColor(tint);
            }
            view = t;
        } catch (Exception e) {
            view = new Rectangle(SHIP_SIZE, SHIP_SIZE, fallback);
        }

        EnemyComponent enemyComponent = new EnemyComponent(type, level);

        if (data.hasKey("entering") && data.<Boolean>get("entering")) {
            double targetX = data.get("targetX");
            double targetY = data.get("targetY");
            WaveManager.EntryPath entryPath = data.get("entryPath");
            int squadId = data.get("squadId");
            enemyComponent.setEntryData(targetX, targetY, entryPath, squadId);
        }

        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY)
                .viewWithBBox(view)
                .zIndex(50)
                .collidable()
                .with(enemyComponent)
                .build();
    }

    @Spawns("playerBullet")
    public Entity newPlayerBullet(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER_BULLET)
                .viewWithBBox(safeTexture("bullet_player.png", BULLET_W, BULLET_H, Color.YELLOW))
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(-500, true))
                .build();
    }

    @Spawns("enemyBullet")
    public Entity newEnemyBullet(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY_BULLET)
                .viewWithBBox(safeTexture("bullet_enemy.png", 10, 16, Color.ORANGERED))
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(250, false))
                .build();
    }

    @Spawns("weaponPickup")
    public Entity newWeaponPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(safeTexture("pickup_weapon.png", PICKUP_SIZE, PICKUP_SIZE, Color.GOLD))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.WEAPON_UPGRADE))
                .build();
    }

    @Spawns("ammoPickup")
    public Entity newAmmoPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(safeTexture("pickup_ammo.png", PICKUP_SIZE, PICKUP_SIZE, Color.CYAN))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.EXTRA_AMMO))
                .build();
    }

    @Spawns("star")
    public Entity newStar(SpawnData data) {
        double size = data.get("size");
        double opacity = data.get("opacity");

        Circle star = new Circle(size / 2);
        star.setFill(Color.WHITE);
        star.setOpacity(opacity);

        return FXGL.entityBuilder(data)
                .view(star)
                .zIndex(-100)
                .build();
    }

    @Spawns("background")
    public Entity newBackground(SpawnData data) {
        int width = data.get("width");
        int height = data.get("height");

        Rectangle bg = new Rectangle(width, height);
        bg.setFill(Color.BLACK);

        return FXGL.entityBuilder(data)
                .at(0, 0)
                .view(bg)
                .zIndex(-1000)
                .build();
    }
}
