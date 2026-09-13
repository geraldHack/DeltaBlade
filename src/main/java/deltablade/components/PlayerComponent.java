package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;
import javafx.scene.Group;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PlayerComponent extends Component {
    
    private static final double MAX_TPF = 1.0 / 30.0;
    private static final int SHIP_SIZE = 48;
    private static final int SHIELD_SIZE = 64;
    private double speed = 230;
    private boolean invulnerable = false;
    private double invulnerableTimer = 0;
    private static final double INVULNERABLE_DURATION = 2.0;
    private double shieldTimer = 0;
    private Group shieldView;
    private int scoopCharges = 0;
    
    private double fireCooldown = 0;
    private static final double FIRE_RATE = 0.10;
    
    private boolean wasMovingLeft = false;
    private boolean wasMovingRight = false;
    private double fireAnimTimer = 0;
    private static final double FIRE_ANIM_DURATION = 0.36;
    
    @Override
    public void onAdded() {
        double radius = SHIELD_SIZE / 2.0;
        Circle fill = new Circle(radius, Color.rgb(80, 220, 255, 0.12));
        Circle ring = new Circle(radius);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.rgb(120, 240, 255));
        ring.setStrokeWidth(2.4);
        ring.setEffect(new Glow(0.55));
        shieldView = new Group(fill, ring);
        shieldView.setTranslateX(SHIP_SIZE / 2.0);
        shieldView.setTranslateY(SHIP_SIZE / 2.0);
        shieldView.setMouseTransparent(true);
        shieldView.setVisible(false);
        entity.getViewComponent().addChild(shieldView);
    }

    @Override
    public void onUpdate(double tpf) {
        tpf = Math.min(tpf, MAX_TPF);
        
        if (fireCooldown > 0) {
            fireCooldown -= tpf;
        }
        if (fireAnimTimer > 0) {
            fireAnimTimer -= tpf;
        }

        if (shieldTimer > 0) {
            shieldTimer = Math.max(0, shieldTimer - tpf);
        }
        updateShieldView();
        
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
            if (fireAnimTimer > 0) {
                setAnimationState(PlayerAnimationComponent.AnimationState.FIRING);
            } else {
                setAnimationState(PlayerAnimationComponent.AnimationState.IDLE);
            }
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
        fireAnimTimer = FIRE_ANIM_DURATION;
        if (!wasMovingLeft && !wasMovingRight) {
            setAnimationState(PlayerAnimationComponent.AnimationState.FIRING);
        }
    }
    
    public void makeInvulnerable() {
        invulnerable = true;
        invulnerableTimer = INVULNERABLE_DURATION;
    }
    
    public boolean isInvulnerable() {
        return invulnerable || isShielded();
    }

    public void addShield(double seconds) {
        shieldTimer += Math.max(0, seconds);
        updateShieldView();
    }

    public boolean isShielded() {
        return shieldTimer > 0;
    }

    public double getShieldRemaining() {
        return shieldTimer;
    }

    public void clearShield() {
        shieldTimer = 0;
        updateShieldView();
    }

    public void attachScoops() {
        scoopCharges = GameVars.SCOOP_COUNT;
        syncScoopVisual();
    }

    public void clearScoops() {
        scoopCharges = 0;
        syncScoopVisual();
    }

    public boolean isScoopActive() {
        return scoopCharges > 0;
    }

    public int getScoopCharges() {
        return scoopCharges;
    }

    private void syncScoopVisual() {
        if (entity != null && entity.hasComponent(PlayerAnimationComponent.class)) {
            entity.getComponent(PlayerAnimationComponent.class).setScooping(scoopCharges > 0);
        }
    }

    private void updateShieldView() {
        if (shieldView == null) {
            return;
        }
        boolean on = shieldTimer > 0;
        shieldView.setVisible(on);
        if (on) {
            double pulse = 0.72 + 0.28 * (0.5 + 0.5 * Math.sin(shieldTimer * 8));
            if (shieldTimer <= 3) {
                pulse = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(shieldTimer * 14));
            }
            shieldView.setOpacity(pulse);
        }
    }
    
    public double getCenterX() {
        return entity.getX() + entity.getWidth() / 2;
    }
    
    public double getTopY() {
        return entity.getY();
    }
}
