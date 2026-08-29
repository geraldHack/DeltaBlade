package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import deltablade.components.BulletComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.ExtraLetterPickupComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlayerComponent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
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

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeltaBladeFactory implements EntityFactory {

    private static final Logger LOG = Logger.getLogger(DeltaBladeFactory.class.getName());

    private static final int SHIP_SIZE = 48;
    private static final int BULLET_W = 12;
    private static final int BULLET_H = 20;
    private static final int PICKUP_SIZE = 28;

    /**
     * Load a PNG directly from classpath, bypassing FXGL's asset loader entirely.
     * This avoids Mac-specific issues with FXGL.texture() that cause missing-texture placeholders.
     * Returns crisp pixel-art scaled ImageView, or logs loudly and returns a visible error indicator.
     */
    private static Node loadSprite(String filename, int w, int h, Color errorColor) {
        String path = "/assets/textures/" + filename;
        try {
            InputStream is = DeltaBladeFactory.class.getResourceAsStream(path);
            if (is == null) {
                LOG.log(Level.SEVERE, "SPRITE NOT FOUND: {0} - check classpath!", path);
                return createErrorIndicator(w, h, errorColor, "NOT FOUND: " + filename);
            }
            Image img = new Image(is);
            is.close();
            if (img.isError()) {
                LOG.log(Level.SEVERE, "SPRITE LOAD ERROR: {0} - {1}", new Object[]{path, img.getException()});
                return createErrorIndicator(w, h, errorColor, "ERROR: " + filename);
            }
            ImageView iv = new ImageView(img);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(false);
            iv.setSmooth(false);
            return iv;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "SPRITE EXCEPTION: " + path, e);
            return createErrorIndicator(w, h, errorColor, "EXC: " + filename);
        }
    }

    private static Node createErrorIndicator(int w, int h, Color color, String msg) {
        Rectangle r = new Rectangle(w, h, color);
        r.setStroke(Color.WHITE);
        r.setStrokeWidth(2);
        Text t = new Text("!");
        t.setFill(Color.WHITE);
        t.setFont(Font.font("Monospace", FontWeight.BOLD, Math.min(w, h) / 2));
        t.setTranslateX(w / 2 - 5);
        t.setTranslateY(h / 2 + 5);
        Group g = new Group(r, t);
        LOG.warning("Showing error indicator for: " + msg);
        return g;
    }

    private static Node loadSpriteWithTint(String filename, int w, int h, Color tint, Color errorColor) {
        Node sprite = loadSprite(filename, w, h, errorColor);
        if (sprite instanceof ImageView iv && tint != null && !tint.equals(Color.WHITE)) {
            double hue = (tint.getHue() - Color.WHITE.getHue()) / 360.0;
            double sat = tint.getSaturation();
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setHue(hue);
            colorAdjust.setSaturation(sat * 0.5);
            iv.setEffect(colorAdjust);
        }
        return sprite;
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(loadSprite("player.png", SHIP_SIZE, SHIP_SIZE, Color.DODGERBLUE))
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

        Color tint = switch (type) {
            case FAST -> Color.LIGHTGREEN;
            case TOUGH -> Color.VIOLET;
            default -> Color.WHITE;
        };

        Node view = loadSpriteWithTint(textureName, SHIP_SIZE, SHIP_SIZE, tint, fallback);

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
                .viewWithBBox(loadSprite("bullet_player.png", BULLET_W, BULLET_H, Color.YELLOW))
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(speedX, speedY, true))
                .build();
    }

    @Spawns("enemyBullet")
    public Entity newEnemyBullet(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY_BULLET)
                .viewWithBBox(loadSprite("bullet_enemy.png", 10, 16, Color.ORANGERED))
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(250, false))
                .build();
    }

    @Spawns("weaponPickup")
    public Entity newWeaponPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(loadSprite("pickup_weapon.png", PICKUP_SIZE, PICKUP_SIZE, Color.GOLD))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.WEAPON_UPGRADE))
                .build();
    }

    @Spawns("ammoPickup")
    public Entity newAmmoPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(loadSprite("pickup_ammo.png", PICKUP_SIZE, PICKUP_SIZE, Color.CYAN))
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
        orb.setStroke(Color.WHITE);
        orb.setStrokeWidth(2);
        
        Glow glow = new Glow(0.7);
        DropShadow shadow = new DropShadow(12, orbColor);
        glow.setInput(shadow);
        orb.setEffect(glow);
        
        Text letterText = new Text(String.valueOf(letter));
        letterText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        letterText.setFill(Color.WHITE);
        letterText.setStroke(Color.rgb(0, 0, 0, 0.5));
        letterText.setStrokeWidth(1);
        letterText.setTranslateX(-6);
        letterText.setTranslateY(6);
        
        DropShadow letterShadow = new DropShadow(3, Color.BLACK);
        Glow letterGlow = new Glow(0.4);
        letterGlow.setInput(letterShadow);
        letterText.setEffect(letterGlow);
        
        Group group = new Group(orb, letterText);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.EXTRA_LETTER_PICKUP)
                .viewWithBBox(group)
                .zIndex(65)
                .collidable()
                .with(new ExtraLetterPickupComponent(letter, letterIndex))
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
}
