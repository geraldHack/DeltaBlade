package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.WaveManager;

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
    
    public enum State {
        ENTERING,
        FORMATION,
        DIVING
    }
    
    private EnemyType type;
    private int health;
    private State state = State.ENTERING;
    
    private double targetX;
    private double targetY;
    private WaveManager.EntryPath entryPath;
    private int squadId = -1;
    
    private double entryProgress = 0;
    private double entrySpeed = 200;
    private double entryCurvePhase = 0;
    
    private double speedX;
    private double baseY;
    private double hoverPhase;
    private double hoverAmplitude = 15;
    
    private double diveSpeed = 150;
    private double diveTargetX;
    
    private double fireTimer = 0;
    private double formationHoldTime = 0;
    private double minFormationTime = 2.0;
    
    private static final Random random = new Random();
    
    public EnemyComponent(EnemyType type, int level) {
        this.type = type;
        this.health = type.health;
        double levelMultiplier = 1 + (level - 1) * 0.12;
        this.entrySpeed = 180 + level * 15;
        this.speedX = type.speed * levelMultiplier * (random.nextBoolean() ? 1 : -1);
        this.hoverPhase = random.nextDouble() * Math.PI * 2;
        this.diveSpeed = 150 + level * 20;
    }
    
    public void setEntryData(double targetX, double targetY, WaveManager.EntryPath path, int squadId) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.entryPath = path;
        this.squadId = squadId;
        this.state = State.ENTERING;
        this.entryProgress = 0;
    }
    
    @Override
    public void onAdded() {
        if (entryPath == null) {
            state = State.FORMATION;
            baseY = entity.getY();
        }
    }
    
    @Override
    public void onUpdate(double tpf) {
        switch (state) {
            case ENTERING -> updateEntering(tpf);
            case FORMATION -> updateFormation(tpf);
            case DIVING -> updateDiving(tpf);
        }
        
        if (state != State.ENTERING) {
            updateFiring(tpf);
        }
    }
    
    private void updateEntering(double tpf) {
        entryProgress += tpf * entrySpeed / 300.0;
        entryCurvePhase += tpf * 4;
        
        double currentX = entity.getX();
        double currentY = entity.getY();
        
        double dx = targetX - currentX;
        double dy = targetY - currentY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        if (dist < 5 || entryProgress >= 1.0) {
            entity.setPosition(targetX, targetY);
            state = State.FORMATION;
            baseY = targetY;
            formationHoldTime = 0;
            
            notifySettled();
            return;
        }
        
        double moveSpeed = entrySpeed * tpf;
        
        double curveOffset = 0;
        if (entryPath != null) {
            curveOffset = switch (entryPath.type) {
                case FROM_LEFT_CURVE -> Math.sin(entryCurvePhase) * 30 * (1 - entryProgress);
                case FROM_RIGHT_CURVE -> -Math.sin(entryCurvePhase) * 30 * (1 - entryProgress);
                case FROM_TOP_SPLIT -> Math.sin(entryCurvePhase * 0.5) * 50 * (1 - entryProgress);
                case FROM_SIDE_SWOOP -> Math.sin(entryCurvePhase * 1.5) * 40 * (1 - entryProgress);
            };
        }
        
        double normalizedDx = dx / dist;
        double normalizedDy = dy / dist;
        
        double perpX = -normalizedDy;
        
        entity.translate(
            normalizedDx * moveSpeed + perpX * curveOffset * tpf * 3,
            normalizedDy * moveSpeed
        );
    }
    
    private void updateFormation(double tpf) {
        formationHoldTime += tpf;
        
        hoverPhase += tpf * 2;
        double hoverY = baseY + Math.sin(hoverPhase) * hoverAmplitude;
        entity.setY(hoverY);
        
        entity.translateX(speedX * tpf);
        
        if (entity.getX() <= 0 || entity.getRightX() >= FXGL.getAppWidth()) {
            speedX = -speedX;
        }
        
        if (formationHoldTime > minFormationTime) {
            double diveChance = 0.001 * (1 + FXGL.geti("level") * 0.2);
            if (random.nextDouble() < diveChance) {
                startDive();
            }
        }
    }
    
    private void startDive() {
        state = State.DIVING;
        
        try {
            var player = FXGL.getGameWorld().getSingleton(deltablade.EntityType.PLAYER);
            diveTargetX = player.getX() + player.getWidth() / 2;
        } catch (Exception e) {
            diveTargetX = entity.getX();
        }
    }
    
    private void updateDiving(double tpf) {
        double dx = diveTargetX - entity.getX();
        double horizontalSpeed = Math.signum(dx) * Math.min(Math.abs(dx), 100) * tpf;
        
        entity.translateX(horizontalSpeed);
        entity.translateY(diveSpeed * tpf);
        
        if (entity.getY() > FXGL.getAppHeight()) {
            entity.removeFromWorld();
        }
    }
    
    private void updateFiring(double tpf) {
        fireTimer += tpf;
        if (fireTimer > 0.5 && random.nextDouble() < type.fireRate * tpf * 60) {
            fireTimer = 0;
            FXGL.<deltablade.DeltaBladeApp>getAppCast().spawnEnemyBullet(
                entity.getX() + entity.getWidth() / 2,
                entity.getBottomY()
            );
        }
    }
    
    private void notifySettled() {
        if (squadId >= 0) {
            FXGL.<deltablade.DeltaBladeApp>getAppCast().onSquadMemberSettled(squadId);
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
    
    public State getState() {
        return state;
    }
    
    public boolean isEntering() {
        return state == State.ENTERING;
    }
    
    public int getSquadId() {
        return squadId;
    }
}
