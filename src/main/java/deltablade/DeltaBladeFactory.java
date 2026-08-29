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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class DeltaBladeFactory implements EntityFactory {
    
    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        Polygon shipBody = new Polygon(
            20, 0,
            0, 30,
            10, 25,
            30, 25,
            40, 30
        );
        shipBody.setFill(Color.DODGERBLUE);
        shipBody.setStroke(Color.CYAN);
        shipBody.setStrokeWidth(2);
        
        Polygon cockpit = new Polygon(
            20, 5,
            12, 18,
            28, 18
        );
        cockpit.setFill(Color.CYAN);
        
        Rectangle engine = new Rectangle(17, 22, 6, 10);
        engine.setFill(Color.ORANGE);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(shipBody)
                .view(cockpit)
                .view(engine)
                .collidable()
                .with(new PlayerComponent())
                .build();
    }
    
    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        EnemyComponent.EnemyType type = data.get("enemyType");
        int level = data.get("level");
        
        double width, height;
        Color mainColor, accentColor;
        
        switch (type) {
            case FAST:
                width = 25;
                height = 20;
                mainColor = Color.LIME;
                accentColor = Color.LIGHTGREEN;
                break;
            case TOUGH:
                width = 40;
                height = 35;
                mainColor = Color.PURPLE;
                accentColor = Color.VIOLET;
                break;
            default:
                width = 30;
                height = 25;
                mainColor = Color.CRIMSON;
                accentColor = Color.ORANGERED;
        }
        
        Polygon body = new Polygon(
            width / 2, height,
            0, 0,
            5, height * 0.7,
            width - 5, height * 0.7,
            width, 0
        );
        body.setFill(mainColor);
        body.setStroke(accentColor);
        body.setStrokeWidth(1);
        
        Circle eye = new Circle(width / 2, 8, 5);
        eye.setFill(Color.YELLOW);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY)
                .viewWithBBox(body)
                .view(eye)
                .collidable()
                .with(new EnemyComponent(type, level))
                .build();
    }
    
    @Spawns("playerBullet")
    public Entity newPlayerBullet(SpawnData data) {
        Rectangle bullet = new Rectangle(4, 14);
        bullet.setFill(Color.YELLOW);
        
        Rectangle glow = new Rectangle(2, 10);
        glow.setFill(Color.WHITE);
        glow.setTranslateX(1);
        glow.setTranslateY(2);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER_BULLET)
                .viewWithBBox(bullet)
                .view(glow)
                .collidable()
                .with(new BulletComponent(-500, true))
                .build();
    }
    
    @Spawns("enemyBullet")
    public Entity newEnemyBullet(SpawnData data) {
        Circle bullet = new Circle(4);
        bullet.setFill(Color.RED);
        
        Circle core = new Circle(2);
        core.setFill(Color.ORANGE);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY_BULLET)
                .viewWithBBox(bullet)
                .view(core)
                .collidable()
                .with(new BulletComponent(250, false))
                .build();
    }
    
    @Spawns("weaponPickup")
    public Entity newWeaponPickup(SpawnData data) {
        Rectangle box = new Rectangle(20, 20);
        box.setFill(Color.GOLD);
        box.setStroke(Color.ORANGE);
        box.setStrokeWidth(2);
        
        Polygon arrow = new Polygon(
            10, 3,
            5, 12,
            8, 12,
            8, 17,
            12, 17,
            12, 12,
            15, 12
        );
        arrow.setFill(Color.WHITE);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(box)
                .view(arrow)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.WEAPON_UPGRADE))
                .build();
    }
    
    @Spawns("ammoPickup")
    public Entity newAmmoPickup(SpawnData data) {
        Circle circle = new Circle(10);
        circle.setFill(Color.CYAN);
        circle.setStroke(Color.DEEPSKYBLUE);
        circle.setStrokeWidth(2);
        
        Rectangle plus1 = new Rectangle(3, 12);
        plus1.setFill(Color.WHITE);
        plus1.setTranslateX(8.5);
        plus1.setTranslateY(4);
        
        Rectangle plus2 = new Rectangle(12, 3);
        plus2.setFill(Color.WHITE);
        plus2.setTranslateX(4);
        plus2.setTranslateY(8.5);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(circle)
                .view(plus1)
                .view(plus2)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.EXTRA_AMMO))
                .build();
    }
}
