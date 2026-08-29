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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.texture;

public class DeltaBladeFactory implements EntityFactory {
    
    private static final double SPRITE_SCALE = 2.5;
    
    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        Texture playerTexture = texture("player.png");
        playerTexture.setScaleX(SPRITE_SCALE);
        playerTexture.setScaleY(SPRITE_SCALE);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(playerTexture)
                .zIndex(100)
                .collidable()
                .with(new PlayerComponent())
                .build();
    }
    
    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        EnemyComponent.EnemyType type = data.get("enemyType");
        int level = data.get("level");
        
        String textureName;
        switch (type) {
            case FAST:
                textureName = "enemy_fast.png";
                break;
            case TOUGH:
                textureName = "enemy_tough.png";
                break;
            default:
                textureName = "enemy_basic.png";
        }
        
        Texture enemyTexture = texture(textureName);
        enemyTexture.setScaleX(SPRITE_SCALE);
        enemyTexture.setScaleY(SPRITE_SCALE);
        
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
                .viewWithBBox(enemyTexture)
                .zIndex(50)
                .collidable()
                .with(enemyComponent)
                .build();
    }
    
    @Spawns("playerBullet")
    public Entity newPlayerBullet(SpawnData data) {
        Texture bulletTexture = texture("bullet_player.png");
        bulletTexture.setScaleX(SPRITE_SCALE);
        bulletTexture.setScaleY(SPRITE_SCALE);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER_BULLET)
                .viewWithBBox(bulletTexture)
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(-500, true))
                .build();
    }
    
    @Spawns("enemyBullet")
    public Entity newEnemyBullet(SpawnData data) {
        Texture bulletTexture = texture("bullet_enemy.png");
        bulletTexture.setScaleX(SPRITE_SCALE);
        bulletTexture.setScaleY(SPRITE_SCALE);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY_BULLET)
                .viewWithBBox(bulletTexture)
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(250, false))
                .build();
    }
    
    @Spawns("weaponPickup")
    public Entity newWeaponPickup(SpawnData data) {
        Texture pickupTexture = texture("pickup_weapon.png");
        pickupTexture.setScaleX(SPRITE_SCALE);
        pickupTexture.setScaleY(SPRITE_SCALE);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(pickupTexture)
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.WEAPON_UPGRADE))
                .build();
    }
    
    @Spawns("ammoPickup")
    public Entity newAmmoPickup(SpawnData data) {
        Texture pickupTexture = texture("pickup_ammo.png");
        pickupTexture.setScaleX(SPRITE_SCALE);
        pickupTexture.setScaleY(SPRITE_SCALE);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(pickupTexture)
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
