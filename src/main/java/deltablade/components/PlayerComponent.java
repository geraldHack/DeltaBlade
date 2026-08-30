package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;

public class PlayerComponent extends Component {
    
    private static final double MAX_TPF = 1.0 / 30.0;
    private double speed = 230;
    private boolean invulnerable = false;
    private double invulnerableTimer = 0;
    private static final double INVULNERABLE_DURATION = 2.0;
    
    private double fireCooldown = 0;
    private static final double FIRE_RATE = 0.10;
    
    private boolean wasMovingLeft = false;
    private boolean wasMovingRight = false;
    
    @Override
    public void onUpdate(double tpf) {
        tpf = Math.min(tpf, MAX_TPF);
        
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
        double minX = GameVars.RAIL_WIDTH;
        if (newX >= minX) {
            entity.setX(newX);
        } else {
            entity.setX(minX);
        }
        wasMovingLeft = true;
        updateAnimationState();
    }
    
    public void moveRight(double tpf) {
        double newX = entity.getX() + speed * tpf;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - GameVars.RAIL_WIDTH;
        if (newX <= maxX) {
            entity.setX(newX);
        } else {
            entity.setX(maxX);
        }
        wasMovingRight = true;
        updateAnimationState();
    }
    
    public void updateIdle() {
        if (!wasMovingLeft && !wasMovingRight) {
            setAnimationState(PlayerAnimationComponent.AnimationState.IDLE);
        }
        wasMovingLeft = false;
        wasMovingRight = false;
    }
    
    private void updateAnimationState() {
        if (wasMovingLeft && !wasMovingRight) {
            setAnimationState(PlayerAnimationComponent.AnimationState.BANKING_LEFT);
        } else if (wasMovingRight && !wasMovingLeft) {
            setAnimationState(PlayerAnimationComponent.AnimationState.BANKING_RIGHT);
        } else {
            setAnimationState(PlayerAnimationComponent.AnimationState.IDLE);
        }
    }
    
    private void setAnimationState(PlayerAnimationComponent.AnimationState state) {
        if (entity.hasComponent(PlayerAnimationComponent.class)) {
            entity.getComponent(PlayerAnimationComponent.class).setState(state);
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
