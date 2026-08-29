package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;

public class PlayerComponent extends Component {
    
    private double speed = 300;
    private boolean invulnerable = false;
    private double invulnerableTimer = 0;
    private static final double INVULNERABLE_DURATION = 2.0;
    
    private double fireCooldown = 0;
    private static final double FIRE_RATE = 0.15;
    
    @Override
    public void onUpdate(double tpf) {
        if (fireCooldown > 0) {
            fireCooldown -= tpf;
        }
        
        if (invulnerable) {
            invulnerableTimer -= tpf;
            if (invulnerableTimer <= 0) {
                invulnerable = false;
                entity.getViewComponent().setOpacity(1.0);
            } else {
                double flash = Math.sin(invulnerableTimer * 20) > 0 ? 1.0 : 0.3;
                entity.getViewComponent().setOpacity(flash);
            }
        }
    }
    
    public void moveLeft(double tpf) {
        double newX = entity.getX() - speed * tpf;
        if (newX >= 0) {
            entity.setX(newX);
        }
    }
    
    public void moveRight(double tpf) {
        double newX = entity.getX() + speed * tpf;
        double maxX = FXGL.getAppWidth() - entity.getWidth();
        if (newX <= maxX) {
            entity.setX(newX);
        }
    }
    
    public boolean canFire(int grade) {
        if (fireCooldown > 0) return false;
        
        int activeBullets = FXGL.geti(GameVars.ACTIVE_BULLETS);
        int ammoCap = FXGL.geti(GameVars.AMMO_CAP);
        return activeBullets + grade <= ammoCap;
    }
    
    public void onFired() {
        fireCooldown = FIRE_RATE;
    }
    
    public void makeInvulnerable() {
        invulnerable = true;
        invulnerableTimer = INVULNERABLE_DURATION;
    }
    
    public boolean isInvulnerable() {
        return invulnerable;
    }
    
    public double getCenterX() {
        return entity.getX() + entity.getWidth() / 2;
    }
    
    public double getTopY() {
        return entity.getY();
    }
}
