package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import deltablade.components.EnemyComponent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

public class WaveManager {
    
    private int currentLevel;
    private List<Squad> activeSquads = new ArrayList<>();
    private List<Point2D> formationSlots = new ArrayList<>();
    private int totalEnemiesInWave = 0;
    private int enemiesSpawned = 0;
    private int squadsToSpawn = 0;
    private double squadSpawnTimer = 0;
    private double squadSpawnDelay = 1.5;
    
    private static final Random random = new Random();
    
    public static class Squad {
        public final int id;
        public final List<Entity> enemies = new ArrayList<>();
        public final int originalSize;
        public int destroyedWhileEntering = 0;
        public boolean settled = false;
        
        public Squad(int id, int size) {
            this.id = id;
            this.originalSize = size;
        }
        
        public void addEnemy(Entity enemy) {
            enemies.add(enemy);
        }
        
        public void onEnemyDestroyed(boolean wasEntering) {
            if (wasEntering && !settled) {
                destroyedWhileEntering++;
            }
        }
        
        public boolean isFullyDestroyed() {
            return enemies.stream().noneMatch(Entity::isActive);
        }
        
        public boolean earnedComboBonus() {
            return destroyedWhileEntering == originalSize && !settled;
        }
        
        public void markSettled() {
            settled = true;
        }
    }
    
    public void startWave(int level) {
        this.currentLevel = level;
        this.activeSquads.clear();
        this.formationSlots.clear();
        this.enemiesSpawned = 0;
        this.squadSpawnTimer = 0;
        
        int rows = 2 + level / 3;
        int cols = Math.min(5 + level, 8);
        
        double spacingX = getAppWidth() / (cols + 1.0);
        double spacingY = 45;
        double startY = 60;
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double x = spacingX * (col + 1) - 15;
                double y = startY + row * spacingY;
                formationSlots.add(new Point2D(x, y));
            }
        }
        
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = 3 + level / 2;
        int enemiesPerSquad = (int) Math.ceil((double) totalEnemiesInWave / squadsToSpawn);
        
        set(GameVars.ENEMIES_REMAINING, totalEnemiesInWave);
        
        spawnNextSquad();
    }
    
    public void update(double tpf) {
        if (enemiesSpawned < totalEnemiesInWave && squadsToSpawn > 0) {
            squadSpawnTimer += tpf;
            if (squadSpawnTimer >= squadSpawnDelay) {
                squadSpawnTimer = 0;
                spawnNextSquad();
            }
        }
        
        checkSquadBonuses();
    }
    
    private void spawnNextSquad() {
        if (enemiesSpawned >= totalEnemiesInWave || squadsToSpawn <= 0) return;
        
        squadsToSpawn--;
        
        int squadSize = Math.min(4 + currentLevel / 2, totalEnemiesInWave - enemiesSpawned);
        squadSize = Math.min(squadSize, 6);
        
        if (squadSize <= 0) return;
        
        Squad squad = new Squad(activeSquads.size(), squadSize);
        activeSquads.add(squad);
        
        EntryPath path = generateEntryPath();
        
        for (int i = 0; i < squadSize && enemiesSpawned < totalEnemiesInWave; i++) {
            Point2D targetSlot = formationSlots.get(enemiesSpawned);
            
            EnemyComponent.EnemyType type = determineEnemyType(enemiesSpawned / 8, currentLevel);
            
            double delay = i * 0.15;
            
            run(() -> {
                Entity enemy = spawn("enemy", new SpawnData(path.startX, path.startY)
                        .put("enemyType", type)
                        .put("level", currentLevel)
                        .put("targetX", targetSlot.getX())
                        .put("targetY", targetSlot.getY())
                        .put("entryPath", path)
                        .put("squadId", squad.id)
                        .put("entering", true));
                
                squad.addEnemy(enemy);
            }, javafx.util.Duration.seconds(delay));
            
            enemiesSpawned++;
        }
    }
    
    private EntryPath generateEntryPath() {
        int pathType = random.nextInt(4);
        
        return switch (pathType) {
            case 0 -> new EntryPath(-50, 100, EntryPath.Type.FROM_LEFT_CURVE);
            case 1 -> new EntryPath(getAppWidth() + 50, 100, EntryPath.Type.FROM_RIGHT_CURVE);
            case 2 -> new EntryPath(getAppWidth() / 2, -50, EntryPath.Type.FROM_TOP_SPLIT);
            default -> new EntryPath(random.nextBoolean() ? -50 : getAppWidth() + 50, 
                                     50 + random.nextDouble() * 100, 
                                     EntryPath.Type.FROM_SIDE_SWOOP);
        };
    }
    
    private EnemyComponent.EnemyType determineEnemyType(int row, int level) {
        if (level >= 3 && row == 0 && random.nextDouble() < 0.3) {
            return EnemyComponent.EnemyType.TOUGH;
        }
        if (level >= 2 && random.nextDouble() < 0.15 + level * 0.05) {
            return EnemyComponent.EnemyType.FAST;
        }
        return EnemyComponent.EnemyType.BASIC;
    }
    
    private void checkSquadBonuses() {
        for (Squad squad : activeSquads) {
            if (squad.isFullyDestroyed() && squad.earnedComboBonus()) {
                int bonus = 500 * currentLevel;
                inc(GameVars.SCORE, bonus);
                showComboBonus(bonus);
                squad.settled = true;
            }
        }
    }
    
    private void showComboBonus(int bonus) {
        Text bonusText = new Text("SQUAD COMBO! +" + bonus);
        bonusText.setFont(Font.font("Monospace", 24));
        bonusText.setFill(Color.GOLD);
        bonusText.setTranslateX(getAppWidth() / 2 - 100);
        bonusText.setTranslateY(getAppHeight() / 2);
        
        getGameScene().addUINode(bonusText);
        
        run(() -> getGameScene().removeUINode(bonusText), javafx.util.Duration.seconds(1.5));
    }
    
    public void onEnemyDestroyed(int squadId, boolean wasEntering) {
        if (squadId >= 0 && squadId < activeSquads.size()) {
            activeSquads.get(squadId).onEnemyDestroyed(wasEntering);
        }
    }
    
    public void markSquadSettled(int squadId) {
        if (squadId >= 0 && squadId < activeSquads.size()) {
            activeSquads.get(squadId).markSettled();
        }
    }
    
    public boolean isWaveComplete() {
        return geti(GameVars.ENEMIES_REMAINING) <= 0;
    }
    
    public static class EntryPath {
        public enum Type {
            FROM_LEFT_CURVE,
            FROM_RIGHT_CURVE,
            FROM_TOP_SPLIT,
            FROM_SIDE_SWOOP
        }
        
        public final double startX;
        public final double startY;
        public final Type type;
        
        public EntryPath(double startX, double startY, Type type) {
            this.startX = startX;
            this.startY = startY;
            this.type = type;
        }
    }
}
