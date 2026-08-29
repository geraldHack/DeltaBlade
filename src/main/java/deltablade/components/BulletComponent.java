package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;

public class BulletComponent extends Component {
    
    private double speed;
    private boolean isPlayerBullet;
    
    public BulletComponent(double speed, boolean isPlayerBullet) {
        this.speed = speed;
        this.isPlayerBullet = isPlayerBullet;
    }
    
    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);
        
        if (entity.getY() < -20 || entity.getY() > FXGL.getAppHeight() + 20) {
            if (isPlayerBullet) {
                int active = FXGL.geti(GameVars.ACTIVE_BULLETS);
                FXGL.set(GameVars.ACTIVE_BULLETS, Math.max(0, active - 1));
            }
            entity.removeFromWorld();
        }
    }
    
    public boolean isPlayerBullet() {
        return isPlayerBullet;
    }
    
    public void onHit() {
        if (isPlayerBullet) {
            int active = FXGL.geti(GameVars.ACTIVE_BULLETS);
            FXGL.set(GameVars.ACTIVE_BULLETS, Math.max(0, active - 1));
        }
    }
}
