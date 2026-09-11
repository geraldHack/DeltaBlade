package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.WaveManager;

import java.util.Random;

public class EnemyComponent extends Component {
    
    public enum EnemyType {
        BASIC(1, 100, 55, 0.015),
        FAST(1, 150, 80, 0.02),
        TOUGH(3, 300, 45, 0.025),
        BOSS(10, 1000, 35, 0.04),
        UFO(5, 500, 110, 0.045);
        
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
        DIVING,
        BOSS_HOVER,
        BOSS_DIVE,
        UFO_SWEEP
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
    
    private boolean isBoss = false;
    private double bossSwayPhase = 0;
    private double bossDiveTimer = 0;
    private double bossReturnY = 0;
    private boolean bossDiving = false;
    private boolean isKamikaze = false;
    private boolean isBonusRunner = false;
    private boolean isUfo = false;
    private double ufoDir = 1;
    private int waveLevel = 1;
    
    private static final Random random = new Random();
    
    public EnemyComponent(EnemyType type, int level) {
        this.type = type;
        this.waveLevel = Math.max(1, level);
        this.health = type.health;
        double levelMultiplier = 1 + (level - 1) * 0.08;
        this.entrySpeed = 60 + level * 5;
        this.speedX = type.speed * levelMultiplier * (random.nextBoolean() ? 1 : -1);
        this.hoverPhase = random.nextDouble() * Math.PI * 2;
        this.diveSpeed = 90 + level * 10;
        this.minFormationTime = 3.0 + random.nextDouble() * 2.0;
        
        if (type == EnemyType.BOSS) {
            this.isBoss = true;
            int cycle = (level - 1) / 4 + 1;
            this.health = deltablade.GameVars.BOSS_BASE_HEALTH + deltablade.GameVars.BOSS_HEALTH_PER_CYCLE * cycle;
            this.speedX = type.speed * (random.nextBoolean() ? 1 : -1);
        }
        if (type == EnemyType.UFO) {
            this.isUfo = true;
            this.ufoDir = random.nextBoolean() ? 1 : -1;
        }
    }
    
    public void setKamikaze(boolean kamikaze) {
        this.isKamikaze = kamikaze;
        if (kamikaze) {
            this.diveSpeed *= 1.3;
        }
    }
    
    public void setBonusRunner(boolean bonusRunner) {
        this.isBonusRunner = bonusRunner;
        if (bonusRunner) {
            this.minFormationTime = 0.15;
        }
    }

    public boolean isBonusRunner() {
        return isBonusRunner;
    }

    public boolean isUfo() {
        return isUfo;
    }

    public boolean isBoss() {
        return isBoss;
    }
    
    public void setEntryData(double targetX, double targetY, WaveManager.EntryPath path, int squadId) {
        this.targetX = targetX;
        this.targetY = Math.min(targetY, formationCeiling());
        this.entryPath = path;
        this.squadId = squadId;
        this.state = State.ENTERING;
        this.entryProgress = 0;
    }
    
    @Override
    public void onAdded() {
        if (isUfo && entryPath == null) {
            state = State.UFO_SWEEP;
            baseY = entity.getY();
            return;
        }
        if (entryPath == null) {
            state = State.FORMATION;
            baseY = entity.getY();
        }
    }
    
    private static final double MAX_TPF = 1.0 / 45.0;
    private int frameCount = 0;
    private static final int WARMUP_FRAMES = 3;
    
    @Override
    public void onUpdate(double tpf) {
        try {
            if (FXGL.<deltablade.DeltaBladeApp>getAppCast().isCombatStopped()) {
                return;
            }
        } catch (Exception ignored) {
            return;
        }
        frameCount++;
        if (frameCount <= WARMUP_FRAMES) {
            tpf = Math.min(tpf, 0.008);
        } else {
            tpf = Math.min(tpf, MAX_TPF);
        }
        
        switch (state) {
            case ENTERING -> updateEntering(tpf);
            case FORMATION -> updateFormation(tpf);
            case DIVING -> updateDiving(tpf);
            case BOSS_HOVER -> updateBossHover(tpf);
            case BOSS_DIVE -> updateBossDive(tpf);
            case UFO_SWEEP -> updateUfoSweep(tpf);
        }
        
        if (!isBonusRunner && (state != State.ENTERING || canFireWhileClimbing())) {
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
        
        if (dist < 5 || (entryProgress >= 1.0 && dist < 30)) {
            entity.setPosition(targetX, targetY);
            if (isBoss) {
                state = State.BOSS_HOVER;
                baseY = targetY;
                bossReturnY = targetY;
            } else if (isUfo) {
                state = State.UFO_SWEEP;
            } else if (isKamikaze) {
                startDive();
            } else if (isBonusRunner) {
                state = State.DIVING;
                diveTargetX = targetX;
            } else {
                state = State.FORMATION;
            }
            baseY = Math.min(targetY, formationCeiling());
            formationHoldTime = 0;
            
            notifySettled();
            return;
        }
        
        double moveSpeed = entrySpeed * tpf;
        
        double curveOffset = 0;
        if (entryPath != null) {
            double curveFade = Math.max(0, 1 - entryProgress);
            curveOffset = switch (entryPath.type) {
                case FROM_LEFT_CURVE -> Math.sin(entryCurvePhase) * 30 * curveFade;
                case FROM_RIGHT_CURVE -> -Math.sin(entryCurvePhase) * 30 * curveFade;
                case FROM_TOP_SPLIT -> Math.sin(entryCurvePhase * 0.5) * 50 * curveFade;
                case FROM_SIDE_SWOOP -> Math.sin(entryCurvePhase * 1.5) * 40 * curveFade;
                case FROM_CENTER -> {
                    double side = targetX < FXGL.getAppWidth() / 2.0 ? -1 : 1;
                    yield side * Math.sin(entryCurvePhase * 0.85) * 90 * curveFade;
                }
                case FROM_BELOW -> Math.sin(entryCurvePhase * 0.65) * 36 * curveFade;
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
        baseY = Math.min(baseY, formationCeiling());
        double hoverY = Math.min(baseY + Math.sin(hoverPhase) * hoverAmplitude, formationCeiling() + 6);
        entity.setY(hoverY);
        
        entity.translateX(speedX * tpf);
        
        if (entity.getX() <= 0 || entity.getRightX() >= FXGL.getAppWidth()) {
            speedX = -speedX;
        }
        
        if (isBonusRunner && formationHoldTime > minFormationTime) {
            startDive();
            diveTargetX = entity.getX();
            return;
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
            FXGL.<deltablade.DeltaBladeApp>getAppCast().onEnemyLeftScreen(squadId);
            entity.removeFromWorld();
        }
    }
    
    private void updateFiring(double tpf) {
        fireTimer += tpf;
        boolean angled = type == EnemyType.FAST || type == EnemyType.TOUGH;
        if (angled) {
            double gap = waveLevel <= 20 ? 2.4 - (waveLevel - 1) * 0.05 : 1.3;
            if (fireTimer >= gap) {
                fireTimer = 0;
                fireNow();
            }
            return;
        }
        double effectiveFireRate = isBoss ? type.fireRate * 1.5 : type.fireRate;
        if (fireTimer > 0.5 && random.nextDouble() < effectiveFireRate * tpf * 60) {
            fireTimer = 0;
            fireNow();
        }
    }

    private void fireNow() {
        FXGL.<deltablade.DeltaBladeApp>getAppCast().spawnEnemyShot(
            type,
            entity.getX() + entity.getWidth() / 2,
            entity.getBottomY()
        );
    }

    private void updateUfoSweep(double tpf) {
        hoverPhase += tpf * 3;
        baseY = Math.min(baseY, formationCeiling());
        entity.setY(Math.min(baseY + Math.sin(hoverPhase) * 10, formationCeiling()));
        entity.translateX(type.speed * ufoDir * tpf);

        double minX = deltablade.GameVars.RAIL_WIDTH + 8;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - deltablade.GameVars.RAIL_WIDTH - 8;
        if (entity.getX() <= minX) {
            entity.setX(minX);
            ufoDir = 1;
        } else if (entity.getX() >= maxX) {
            entity.setX(maxX);
            ufoDir = -1;
        }
    }
    
    private void updateBossHover(double tpf) {
        bossSwayPhase += tpf * 1.5;
        bossDiveTimer += tpf;
        
        double swayX = Math.sin(bossSwayPhase) * 80;
        double swayY = Math.sin(bossSwayPhase * 0.7) * 20;
        
        double centerX = (FXGL.getAppWidth() - entity.getWidth()) / 2;
        double targetX = centerX + swayX;
        
        double dx = targetX - entity.getX();
        entity.translateX(dx * tpf * 2);
        
        entity.setY(bossReturnY + swayY);
        
        double minX = deltablade.GameVars.RAIL_WIDTH;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - deltablade.GameVars.RAIL_WIDTH;
        if (entity.getX() < minX) entity.setX(minX);
        if (entity.getX() > maxX) entity.setX(maxX);
        
        if (bossDiveTimer > 4.0 && random.nextDouble() < 0.02) {
            bossDiveTimer = 0;
            state = State.BOSS_DIVE;
            bossDiving = true;
        }
    }
    
    private void updateBossDive(double tpf) {
        double maxDiveY = FXGL.getAppHeight() * 0.55;
        
        if (bossDiving) {
            entity.translateY(diveSpeed * tpf);
            if (entity.getY() >= maxDiveY) {
                bossDiving = false;
            }
        } else {
            entity.translateY(-diveSpeed * 0.7 * tpf);
            if (entity.getY() <= bossReturnY) {
                entity.setY(bossReturnY);
                state = State.BOSS_HOVER;
            }
        }
        
        double minX = deltablade.GameVars.RAIL_WIDTH;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - deltablade.GameVars.RAIL_WIDTH;
        if (entity.getX() < minX) entity.setX(minX);
        if (entity.getX() > maxX) entity.setX(maxX);
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

    public boolean isHarmlessOnContact() {
        return isEntering()
                && entryPath != null
                && entryPath.type == WaveManager.EntryPath.Type.FROM_BELOW;
    }

    private static double formationCeiling() {
        return FXGL.getAppHeight() * deltablade.GameVars.FORMATION_MAX_Y_RATIO;
    }

    private boolean canFireWhileClimbing() {
        if (state != State.ENTERING || entryPath == null
                || entryPath.type != WaveManager.EntryPath.Type.FROM_BELOW) {
            return false;
        }
        return entity.getBottomY() < FXGL.getAppHeight() - 130;
    }
    
    public int getSquadId() {
        return squadId;
    }
}
