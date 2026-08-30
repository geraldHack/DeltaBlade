package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import deltablade.components.EnemyComponent;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

public class WaveManager {
    
    public enum WaveType {
        FIGHTERS,
        MIXED,
        BOSS,
        KAMIKAZE
    }
    
    public enum FormationType {
        ROW,
        V_FORMATION,
        DIAMOND,
        TWO_COLUMNS,
        STAGGERED
    }
    
    private int currentLevel;
    private WaveType currentWaveType;
    private List<Squad> activeSquads = new ArrayList<>();
    private List<Point2D> formationSlots = new ArrayList<>();
    private int totalEnemiesInWave = 0;
    private int enemiesSpawned = 0;
    private int squadsToSpawn = 0;
    private double squadSpawnTimer = 0;
    private double squadSpawnDelay = 2.5;
    private double initialDelay = 1.0;
    private boolean initialDelayPassed = false;
    
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
        this.initialDelayPassed = false;
        
        int cycleSlot = ((level - 1) % 4) + 1;
        currentWaveType = switch (cycleSlot) {
            case 1 -> WaveType.FIGHTERS;
            case 2 -> WaveType.MIXED;
            case 3 -> WaveType.KAMIKAZE;
            case 4 -> WaveType.BOSS;
            default -> WaveType.FIGHTERS;
        };
        
        switch (currentWaveType) {
            case FIGHTERS -> setupFightersWave(level);
            case MIXED -> setupMixedWave(level);
            case BOSS -> setupBossWave(level);
            case KAMIKAZE -> setupKamikazeWave(level);
        }
        
        set(GameVars.ENEMIES_REMAINING, totalEnemiesInWave);
        set(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 0);
    }
    
    private void setupFightersWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int baseCount = level == 1 ? 6 : Math.min(8 + cycle * 2, 14);
        
        FormationType formation = random.nextBoolean() ? FormationType.ROW : FormationType.V_FORMATION;
        generateFormationSlots(formation, baseCount);
        
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = Math.min(2 + cycle / 2, 3);
    }
    
    private void setupMixedWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int baseCount = Math.min(10 + cycle * 2, 16);
        
        FormationType formation = switch (random.nextInt(3)) {
            case 0 -> FormationType.DIAMOND;
            case 1 -> FormationType.TWO_COLUMNS;
            default -> FormationType.STAGGERED;
        };
        generateFormationSlots(formation, baseCount);
        
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = Math.min(3 + cycle / 2, 4);
    }
    
    private void setupBossWave(int level) {
        int escortCount = 2 + Math.min((level - 1) / 4, 2);
        
        double centerX = getAppWidth() / 2.0 - 40;
        formationSlots.add(new Point2D(centerX, 80));
        
        double spacing = 60;
        for (int i = 0; i < escortCount; i++) {
            double offset = (i % 2 == 0 ? -1 : 1) * spacing * ((i / 2) + 1);
            formationSlots.add(new Point2D(centerX + offset, 140 + (i % 2) * 30));
        }
        
        totalEnemiesInWave = 1 + escortCount;
        squadsToSpawn = 1;
    }
    
    private void setupKamikazeWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int totalKamikaze = Math.min(8 + cycle * 2, 14);
        
        for (int i = 0; i < totalKamikaze; i++) {
            double x = GameVars.RAIL_WIDTH + 50 + random.nextDouble() * (getAppWidth() - 2 * GameVars.RAIL_WIDTH - 100);
            double y = 60 + (i % 3) * 40;
            formationSlots.add(new Point2D(x, y));
        }
        
        totalEnemiesInWave = totalKamikaze;
        squadsToSpawn = 2 + random.nextInt(2);
        squadSpawnDelay = 1.5;
    }
    
    private void generateFormationSlots(FormationType type, int targetCount) {
        double playableLeft = GameVars.RAIL_WIDTH + 30;
        double playableRight = getAppWidth() - GameVars.RAIL_WIDTH - 30;
        double playableWidth = playableRight - playableLeft;
        
        switch (type) {
            case ROW -> {
                int perRow = Math.min(6, targetCount);
                int rows = (int) Math.ceil((double) targetCount / perRow);
                double spacingX = playableWidth / (perRow + 1);
                double spacingY = 45;
                
                int created = 0;
                for (int row = 0; row < rows && created < targetCount; row++) {
                    int inThisRow = Math.min(perRow, targetCount - created);
                    double rowStartX = playableLeft + (playableWidth - (inThisRow - 1) * spacingX) / 2;
                    for (int col = 0; col < inThisRow && created < targetCount; col++) {
                        formationSlots.add(new Point2D(rowStartX + col * spacingX, 60 + row * spacingY));
                        created++;
                    }
                }
            }
            case V_FORMATION -> {
                double centerX = getAppWidth() / 2.0;
                double spacingX = 50;
                double spacingY = 35;
                
                int created = 0;
                int row = 0;
                while (created < targetCount) {
                    int inThisRow = Math.min(row + 1, targetCount - created);
                    for (int i = 0; i < inThisRow && created < targetCount; i++) {
                        double offsetX = (i - (inThisRow - 1) / 2.0) * spacingX;
                        formationSlots.add(new Point2D(centerX + offsetX - 20, 60 + row * spacingY));
                        created++;
                    }
                    row++;
                    if (row > 5) break;
                }
            }
            case DIAMOND -> {
                double centerX = getAppWidth() / 2.0 - 20;
                double spacingX = 55;
                double spacingY = 40;
                
                int[] rowCounts = {1, 2, 3, 2, 1};
                int created = 0;
                for (int row = 0; row < rowCounts.length && created < targetCount; row++) {
                    int inThisRow = Math.min(rowCounts[row], targetCount - created);
                    for (int i = 0; i < inThisRow && created < targetCount; i++) {
                        double offsetX = (i - (inThisRow - 1) / 2.0) * spacingX;
                        formationSlots.add(new Point2D(centerX + offsetX, 60 + row * spacingY));
                        created++;
                    }
                }
            }
            case TWO_COLUMNS -> {
                double leftX = playableLeft + playableWidth * 0.25;
                double rightX = playableLeft + playableWidth * 0.75;
                double spacingY = 45;
                
                int perColumn = (targetCount + 1) / 2;
                int created = 0;
                for (int i = 0; i < perColumn && created < targetCount; i++) {
                    formationSlots.add(new Point2D(leftX, 60 + i * spacingY));
                    created++;
                    if (created < targetCount) {
                        formationSlots.add(new Point2D(rightX, 60 + i * spacingY));
                        created++;
                    }
                }
            }
            case STAGGERED -> {
                int perRow = 4;
                int rows = (int) Math.ceil((double) targetCount / perRow);
                double spacingX = playableWidth / (perRow + 1);
                double spacingY = 50;
                
                int created = 0;
                for (int row = 0; row < rows && created < targetCount; row++) {
                    double rowOffset = (row % 2 == 1) ? spacingX / 2 : 0;
                    int inThisRow = Math.min(perRow, targetCount - created);
                    for (int col = 0; col < inThisRow && created < targetCount; col++) {
                        formationSlots.add(new Point2D(playableLeft + spacingX * (col + 1) + rowOffset - 20, 60 + row * spacingY));
                        created++;
                    }
                }
            }
        }
    }
    
    public void update(double tpf) {
        if (!initialDelayPassed) {
            squadSpawnTimer += tpf;
            if (squadSpawnTimer >= initialDelay) {
                initialDelayPassed = true;
                squadSpawnTimer = 0;
                spawnNextSquad();
            }
            return;
        }
        
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
        
        int remaining = totalEnemiesInWave - enemiesSpawned;
        int squadSize;
        
        if (currentWaveType == WaveType.BOSS && activeSquads.isEmpty()) {
            squadSize = remaining;
        } else {
            int baseSize = Math.max(3, remaining / Math.max(squadsToSpawn + 1, 1));
            squadSize = Math.min(baseSize, 5);
            squadSize = Math.min(squadSize, remaining);
        }
        
        if (squadSize <= 0) return;
        
        Squad squad = new Squad(activeSquads.size(), squadSize);
        activeSquads.add(squad);
        
        EntryPath path = generateEntryPath();
        
        for (int i = 0; i < squadSize && enemiesSpawned < totalEnemiesInWave; i++) {
            Point2D targetSlot = formationSlots.get(enemiesSpawned);
            
            EnemyComponent.EnemyType type = determineEnemyType(enemiesSpawned);
            
            final int index = i;
            final int squadIdFinal = squad.id;
            final EnemyComponent.EnemyType finalType = type;
            
            Runnable spawnEnemy = () -> {
                SpawnData spawnData = new SpawnData(path.startX, path.startY)
                        .put("enemyType", finalType)
                        .put("level", currentLevel)
                        .put("targetX", targetSlot.getX())
                        .put("targetY", targetSlot.getY())
                        .put("entryPath", path)
                        .put("squadId", squadIdFinal)
                        .put("entering", true);
                
                if (currentWaveType == WaveType.KAMIKAZE) {
                    spawnData.put("kamikaze", true);
                }
                
                Entity enemy = spawn("enemy", spawnData);
                squad.addEnemy(enemy);
            };
            
            if (index == 0) {
                spawnEnemy.run();
            } else {
                double delay = currentWaveType == WaveType.KAMIKAZE ? index * 0.15 : index * 0.25;
                runOnce(spawnEnemy, javafx.util.Duration.seconds(delay));
            }
            
            enemiesSpawned++;
        }
    }
    
    private EntryPath generateEntryPath() {
        return switch (currentWaveType) {
            case FIGHTERS -> {
                boolean useLeft = random.nextBoolean();
                yield useLeft 
                    ? new EntryPath(-50, 100, EntryPath.Type.FROM_LEFT_CURVE)
                    : new EntryPath(getAppWidth() + 50, 100, EntryPath.Type.FROM_RIGHT_CURVE);
            }
            case MIXED -> {
                int pathType = random.nextInt(2);
                yield pathType == 0
                    ? new EntryPath(getAppWidth() / 2, -50, EntryPath.Type.FROM_TOP_SPLIT)
                    : new EntryPath(random.nextBoolean() ? -50 : getAppWidth() + 50, 80, EntryPath.Type.FROM_SIDE_SWOOP);
            }
            case BOSS -> new EntryPath(getAppWidth() / 2, -80, EntryPath.Type.FROM_TOP_SPLIT);
            case KAMIKAZE -> {
                boolean fromLeft = random.nextBoolean();
                yield fromLeft
                    ? new EntryPath(-50, 50 + random.nextDouble() * 100, EntryPath.Type.FROM_LEFT_CURVE)
                    : new EntryPath(getAppWidth() + 50, 50 + random.nextDouble() * 100, EntryPath.Type.FROM_RIGHT_CURVE);
            }
        };
    }
    
    private EnemyComponent.EnemyType determineEnemyType(int slotIndex) {
        if (currentWaveType == WaveType.BOSS && slotIndex == 0) {
            return EnemyComponent.EnemyType.BOSS;
        }
        
        if (currentWaveType == WaveType.BOSS) {
            return random.nextDouble() < 0.3 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }
        
        if (currentWaveType == WaveType.KAMIKAZE) {
            return random.nextDouble() < 0.4 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }
        
        if (currentWaveType == WaveType.MIXED) {
            double roll = random.nextDouble();
            if (roll < 0.15 + currentLevel * 0.02) {
                return EnemyComponent.EnemyType.TOUGH;
            } else if (roll < 0.35 + currentLevel * 0.03) {
                return EnemyComponent.EnemyType.FAST;
            }
        }
        
        if (currentLevel >= 3 && slotIndex < 3 && random.nextDouble() < 0.2) {
            return EnemyComponent.EnemyType.TOUGH;
        }
        if (currentLevel >= 2 && random.nextDouble() < 0.15 + currentLevel * 0.03) {
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
        String message = "SQUAD COMBO! +" + bonus;
        Color textColor = Color.GOLD;
        
        Rectangle bar = new Rectangle(getAppWidth(), 40);
        bar.setFill(Color.rgb(20, 20, 20, 0.95));
        bar.setStroke(textColor);
        bar.setStrokeWidth(2);
        bar.setTranslateX(0);
        bar.setTranslateY(85);
        
        Text text = new Text(message);
        text.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
        text.setFill(textColor);
        text.setStroke(Color.BLACK);
        text.setStrokeWidth(1);
        
        double textWidth = text.getLayoutBounds().getWidth();
        text.setTranslateX((getAppWidth() - textWidth) / 2);
        text.setTranslateY(113);
        
        Group banner = new Group(bar, text);
        
        getGameScene().addUINode(banner);
        
        runOnce(() -> getGameScene().removeUINode(banner), javafx.util.Duration.seconds(1.5));
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
    
    public WaveType getCurrentWaveType() {
        return currentWaveType;
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
