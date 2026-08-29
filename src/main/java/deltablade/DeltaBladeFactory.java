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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DeltaBladeFactory implements EntityFactory {

    private static final int SHIP_SIZE = 48;
    private static final int BULLET_W = 12;
    private static final int BULLET_H = 20;
    private static final int PICKUP_SIZE = 28;
    
    private static final ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final Set<String> failedSprites = ConcurrentHashMap.newKeySet();
    private static volatile boolean errorBannerShown = false;
    
    private static final String[] TEXTURE_PATHS = {
        "assets/textures/",
        "/assets/textures/",
        "textures/",
        "/textures/"
    };
    
    private static final String[] FILE_FALLBACKS = {
        "src/main/resources/assets/textures/",
        "target/classes/assets/textures/"
    };

    private static Image loadImage(String filename) {
        if (imageCache.containsKey(filename)) {
            return imageCache.get(filename);
        }
        
        Image img = null;
        
        for (String basePath : TEXTURE_PATHS) {
            String resourcePath = basePath + filename;
            
            try {
                ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
                if (contextCL != null) {
                    URL url = contextCL.getResource(resourcePath);
                    if (url != null) {
                        img = new Image(url.toExternalForm(), false);
                        if (!img.isError() && img.getWidth() > 0) {
                            imageCache.put(filename, img);
                            return img;
                        }
                    }
                    
                    InputStream is = contextCL.getResourceAsStream(resourcePath);
                    if (is != null) {
                        img = new Image(is);
                        is.close();
                        if (!img.isError() && img.getWidth() > 0) {
                            imageCache.put(filename, img);
                            return img;
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            try {
                ClassLoader classCL = DeltaBladeFactory.class.getClassLoader();
                if (classCL != null) {
                    URL url = classCL.getResource(resourcePath);
                    if (url != null) {
                        img = new Image(url.toExternalForm(), false);
                        if (!img.isError() && img.getWidth() > 0) {
                            imageCache.put(filename, img);
                            return img;
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            try {
                URL url = DeltaBladeFactory.class.getResource("/" + resourcePath.replaceFirst("^/", ""));
                if (url != null) {
                    img = new Image(url.toExternalForm(), false);
                    if (!img.isError() && img.getWidth() > 0) {
                        imageCache.put(filename, img);
                        return img;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        String userDir = System.getProperty("user.dir", ".");
        for (String fallbackPath : FILE_FALLBACKS) {
            try {
                File file = new File(userDir, fallbackPath + filename);
                if (file.exists() && file.canRead()) {
                    img = new Image(file.toURI().toString(), false);
                    if (!img.isError() && img.getWidth() > 0) {
                        imageCache.put(filename, img);
                        return img;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        try {
            img = FXGL.getAssetLoader().loadImage(filename);
            if (img != null && !img.isError() && img.getWidth() > 0) {
                boolean isMagentaPlaceholder = img.getWidth() == 64 && img.getHeight() == 64;
                if (!isMagentaPlaceholder) {
                    imageCache.put(filename, img);
                    return img;
                }
            }
        } catch (Exception ignored) {}
        
        return null;
    }
    
    private static void showSpriteErrorBanner(String filename) {
        if (!failedSprites.add(filename)) {
            return;
        }
        
        if (!errorBannerShown) {
            errorBannerShown = true;
            try {
                javafx.application.Platform.runLater(() -> {
                    try {
                        String message = "Sprite nicht geladen: " + String.join(", ", failedSprites);
                        FXGL.getNotificationService().pushNotification(message);
                    } catch (Exception e) {
                        Text errorText = new Text("Sprites fehlen: " + String.join(", ", failedSprites));
                        errorText.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
                        errorText.setFill(Color.ORANGERED);
                        errorText.setTranslateX(FXGL.getAppWidth() / 2 - 120);
                        errorText.setTranslateY(50);
                        FXGL.getGameScene().addUINode(errorText);
                        FXGL.runOnce(() -> FXGL.getGameScene().removeUINode(errorText), 
                                    javafx.util.Duration.seconds(5));
                    }
                });
            } catch (Exception ignored) {}
        }
    }
    
    private static Node loadSprite(String filename, int w, int h, Color fallbackColor) {
        Image img = loadImage(filename);
        
        if (img != null && !img.isError()) {
            ImageView view = new ImageView(img);
            view.setFitWidth(w);
            view.setFitHeight(h);
            view.setSmooth(false);
            view.setPreserveRatio(false);
            return view;
        }
        
        showSpriteErrorBanner(filename);
        
        Rectangle marker = new Rectangle(w * 0.6, h * 0.6, fallbackColor.deriveColor(0, 0.5, 0.7, 0.8));
        marker.setStroke(fallbackColor.darker());
        marker.setStrokeWidth(1);
        marker.setTranslateX(w * 0.2);
        marker.setTranslateY(h * 0.2);
        return marker;
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

        Node view = loadSprite(textureName, SHIP_SIZE, SHIP_SIZE, fallback);

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
        orb.setStroke(orbColor.darker());
        orb.setStrokeWidth(1.5);
        
        Text letterText = new Text(String.valueOf(letter));
        letterText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        letterText.setFill(Color.WHITE);
        letterText.setTranslateX(-6);
        letterText.setTranslateY(6);
        
        Circle clipCircle = new Circle(15);
        Group contentGroup = new Group(orb, letterText);
        contentGroup.setClip(clipCircle);
        
        Group group = new Group(contentGroup);
        
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
