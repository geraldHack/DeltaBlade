package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import deltablade.components.BulletComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlayerComponent;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.texture;

public class DeltaBladeFactory implements EntityFactory {
    
    private static final int SPRITE_SIZE = 80;
    
    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(texture("player.png", SPRITE_SIZE, SPRITE_SIZE))
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
                .viewWithBBox(texture(textureName, SPRITE_SIZE, SPRITE_SIZE))
                .zIndex(50)
                .collidable()
                .with(enemyComponent)
                .build();
    }
    
    @Spawns("playerBullet")
    public Entity newPlayerBullet(SpawnData data) {
        double speedX = data.hasKey("speedX") ? data.<Double>get("speedX") : 0.0;
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER_BULLET)
                .viewWithBBox(texture("bullet_player.png", 20, 32))
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(-500, speedX, true))
                .build();
    }
    
    @Spawns("enemyBullet")
    public Entity newEnemyBullet(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY_BULLET)
                .viewWithBBox(texture("bullet_enemy.png", 16, 24))
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(250, false))
                .build();
    }
    
    @Spawns("weaponPickup")
    public Entity newWeaponPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(texture("pickup_weapon.png", 32, 32))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.WEAPON_UPGRADE))
                .build();
    }
    
    @Spawns("ammoPickup")
    public Entity newAmmoPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(texture("pickup_ammo.png", 32, 32))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.EXTRA_AMMO))
                .build();
    }
    
    @Spawns("lifePickup")
    public Entity newLifePickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(texture("heart.png", 32, 32))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.EXTRA_LIFE))
                .build();
    }
    
    @Spawns("sideRail")
    public Entity newSideRail(SpawnData data) {
        int width = data.get("width");
        int height = data.get("height");
        boolean isLeft = data.get("isLeft");
        
        Rectangle rail = new Rectangle(width, height);
        
        LinearGradient gradient;
        if (isLeft) {
            gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(40, 60, 80)),
                    new Stop(0.3, Color.rgb(60, 100, 140)),
                    new Stop(0.7, Color.rgb(80, 140, 180)),
                    new Stop(1.0, Color.rgb(30, 50, 70)));
        } else {
            gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(30, 50, 70)),
                    new Stop(0.3, Color.rgb(80, 140, 180)),
                    new Stop(0.7, Color.rgb(60, 100, 140)),
                    new Stop(1.0, Color.rgb(40, 60, 80)));
        }
        rail.setFill(gradient);
        
        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(100, 180, 255, 0.6));
        glow.setRadius(8);
        glow.setSpread(0.2);
        rail.setEffect(glow);
        
        return FXGL.entityBuilder(data)
                .view(rail)
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
                .viewWithBBox(star)
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
                .viewWithBBox(bg)
                .zIndex(-1000)
                .build();
    }
}
