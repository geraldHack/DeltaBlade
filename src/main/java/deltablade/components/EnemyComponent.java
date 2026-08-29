package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

import java.util.Random;

public class EnemyComponent extends Component {
    
    public enum EnemyType {
        BASIC(1, 100, 80, 0.02),
        FAST(1, 150, 120, 0.03),
        TOUGH(3, 300, 60, 0.04);
        
        public final int health;
        public final int scoreValue;
        public final double speed;
        public final double fireRate;
        
        EnemyType(int health, int scoreValue, double speed, double fireRate) {
            this.health = health;
            this.scoreValue = scoreValue;
            this.speed = speed;
            this.fireRate = fireRate;
        }
    }
    
    private EnemyType type;
    private int health;
    private double speedX;
    private double baseY;
    private double hoverPhase;
    private double hoverAmplitude = 15;
    
    private boolean diving = false;
    private double diveSpeed = 150;
    
    private double fireTimer = 0;
    
    private static final Random random = new Random();
    
    public EnemyComponent(EnemyType type, int level) {
        this.type = type;
        this.health = type.health;
        double levelMultiplier = 1 + (level - 1) * 0.12;
        this.speedX = type.speed * levelMultiplier * (random.nextBoolean() ? 1 : -1);
        this.hoverPhase = random.nextDouble() * Math.PI * 2;
        this.diveSpeed = 150 + level * 20;
    }
    
    @Override
    public void onAdded() {
        baseY = entity.getY();
    }
    
    @Override
    public void onUpdate(double tpf) {
        if (!diving) {
            hoverPhase += tpf * 2;
            double hoverY = baseY + Math.sin(hoverPhase) * hoverAmplitude;
            entity.setY(hoverY);
            
            entity.translateX(speedX * tpf);
            
            if (entity.getX() <= 0 || entity.getRightX() >= FXGL.getAppWidth()) {
                speedX = -speedX;
            }
            
            if (random.nextDouble() < 0.001 * (1 + FXGL.geti("level") * 0.2)) {
                diving = true;
            }
        } else {
            entity.translateY(diveSpeed * tpf);
            
            if (entity.getY() > FXGL.getAppHeight()) {
                entity.removeFromWorld();
            }
        }
        
        fireTimer += tpf;
        if (fireTimer > 0.5 && random.nextDouble() < type.fireRate * tpf * 60) {
            fireTimer = 0;
            FXGL.<deltablade.DeltaBladeApp>getAppCast().spawnEnemyBullet(
                entity.getX() + entity.getWidth() / 2,
                entity.getBottomY()
            );
        }
    }
    
    public void hit() {
        health--;
    }
    
    public boolean isDead() {
        return health <= 0;
    }
    
    public int getScoreValue() {
        return type.scoreValue;
    }
    
    public EnemyType getType() {
        return type;
    }
}
