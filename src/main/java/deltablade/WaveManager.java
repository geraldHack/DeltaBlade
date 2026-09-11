package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import deltablade.components.EnemyComponent;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.*;

public class WaveManager {

    public enum WaveType {
        FIGHTERS,
        MIXED,
        RANK,
        BONUS,
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
    private double initialDelay = 0.2;
    private boolean initialDelayPassed = false;
    private boolean stopped = false;
    private int bonusPerfectStreak = 0;

    public static class Squad {
        public final int id;
        public final List<Entity> enemies = new ArrayList<>();
        public final int originalSize;
        public int destroyedWhileEntering = 0;
        public int escaped = 0;
        public boolean settled = false;
        public boolean bonusResolved = false;

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
        startWave(level, resolveWaveType(level));
    }

    public void startWave(int level, WaveType forcedType) {
        this.currentLevel = level;
        this.activeSquads.clear();
        this.formationSlots.clear();
        this.enemiesSpawned = 0;
        this.squadSpawnTimer = 0;
        this.initialDelayPassed = false;
        this.squadSpawnDelay = 2.5;
        this.bonusPerfectStreak = 0;
        this.stopped = false;

        currentWaveType = forcedType != null ? forcedType : resolveWaveType(level);

        switch (currentWaveType) {
            case FIGHTERS -> setupFightersWave(level);
            case MIXED -> setupMixedWave(level);
            case RANK -> setupRankWave(level);
            case BONUS -> setupBonusWave(level);
            case BOSS -> setupBossWave(level);
            case KAMIKAZE -> setupKamikazeWave(level);
        }

        set(GameVars.ENEMIES_REMAINING, totalEnemiesInWave);
        set(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 0);
    }

    public static WaveType resolveWaveType(int level) {
        if (level > 0 && level % 25 == 0) {
            return WaveType.BOSS;
        }
        int slot = ((level - 1) % 4) + 1;
        return switch (slot) {
            case 1 -> WaveType.FIGHTERS;
            case 2 -> WaveType.MIXED;
            case 3 -> WaveType.RANK;
            case 4 -> {
                int pack = (level - 1) / 4;
                yield (pack % 2 == 0) ? WaveType.BONUS : WaveType.KAMIKAZE;
            }
            default -> WaveType.FIGHTERS;
        };
    }

    private void setupFightersWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int baseCount = level == 1 ? 6 : Math.min(8 + cycle * 2, 14);
        FormationType formation = cycle % 2 == 1 ? FormationType.ROW : FormationType.V_FORMATION;
        generateFormationSlots(formation, baseCount);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = Math.min(2 + cycle / 2, 3);
    }

    private void setupMixedWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int baseCount = Math.min(10 + cycle * 2, 16);
        FormationType formation = switch (cycle % 3) {
            case 1 -> FormationType.DIAMOND;
            case 2 -> FormationType.TWO_COLUMNS;
            default -> FormationType.STAGGERED;
        };
        generateFormationSlots(formation, baseCount);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = Math.min(3 + cycle / 2, 4);
    }

    private void setupRankWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int baseCount = Math.min(6 + cycle, 10);
        generateFormationSlots(FormationType.ROW, baseCount);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = 2;
    }

    private void setupBonusWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int total = Math.min(10 + cycle * 2, 16);
        placeFlythroughSlots(total, 70, 48);
        totalEnemiesInWave = total;
        squadsToSpawn = 3 + cycle % 2;
        squadSpawnDelay = 1.8;
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
        placeFlythroughSlots(totalKamikaze, 60, 52);
        totalEnemiesInWave = totalKamikaze;
        squadsToSpawn = 2 + cycle % 2;
        squadSpawnDelay = 1.5;
    }

    private void placeFlythroughSlots(int total, double startY, double spacingY) {
        double playableLeft = GameVars.RAIL_WIDTH + 50;
        double playableWidth = getAppWidth() - 2 * GameVars.RAIL_WIDTH - 100;
        int cols = 4;
        for (int i = 0; i < total; i++) {
            int col = i % cols;
            int row = i / cols;
            double x = playableLeft + (col + 0.5) * playableWidth / cols;
            double y = clampFormationY(startY + row * spacingY);
            formationSlots.add(new Point2D(x, y));
        }
    }

    private void generateFormationSlots(FormationType type, int targetCount) {
        double playableLeft = GameVars.RAIL_WIDTH + 40;
        double playableRight = getAppWidth() - GameVars.RAIL_WIDTH - 40;
        double playableWidth = playableRight - playableLeft;
        double minGap = 96;

        switch (type) {
            case ROW -> {
                int perRow = Math.max(1, Math.min(targetCount, (int) (playableWidth / minGap)));
                int rows = (int) Math.ceil((double) targetCount / perRow);
                int created = 0;
                for (int row = 0; row < rows && created < targetCount; row++) {
                    int inThisRow = Math.min(perRow, targetCount - created);
                    addSpreadRow(playableLeft, playableWidth, inThisRow, clampFormationY(56 + row * 72));
                    created += inThisRow;
                }
            }
            case V_FORMATION -> {
                double centerX = getAppWidth() / 2.0;
                double spacingX = 92;
                double spacingY = 72;
                int created = 0;
                int row = 0;
                while (created < targetCount) {
                    int inThisRow = Math.min(row + 1, targetCount - created);
                    for (int i = 0; i < inThisRow && created < targetCount; i++) {
                        double offsetX = (i - (inThisRow - 1) / 2.0) * spacingX;
                        formationSlots.add(new Point2D(centerX + offsetX - 24, clampFormationY(56 + row * spacingY)));
                        created++;
                    }
                    row++;
                    if (row > 5) break;
                }
            }
            case DIAMOND -> {
                double centerX = getAppWidth() / 2.0 - 24;
                double spacingX = 96;
                double spacingY = 76;
                int[] rowCounts = {1, 2, 3, 2, 1};
                int created = 0;
                for (int row = 0; row < rowCounts.length && created < targetCount; row++) {
                    int inThisRow = Math.min(rowCounts[row], targetCount - created);
                    for (int i = 0; i < inThisRow && created < targetCount; i++) {
                        double offsetX = (i - (inThisRow - 1) / 2.0) * spacingX;
                        formationSlots.add(new Point2D(centerX + offsetX, clampFormationY(56 + row * spacingY)));
                        created++;
                    }
                }
            }
            case TWO_COLUMNS -> {
                double leftX = playableLeft + playableWidth * 0.22;
                double rightX = playableLeft + playableWidth * 0.78;
                double spacingY = 76;
                int perColumn = (targetCount + 1) / 2;
                double maxY = formationMaxY();
                if (perColumn > 1) {
                    spacingY = Math.min(spacingY, (maxY - 56) / (perColumn - 1));
                }
                int created = 0;
                for (int i = 0; i < perColumn && created < targetCount; i++) {
                    formationSlots.add(new Point2D(leftX, clampFormationY(56 + i * spacingY)));
                    created++;
                    if (created < targetCount) {
                        formationSlots.add(new Point2D(rightX, clampFormationY(56 + i * spacingY)));
                        created++;
                    }
                }
            }
            case STAGGERED -> {
                int perRow = Math.max(1, Math.min(4, (int) (playableWidth / minGap)));
                int rows = (int) Math.ceil((double) targetCount / perRow);
                int created = 0;
                for (int row = 0; row < rows && created < targetCount; row++) {
                    int inThisRow = Math.min(perRow, targetCount - created);
                    double inset = (row % 2 == 1) ? minGap * 0.35 : 0;
                    addSpreadRow(playableLeft + inset, playableWidth - inset * 2, inThisRow, clampFormationY(56 + row * 72));
                    created += inThisRow;
                }
            }
        }
    }

    private void addSpreadRow(double left, double width, int count, double y) {
        if (count <= 0) {
            return;
        }
        if (count == 1) {
            formationSlots.add(new Point2D(left + width / 2.0 - 24, clampFormationY(y)));
            return;
        }
        double spacing = width / (count + 1);
        spacing = Math.max(96, spacing);
        double used = (count - 1) * spacing;
        double startX = left + Math.max(0, (width - used) / 2.0);
        for (int col = 0; col < count; col++) {
            formationSlots.add(new Point2D(startX + col * spacing, clampFormationY(y)));
        }
    }

    private static double formationMaxY() {
        return getAppHeight() * GameVars.FORMATION_MAX_Y_RATIO;
    }

    private static double clampFormationY(double y) {
        return Math.min(y, formationMaxY());
    }

    public void stop() {
        stopped = true;
    }

    public void update(double tpf) {
        if (stopped) {
            return;
        }
        if (!initialDelayPassed) {
            squadSpawnTimer += tpf;
            if (squadSpawnTimer >= initialDelay) {
                initialDelayPassed = true;
                squadSpawnTimer = 0;
                spawnNextSquad();
            }
            return;
        }

        if (enemiesSpawned < totalEnemiesInWave) {
            squadSpawnTimer += tpf;
            if (squadSpawnTimer >= squadSpawnDelay) {
                squadSpawnTimer = 0;
                if (squadsToSpawn <= 0) {
                    squadsToSpawn = 1;
                }
                spawnNextSquad();
            }
        }

        if (currentWaveType == WaveType.BONUS) {
            checkBonusPerfects();
        } else {
            checkSquadBonuses();
        }
    }

    private void spawnNextSquad() {
        if (stopped || enemiesSpawned >= totalEnemiesInWave || squadsToSpawn <= 0) return;

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

            double startX = path.startX + (index - (squadSize - 1) / 2.0) * 42;
            double startY = path.startY + (path.type == EntryPath.Type.FROM_BELOW ? -index * 18 : index * 18);

            Runnable spawnEnemy = () -> {
                if (stopped) {
                    return;
                }
                SpawnData spawnData = new SpawnData(startX, startY)
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
                if (currentWaveType == WaveType.BONUS) {
                    spawnData.put("bonus", true);
                }

                Entity enemy = spawn("enemy", spawnData);
                squad.addEnemy(enemy);
            };

            if (index == 0) {
                spawnEnemy.run();
            } else {
                double delay = currentWaveType == WaveType.KAMIKAZE || currentWaveType == WaveType.BONUS
                        ? index * 0.28
                        : index * 0.42;
                runOnce(spawnEnemy, javafx.util.Duration.seconds(delay));
            }

            enemiesSpawned++;
        }
    }

    private EntryPath generateEntryPath() {
        int squadIndex = Math.max(0, activeSquads.size() - 1);
        return switch (currentWaveType) {
            case FIGHTERS, MIXED, BONUS -> mixedPath(squadIndex);
            case RANK -> squadIndex % 2 == 0
                    ? new EntryPath(getAppWidth() / 2.0, -50, EntryPath.Type.FROM_CENTER)
                    : new EntryPath(getAppWidth() / 2.0, -50, EntryPath.Type.FROM_TOP_SPLIT);
            case BOSS -> new EntryPath(getAppWidth() / 2, -80, EntryPath.Type.FROM_TOP_SPLIT);
            case KAMIKAZE -> {
                boolean fromLeft = squadIndex % 2 == 0;
                double y = 50 + (squadIndex % 3) * 30;
                yield fromLeft
                    ? new EntryPath(-50, y, EntryPath.Type.FROM_LEFT_CURVE)
                    : new EntryPath(getAppWidth() + 50, y, EntryPath.Type.FROM_RIGHT_CURVE);
            }
        };
    }

    private EntryPath mixedPath(int squadIndex) {
        return switch (Math.floorMod(squadIndex + currentLevel, 4)) {
            case 0 -> new EntryPath(-50, 100, EntryPath.Type.FROM_LEFT_CURVE);
            case 1 -> new EntryPath(getAppWidth() + 50, 100, EntryPath.Type.FROM_RIGHT_CURVE);
            case 2 -> new EntryPath(getAppWidth() / 2.0, -50, EntryPath.Type.FROM_CENTER);
            default -> {
                boolean left = squadIndex % 2 == 0;
                double x = left
                        ? GameVars.RAIL_WIDTH + 16
                        : getAppWidth() - GameVars.RAIL_WIDTH - 64;
                yield new EntryPath(x, getAppHeight() + 40, EntryPath.Type.FROM_BELOW);
            }
        };
    }

    private EnemyComponent.EnemyType determineEnemyType(int slotIndex) {
        if (currentWaveType == WaveType.BOSS && slotIndex == 0) {
            return EnemyComponent.EnemyType.BOSS;
        }

        if (currentWaveType == WaveType.BOSS) {
            return slotIndex % 3 == 0 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }

        if (currentWaveType == WaveType.RANK) {
            return EnemyComponent.EnemyType.TOUGH;
        }

        if (currentWaveType == WaveType.KAMIKAZE) {
            return slotIndex % 3 == 0 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }

        if (currentWaveType == WaveType.BONUS) {
            return slotIndex % 4 == 1 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }

        if (currentWaveType == WaveType.MIXED) {
            if (slotIndex % 5 == 0 && currentLevel >= 2) {
                return EnemyComponent.EnemyType.TOUGH;
            }
            if (slotIndex % 3 == 1) {
                return EnemyComponent.EnemyType.FAST;
            }
            return EnemyComponent.EnemyType.BASIC;
        }

        if (currentLevel >= 3 && slotIndex == 0) {
            return EnemyComponent.EnemyType.TOUGH;
        }
        if (currentLevel >= 2 && slotIndex % 4 == 1) {
            return EnemyComponent.EnemyType.FAST;
        }

        return EnemyComponent.EnemyType.BASIC;
    }

    public void checkKillBonuses() {
        if (currentWaveType == WaveType.BONUS) {
            checkBonusPerfects();
        } else {
            checkSquadBonuses();
        }
    }

    public void checkSquadBonuses() {
        for (Squad squad : activeSquads) {
            if (squad.isFullyDestroyed() && squad.earnedComboBonus()) {
                int bonus = 500 * currentLevel;
                inc(GameVars.SCORE, bonus);
                inc(GameVars.SQUAD_COMBOS, 1);
                showComboBonus(bonus);
                squad.settled = true;
            }
        }
    }

    public void checkBonusPerfects() {
        for (Squad squad : activeSquads) {
            if (squad.bonusResolved) {
                continue;
            }
            if (squad.enemies.size() < squad.originalSize) {
                continue;
            }
            if (!squad.isFullyDestroyed()) {
                continue;
            }
            squad.bonusResolved = true;
            if (squad.escaped == 0) {
                bonusPerfectStreak++;
                int award = isBonusWaveFullyClean()
                        ? GameVars.BONUS_ROUND_PERFECT_SCORE
                        : GameVars.BONUS_PERFECT_BASE << Math.min(bonusPerfectStreak - 1, 6);
                inc(GameVars.SCORE, award);
                FXGL.<DeltaBladeApp>getAppCast().showBonusPerfect(award);
            } else {
                bonusPerfectStreak = 0;
            }
        }
    }

    private boolean isBonusWaveFullyClean() {
        if (currentWaveType != WaveType.BONUS || enemiesSpawned < totalEnemiesInWave) {
            return false;
        }
        if (geti(GameVars.ENEMIES_REMAINING) > 0 || activeSquads.isEmpty()) {
            return false;
        }
        for (Squad squad : activeSquads) {
            if (squad.escaped != 0 || !squad.isFullyDestroyed() || squad.enemies.size() < squad.originalSize) {
                return false;
            }
        }
        return true;
    }

    private void showComboBonus(int bonus) {
        FXGL.<DeltaBladeApp>getAppCast().showSquadCombo(bonus);
    }

    public void onEnemyDestroyed(int squadId, boolean wasEntering) {
        if (squadId >= 0 && squadId < activeSquads.size()) {
            activeSquads.get(squadId).onEnemyDestroyed(wasEntering);
        }
    }

    public void onEnemyEscaped(int squadId) {
        if (squadId >= 0 && squadId < activeSquads.size()) {
            activeSquads.get(squadId).escaped++;
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
            FROM_SIDE_SWOOP,
            FROM_CENTER,
            FROM_BELOW
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
