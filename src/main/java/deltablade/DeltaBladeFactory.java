package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import deltablade.components.BulletComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.ExplosionComponent;
import deltablade.components.ExtraLetterPickupComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlayerComponent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class DeltaBladeFactory implements EntityFactory {

    private static final int SHIP_SIZE = 48;
    private static final int BULLET_W = 12;
    private static final int BULLET_H = 20;
    private static final int PICKUP_SIZE = 28;

    /**
     * Load a texture from embedded bytes first (guaranteed to work).
     * Falls back to a triangle polygon if name is missing from embed.
     * NEVER returns a Rectangle - squares are the visual bug indicator.
     */
    private static Node safeTexture(String name, int w, int h, Color fallback) {
        Image img = EmbeddedTextures.getImage(name, w, h);
        if (img != null && !img.isError()) {
            ImageView view = new ImageView(img);
            view.setFitWidth(w);
            view.setFitHeight(h);
            view.setSmooth(false);
            return view;
        }
        return EmbeddedTextures.createFallbackShip(w, fallback);
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

        String textureName = switch (type) {
            case FAST -> "enemy_fast.png";
            case TOUGH -> "enemy_tough.png";
            default -> "enemy_basic.png";
        };

        Color fallback = switch (type) {
            case FAST -> Color.LIME;
            case TOUGH -> Color.MEDIUMPURPLE;
            default -> Color.CRIMSON;
        };

        Node view = safeTexture(textureName, SHIP_SIZE, SHIP_SIZE, fallback);

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
        double speedX = data.hasKey("speedX") ? data.<Double>get("speedX") : 0.0;
        double speedY = data.hasKey("speedY") ? data.<Double>get("speedY") : -500.0;
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER_BULLET)
                .viewWithBBox(safeTexture("bullet_player.png", BULLET_W, BULLET_H, Color.YELLOW))
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(speedX, speedY, true))
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

    @Spawns("extraLetterOrb")
    public Entity newExtraLetterOrb(SpawnData data) {
        char letter = data.get("letter");
        int letterIndex = data.get("letterIndex");
        
        Color[] letterColors = {
            Color.rgb(255, 100, 100),
            Color.rgb(100, 255, 100),
            Color.rgb(100, 200, 255),
            Color.rgb(255, 200, 100),
            Color.rgb(200, 100, 255)
        };
        
        Color orbColor = letterColors[letterIndex % letterColors.length];
        Color innerColor = orbColor.brighter().brighter();
        
        RadialGradient gradient = new RadialGradient(
            0, 0, 0.25, 0.25, 0.9, true, CycleMethod.NO_CYCLE,
            new Stop(0, innerColor),
            new Stop(0.4, orbColor.brighter()),
            new Stop(0.7, orbColor),
            new Stop(1, orbColor.darker().darker())
        );
        
        Circle orb = new Circle(16);
        orb.setFill(gradient);
        
        Text letterText = new Text(String.valueOf(letter));
        letterText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        letterText.setFill(Color.WHITE);
        letterText.setTranslateX(-6);
        letterText.setTranslateY(6);
        
        ExtraLetterPickupComponent component = new ExtraLetterPickupComponent(letter, letterIndex, letterText);
        Group flipWrapper = component.getFlipWrapper();
        
        Group group = new Group(orb, flipWrapper != null ? flipWrapper : letterText);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.EXTRA_LETTER_PICKUP)
                .viewWithBBox(group)
                .zIndex(65)
                .collidable()
                .with(component)
                .build();
    }

    @Spawns("sideRail")
    public Entity newSideRail(SpawnData data) {
        int width = GameVars.RAIL_WIDTH;
        int height = data.get("height");
        boolean isLeft = data.get("isLeft");

        LinearGradient metalGradient;
        if (isLeft) {
            metalGradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(40, 45, 55)),
                    new Stop(0.2, Color.rgb(70, 80, 95)),
                    new Stop(0.5, Color.rgb(90, 100, 115)),
                    new Stop(0.7, Color.rgb(60, 70, 85)),
                    new Stop(1, Color.rgb(30, 35, 45)));
        } else {
            metalGradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(30, 35, 45)),
                    new Stop(0.3, Color.rgb(60, 70, 85)),
                    new Stop(0.5, Color.rgb(90, 100, 115)),
                    new Stop(0.8, Color.rgb(70, 80, 95)),
                    new Stop(1, Color.rgb(40, 45, 55)));
        }

        Rectangle rail = new Rectangle(width, height);
        rail.setFill(metalGradient);
        
        Rectangle innerBorder = new Rectangle(2, height - 4);
        innerBorder.setFill(Color.rgb(50, 55, 65));
        innerBorder.setX(isLeft ? width - 4 : 2);
        innerBorder.setY(2);
        
        Rectangle highlight = new Rectangle(1, height);
        highlight.setFill(Color.rgb(120, 130, 150, 0.5));
        highlight.setX(isLeft ? 3 : width - 4);

        Group railGroup = new Group(rail, innerBorder, highlight);

        return FXGL.entityBuilder(data)
                .view(railGroup)
                .zIndex(200)
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

    /**
     * Spawns an explosion effect at the given position.
     * Data keys:
     *   - "size": String - "hit" (small), "ship" (medium), or "big" (boss)
     */
    @Spawns("explosion")
    public Entity newExplosion(SpawnData data) {
        String size = data.hasKey("size") ? data.<String>get("size") : "ship";

        String textureName = switch (size) {
            case "hit" -> "explosion_hit.png";
            case "big" -> "explosion_big.png";
            default -> "explosion_ship.png";
        };

        double duration = switch (size) {
            case "hit" -> 0.4;
            case "big" -> 0.8;
            default -> 0.6;
        };

        int frameSize = 64;
        int frameCount = 8;

        Image spriteSheet = EmbeddedTextures.getImage(textureName, frameSize * frameCount, frameSize);
        if (spriteSheet == null || spriteSheet.isError()) {
            return FXGL.entityBuilder(data).build();
        }

        ExplosionComponent explosionComp = new ExplosionComponent(
                spriteSheet, frameCount, frameSize, frameSize, duration);

        return FXGL.entityBuilder(data)
                .view(explosionComp.getView())
                .zIndex(90)
                .with(explosionComp)
                .build();
    }
}
