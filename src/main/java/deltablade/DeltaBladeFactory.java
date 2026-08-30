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
import deltablade.components.StarComponent;
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
    private static final int BOSS_SIZE = 80;
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

        boolean isBoss = (type == EnemyComponent.EnemyType.BOSS);
        int size = isBoss ? BOSS_SIZE : SHIP_SIZE;

        String textureName = switch (type) {
            case FAST -> "enemy_fast.png";
            case TOUGH -> "enemy_tough.png";
            case BOSS -> "enemy_tough.png";
            default -> "enemy_basic.png";
        };

        Color fallback = switch (type) {
            case FAST -> Color.LIME;
            case TOUGH -> Color.MEDIUMPURPLE;
            case BOSS -> Color.DARKVIOLET;
            default -> Color.CRIMSON;
        };

        Node view = safeTexture(textureName, size, size, fallback);

        EnemyComponent enemyComponent = new EnemyComponent(type, level);

        if (data.hasKey("kamikaze") && data.<Boolean>get("kamikaze")) {
            enemyComponent.setKamikaze(true);
        }

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

    @Spawns("autofirePickup")
    public Entity newAutofirePickup(SpawnData data) {
        Circle outer = new Circle(14);
        outer.setFill(new RadialGradient(
            0, 0, 0.3, 0.3, 0.9, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.WHITE),
            new Stop(0.4, Color.CYAN),
            new Stop(0.8, Color.DARKCYAN),
            new Stop(1, Color.rgb(0, 80, 100))
        ));
        
        Text autoText = new Text("A");
        autoText.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        autoText.setFill(Color.WHITE);
        autoText.setTranslateX(-5);
        autoText.setTranslateY(5);
        
        Group pickup = new Group(outer, autoText);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(pickup)
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.AUTOFIRE))
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

    @Spawns("scrollingStar")
    public Entity newScrollingStar(SpawnData data) {
        double size = data.get("size");
        double opacity = data.get("opacity");
        double scrollSpeed = data.get("scrollSpeed");
        boolean isNear = data.hasKey("isNear") && data.<Boolean>get("isNear");
        
        Circle star = new Circle(size / 2);
        
        if (isNear && size > 2.5) {
            RadialGradient flare = new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.3, Color.rgb(200, 220, 255, 0.9)),
                new Stop(0.7, Color.rgb(150, 180, 255, 0.5)),
                new Stop(1, Color.TRANSPARENT)
            );
            star.setFill(flare);
        } else {
            Color starColor = isNear ? Color.rgb(220, 230, 255) : Color.rgb(180, 180, 200);
            star.setFill(starColor);
        }
        star.setOpacity(opacity);
        
        double twinkleSpeed = 1.5 + Math.random() * 2.0;
        double twinkleAmount = 0.15 + Math.random() * 0.15;
        
        return FXGL.entityBuilder(data)
                .view(star)
                .zIndex(isNear ? -90 : -100)
                .with(new StarComponent(star, scrollSpeed, opacity, twinkleSpeed, twinkleAmount))
                .build();
    }

    @Spawns("nebula")
    public Entity newNebula(SpawnData data) {
        double radius = data.get("radius");
        Color color = data.get("color");
        double opacity = data.get("opacity");
        
        RadialGradient gradient = new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 0.8)),
            new Stop(0.4, Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 0.4)),
            new Stop(0.7, Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 0.15)),
            new Stop(1, Color.TRANSPARENT)
        );
        
        Circle blob = new Circle(radius);
        blob.setFill(gradient);
        
        return FXGL.entityBuilder(data)
                .view(blob)
                .zIndex(-500)
                .build();
    }

    @Spawns("distantPlanet")
    public Entity newDistantPlanet(SpawnData data) {
        double radius = data.get("radius");
        
        RadialGradient planetGradient = new RadialGradient(
            -45, 0.3, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(60, 50, 80)),
            new Stop(0.5, Color.rgb(40, 35, 55)),
            new Stop(0.8, Color.rgb(25, 20, 35)),
            new Stop(1, Color.rgb(15, 12, 22))
        );
        
        Circle planet = new Circle(radius);
        planet.setFill(planetGradient);
        planet.setOpacity(0.6);
        
        return FXGL.entityBuilder(data)
                .view(planet)
                .zIndex(-400)
                .build();
    }

    @Spawns("background")
    public Entity newBackground(SpawnData data) {
        int width = data.get("width");
        int height = data.get("height");

        Rectangle bg = new Rectangle(width, height);
        bg.setFill(Color.rgb(5, 5, 16));

        return FXGL.entityBuilder(data)
                .at(0, 0)
                .view(bg)
                .zIndex(-1000)
                .build();
    }

    /**
     * Spawns an explosion effect at the given position.
     * Data keys:
     *   - "size": String - "hit" (small sparks), "ship" (medium), or "big" (boss, 2-row grid)
     * The explosion is centered on the spawn position (x,y is center, not top-left).
     */
    @Spawns("explosion")
    public Entity newExplosion(SpawnData data) {
        String size = data.hasKey("size") ? data.<String>get("size") : "ship";

        String textureName;
        int frameCount;
        int frameWidth;
        int frameHeight;
        int columns;
        int sheetWidth;
        int sheetHeight;
        double duration;
        int displayW;
        int displayH;

        int zIndex = 90;
        switch (size) {
            case "hit" -> {
                textureName = "explosion_hit.png";
                frameCount = 6;
                frameWidth = 32;
                frameHeight = 32;
                columns = 6;
                sheetWidth = 192;
                sheetHeight = 32;
                duration = 0.35;
                displayW = 80;
                displayH = 80;
            }
            case "boss_hit" -> {
                textureName = "explosion_hit.png";
                frameCount = 6;
                frameWidth = 32;
                frameHeight = 32;
                columns = 6;
                sheetWidth = 192;
                sheetHeight = 32;
                duration = 0.45;
                displayW = 150;
                displayH = 150;
                zIndex = 150;
            }
            case "big" -> {
                textureName = "explosion_big.png";
                frameCount = 10;
                frameWidth = 64;
                frameHeight = 64;
                columns = 8;
                sheetWidth = 512;
                sheetHeight = 128;
                duration = 0.9;
                displayW = 280;
                displayH = 280;
            }
            default -> {
                textureName = "explosion_ship.png";
                frameCount = 8;
                frameWidth = 64;
                frameHeight = 64;
                columns = 8;
                sheetWidth = 512;
                sheetHeight = 64;
                duration = 0.7;
                displayW = 192;
                displayH = 192;
            }
        }

        Image spriteSheet = EmbeddedTextures.getImage(textureName, sheetWidth, sheetHeight);
        if (spriteSheet == null || spriteSheet.isError()) {
            return createFallbackExplosion(data, size);
        }

        ExplosionComponent explosionComp = new ExplosionComponent(
                spriteSheet, frameCount, frameWidth, frameHeight, columns, duration);

        explosionComp.setDisplaySize(displayW, displayH);

        ImageView view = explosionComp.getView();
        view.setTranslateX(-displayW / 2.0);
        view.setTranslateY(-displayH / 2.0);

        return FXGL.entityBuilder(data)
                .view(view)
                .zIndex(zIndex)
                .with(explosionComp)
                .build();
    }

    private Entity createFallbackExplosion(SpawnData data, String size) {
        int displaySize;
        double durationSec;
        switch (size) {
            case "hit" -> { displaySize = 80; durationSec = 0.35; }
            case "big" -> { displaySize = 280; durationSec = 0.9; }
            default -> { displaySize = 192; durationSec = 0.7; }
        }
        
        Group circleGroup = new Group();
        double center = displaySize / 2.0;

        Circle c1 = new Circle(center * 0.6);
        c1.setFill(Color.ORANGE);
        c1.setCenterX(0);
        c1.setCenterY(0);

        Circle c2 = new Circle(center * 0.4);
        c2.setFill(Color.YELLOW);
        c2.setCenterX(-center * 0.2);
        c2.setCenterY(-center * 0.15);

        Circle c3 = new Circle(center * 0.25);
        c3.setFill(Color.WHITE);
        c3.setCenterX(center * 0.15);
        c3.setCenterY(center * 0.1);

        circleGroup.getChildren().addAll(c1, c2, c3);

        Entity entity = FXGL.entityBuilder(data)
                .view(circleGroup)
                .zIndex(90)
                .build();

        FXGL.getGameTimer().runOnceAfter(() -> {
            if (entity.isActive()) {
                entity.removeFromWorld();
            }
        }, javafx.util.Duration.seconds(durationSec));

        return entity;
    }
}
