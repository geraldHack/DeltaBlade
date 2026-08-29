package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;

public class BulletComponent extends Component {
    
    private static final double MAX_TPF = 1.0 / 30.0;
    
    private double speedY;
    private double speedX;
    private boolean isPlayerBullet;
    
    public BulletComponent(double speedY, boolean isPlayerBullet) {
        this(speedY, 0, isPlayerBullet);
    }
    
    public BulletComponent(double speedY, double speedX, boolean isPlayerBullet) {
        this.speedY = speedY;
        this.speedX = speedX;
        this.isPlayerBullet = isPlayerBullet;
    }
    
    @Override
    public void onUpdate(double tpf) {
        tpf = Math.min(tpf, MAX_TPF);
        entity.translateY(speedY * tpf);
        entity.translateX(speedX * tpf);
        
        boolean outOfBounds = entity.getY() < -20 
                || entity.getY() > FXGL.getAppHeight() + 20
                || entity.getX() < -20 
                || entity.getX() > FXGL.getAppWidth() + 20;
        
        if (outOfBounds) {
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
