package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.EntityType;
import deltablade.GameVars;

public class BulletComponent extends Component {

    private double speedX;
    private double speedY;
    private final boolean isPlayerBullet;
    private final boolean homing;
    private final double turnRate;
    private final double speed;
    private double acquireDelay;
    private double homingLeft;

    /** Delay before the missile starts turning. */
    private static final double HOMING_ACQUIRE = 0.10;
    /** How long it may steer after lock-on. Then it flies straight. */
    private static final double HOMING_DURATION = 1.45;
    /** Stop steering in the last stretch above the player. */
    private static final double HOMING_CUTOFF_Y_RATIO = 0.84;

    public BulletComponent(double speedY, boolean isPlayerBullet) {
        this(0, speedY, isPlayerBullet, false);
    }

    public BulletComponent(double speedX, double speedY, boolean isPlayerBullet) {
        this(speedX, speedY, isPlayerBullet, false);
    }

    public BulletComponent(double speedX, double speedY, boolean isPlayerBullet, boolean homing) {
        this.speedX = speedX;
        this.speedY = speedY;
        this.isPlayerBullet = isPlayerBullet;
        this.homing = homing;
        this.turnRate = 0.82;
        this.speed = Math.hypot(speedX, speedY);
        this.acquireDelay = homing ? HOMING_ACQUIRE : 0;
        this.homingLeft = homing ? HOMING_DURATION : 0;
    }

    private static final double MAX_TPF = 1.0 / 30.0;

    @Override
    public void onUpdate(double tpf) {
        tpf = Math.min(tpf, MAX_TPF);

        if (homing && !isPlayerBullet) {
            if (acquireDelay > 0) {
                acquireDelay -= tpf;
            } else if (homingLeft > 0) {
                if (entity.getY() >= FXGL.getAppHeight() * HOMING_CUTOFF_Y_RATIO) {
                    homingLeft = 0;
                } else {
                    homingLeft -= tpf;
                    steerTowardPlayer(tpf);
                }
            }
        }

        entity.translateX(speedX * tpf);
        entity.translateY(speedY * tpf);

        if (homing || Math.abs(speedX) > 8) {
            entity.setRotation(Math.toDegrees(Math.atan2(speedX, -speedY)));
        }

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

    private void steerTowardPlayer(double tpf) {
        try {
            var player = FXGL.getGameWorld().getSingleton(EntityType.PLAYER);
            double dx = player.getX() + player.getWidth() / 2 - (entity.getX() + entity.getWidth() / 2);
            double dy = player.getY() + player.getHeight() / 2 - (entity.getY() + entity.getHeight() / 2);
            double dist = Math.hypot(dx, dy);
            if (dist < 4) {
                return;
            }
            dx /= dist;
            dy /= dist;
            speedX += dx * turnRate * tpf * speed;
            speedY += dy * turnRate * tpf * speed;
            double n = Math.hypot(speedX, speedY);
            if (n > 1) {
                speedX = speedX / n * speed;
                speedY = speedY / n * speed;
            }
        } catch (Exception ignored) {
            // Player may be gone during reset.
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
