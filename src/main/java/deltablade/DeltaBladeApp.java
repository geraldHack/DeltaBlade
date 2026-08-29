package deltablade;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import deltablade.components.BulletComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.ExtraLetterPickupComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlayerComponent;
import javafx.beans.property.IntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Map;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

public class DeltaBladeApp extends GameApplication {
    
    private Entity player;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean gameOver = false;
    
    private WaveManager waveManager;
    private boolean waveTransition = false;
    
    private static final Random random = new Random();
    
    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("DeltaBlade");
        settings.setVersion("1.0");
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
    }
    
    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put(GameVars.SCORE, 0);
        vars.put(GameVars.LIVES, GameVars.INITIAL_LIVES);
        vars.put(GameVars.LEVEL, 1);
        vars.put(GameVars.WEAPON_GRADE, 1);
        vars.put(GameVars.AMMO_CAP, GameVars.INITIAL_AMMO_CAP);
        vars.put(GameVars.ACTIVE_BULLETS, 0);
        vars.put(GameVars.ENEMIES_REMAINING, 0);
        vars.put(GameVars.MONEY, 0);
        vars.put(GameVars.EXTRA_E, 0);
        vars.put(GameVars.EXTRA_X, 0);
        vars.put(GameVars.EXTRA_T, 0);
        vars.put(GameVars.EXTRA_R, 0);
        vars.put(GameVars.EXTRA_A, 0);
    }
    
    @Override
    protected void initInput() {
        getInput().addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                movingLeft = true;
            }
            @Override
            protected void onActionEnd() {
                movingLeft = false;
            }
        }, KeyCode.LEFT);
        
        getInput().addAction(new UserAction("Move Left A") {
            @Override
            protected void onAction() {
                movingLeft = true;
            }
            @Override
            protected void onActionEnd() {
                movingLeft = false;
            }
        }, KeyCode.A);
        
        getInput().addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                movingRight = true;
            }
            @Override
            protected void onActionEnd() {
                movingRight = false;
            }
        }, KeyCode.RIGHT);
        
        getInput().addAction(new UserAction("Move Right D") {
            @Override
            protected void onAction() {
                movingRight = true;
            }
            @Override
            protected void onActionEnd() {
                movingRight = false;
            }
        }, KeyCode.D);
        
        getInput().addAction(new UserAction("Fire") {
            @Override
            protected void onAction() {
                if (!gameOver) {
                    fire();
                }
            }
        }, KeyCode.SPACE);
        
        getInput().addAction(new UserAction("Fire X") {
            @Override
            protected void onAction() {
                if (!gameOver) {
                    fire();
                }
            }
        }, KeyCode.X);
        
        getInput().addAction(new UserAction("Restart") {
            @Override
            protected void onActionBegin() {
                if (gameOver) {
                    restartGame();
                }
            }
        }, KeyCode.R);
    }
    
    @Override
    protected void initGame() {
        getGameScene().clearUINodes();
        
        gameOver = false;
        movingLeft = false;
        movingRight = false;
        waveTransition = false;
        player = null;
        waveManager = null;
        
        getGameWorld().addEntityFactory(new DeltaBladeFactory());
        
        getGameScene().getViewport().setX(0);
        getGameScene().getViewport().setY(0);
        
        spawn("background", new com.almasb.fxgl.entity.SpawnData(0, 0)
                .put("width", getAppWidth())
                .put("height", getAppHeight()));
        
        spawnStars();
        spawnSideRails();
        
        waveManager = new WaveManager();
        
        spawnPlayer();
        startWave();
    }
    
    private void spawnSideRails() {
        spawn("sideRail", new com.almasb.fxgl.entity.SpawnData(0, 0)
                .put("height", getAppHeight())
                .put("isLeft", true));
        spawn("sideRail", new com.almasb.fxgl.entity.SpawnData(getAppWidth() - GameVars.RAIL_WIDTH, 0)
                .put("height", getAppHeight())
                .put("isLeft", false));
    }
    
    private void spawnStars() {
        for (int i = 0; i < 80; i++) {
            double x = random.nextDouble() * getAppWidth();
            double y = random.nextDouble() * getAppHeight();
            double size = random.nextDouble() * 2 + 1;
            double opacity = 0.3 + random.nextDouble() * 0.7;
            
            spawn("star", new com.almasb.fxgl.entity.SpawnData(x, y)
                    .put("size", size)
                    .put("opacity", opacity));
        }
    }
    
    private void spawnPlayer() {
        player = spawn("player", getAppWidth() / 2 - 20, getAppHeight() - 60);
    }
    
    private void startWave() {
        waveTransition = false;
        waveManager.startWave(geti(GameVars.LEVEL));
        
        showWaveAnnouncement();
    }
    
    private void showWaveAnnouncement() {
        Text waveText = new Text("WAVE " + geti(GameVars.LEVEL));
        waveText.setFont(Font.font("Monospace", 36));
        waveText.setFill(Color.YELLOW);
        waveText.setTranslateX(getAppWidth() / 2 - 80);
        waveText.setTranslateY(getAppHeight() / 2 - 100);
        
        getGameScene().addUINode(waveText);
        
        runOnce(() -> getGameScene().removeUINode(waveText), Duration.seconds(2));
    }
    
    private static final double BULLET_SPEED = -500;
    private static final double SPREAD_VX = 80;
    
    private void fire() {
        if (player == null || gameOver) return;
        
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        int grade = geti(GameVars.WEAPON_GRADE);
        
        if (!pc.canFire(grade)) return;
        
        pc.onFired();
        
        double centerX = pc.getCenterX();
        double topY = pc.getTopY();
        
        if (grade == 1) {
            spawnBullet(centerX - 2, topY, 0, BULLET_SPEED);
        } else if (grade == 2) {
            spawnBullet(centerX - 10, topY + 3, 0, BULLET_SPEED);
            spawnBullet(centerX + 6, topY + 3, 0, BULLET_SPEED);
        } else if (grade == 3) {
            spawnBullet(centerX - 2, topY, 0, BULLET_SPEED);
            spawnBullet(centerX - 14, topY + 5, -SPREAD_VX, BULLET_SPEED);
            spawnBullet(centerX + 10, topY + 5, SPREAD_VX, BULLET_SPEED);
        } else {
            spawnBullet(centerX - 6, topY, -SPREAD_VX * 0.4, BULLET_SPEED);
            spawnBullet(centerX + 2, topY, SPREAD_VX * 0.4, BULLET_SPEED);
            spawnBullet(centerX - 18, topY + 6, -SPREAD_VX * 1.2, BULLET_SPEED);
            spawnBullet(centerX + 14, topY + 6, SPREAD_VX * 1.2, BULLET_SPEED);
        }
    }
    
    private void spawnBullet(double x, double y, double speedX, double speedY) {
        spawn("playerBullet", new com.almasb.fxgl.entity.SpawnData(x, y)
                .put("speedX", speedX)
                .put("speedY", speedY));
        inc(GameVars.ACTIVE_BULLETS, 1);
    }
    
    public void spawnEnemyBullet(double x, double y) {
        spawn("enemyBullet", x - 4, y);
    }
    
    public void onSquadMemberSettled(int squadId) {
        waveManager.markSquadSettled(squadId);
    }
    
    public void onEnemyLeftScreen(int squadId) {
        inc(GameVars.ENEMIES_REMAINING, -1);
        checkWaveComplete();
    }
    
    @Override
    protected void initPhysics() {
        onCollisionBegin(EntityType.PLAYER_BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            bullet.getComponent(BulletComponent.class).onHit();
            bullet.removeFromWorld();
            
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            ec.hit();
            
            if (ec.isDead()) {
                inc(GameVars.SCORE, ec.getScoreValue());
                inc(GameVars.ENEMIES_REMAINING, -1);
                
                waveManager.onEnemyDestroyed(ec.getSquadId(), ec.isEntering());
                
                trySpawnPickup(enemy.getX() + enemy.getWidth() / 2, enemy.getY() + enemy.getHeight() / 2);
                
                enemy.removeFromWorld();
                
                checkWaveComplete();
            }
        });
        
        onCollisionBegin(EntityType.ENEMY_BULLET, EntityType.PLAYER, (bullet, playerEntity) -> {
            bullet.removeFromWorld();
            
            PlayerComponent pc = playerEntity.getComponent(PlayerComponent.class);
            if (!pc.isInvulnerable()) {
                playerHit(pc);
            }
        });
        
        onCollisionBegin(EntityType.ENEMY, EntityType.PLAYER, (enemy, playerEntity) -> {
            PlayerComponent pc = playerEntity.getComponent(PlayerComponent.class);
            if (!pc.isInvulnerable()) {
                playerHit(pc);
            }
        });
        
        onCollisionBegin(EntityType.PICKUP, EntityType.PLAYER, (pickup, playerEntity) -> {
            PickupComponent pc = pickup.getComponent(PickupComponent.class);
            applyPickup(pc.getType());
            pickup.removeFromWorld();
        });
        
        onCollisionBegin(EntityType.EXTRA_LETTER_PICKUP, EntityType.PLAYER, (letterOrb, playerEntity) -> {
            ExtraLetterPickupComponent lpc = letterOrb.getComponent(ExtraLetterPickupComponent.class);
            collectExtraLetter(lpc.getLetter(), lpc.getLetterIndex());
            letterOrb.removeFromWorld();
        });
    }
    
    private void playerHit(PlayerComponent pc) {
        inc(GameVars.LIVES, -1);
        pc.makeInvulnerable();
        
        if (geti(GameVars.WEAPON_GRADE) > 1) {
            inc(GameVars.WEAPON_GRADE, -1);
        }
        
        if (geti(GameVars.LIVES) <= 0) {
            triggerGameOver();
        }
    }
    
    private void trySpawnPickup(double x, double y) {
        inc(GameVars.MONEY, GameVars.KILL_MONEY_BASE + random.nextInt(6));
        
        if (random.nextDouble() < GameVars.EXTRA_LETTER_DROP_CHANCE) {
            int nextLetterIndex = getNextExtraLetterIndex();
            if (nextLetterIndex >= 0) {
                char letter = GameVars.EXTRA_LETTERS[nextLetterIndex];
                spawn("extraLetterOrb", new com.almasb.fxgl.entity.SpawnData(x - 14, y - 14)
                        .put("letter", letter)
                        .put("letterIndex", nextLetterIndex));
            }
        } else if (random.nextDouble() < 0.25) {
            double roll = random.nextDouble();
            String pickupType;
            if (roll < 0.55) {
                pickupType = "weaponPickup";
            } else {
                pickupType = "ammoPickup";
            }
            spawn(pickupType, x - 14, y - 14);
        }
    }
    
    private int getNextExtraLetterIndex() {
        for (int i = 0; i < GameVars.EXTRA_VARS.length; i++) {
            if (geti(GameVars.EXTRA_VARS[i]) == 0) {
                return i;
            }
        }
        return -1;
    }
    
    private void collectExtraLetter(char letter, int letterIndex) {
        String varName = GameVars.EXTRA_VARS[letterIndex];
        if (geti(varName) == 0) {
            set(varName, 1);
            inc(GameVars.SCORE, 25);
            
            if (isExtraComplete()) {
                inc(GameVars.LIVES, 1);
                resetExtraLetters();
                showExtraLifeFlash();
            }
        }
    }
    
    private boolean isExtraComplete() {
        for (String var : GameVars.EXTRA_VARS) {
            if (geti(var) == 0) return false;
        }
        return true;
    }
    
    private void resetExtraLetters() {
        for (String var : GameVars.EXTRA_VARS) {
            set(var, 0);
        }
    }
    
    private void showExtraLifeFlash() {
        Text flash = new Text("+1 LIFE!");
        flash.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
        flash.setFill(Color.GOLD);
        flash.setStroke(Color.WHITE);
        flash.setStrokeWidth(1);
        flash.setTranslateX(8);
        flash.setTranslateY(280);
        
        Glow glow = new Glow(0.8);
        flash.setEffect(glow);
        
        getGameScene().addUINode(flash);
        runOnce(() -> getGameScene().removeUINode(flash), Duration.seconds(1.5));
    }
    
    private void applyPickup(PickupComponent.PickupType type) {
        switch (type) {
            case WEAPON_UPGRADE -> {
                if (geti(GameVars.WEAPON_GRADE) < GameVars.MAX_WEAPON_GRADE) {
                    inc(GameVars.WEAPON_GRADE, 1);
                }
                inc(GameVars.SCORE, 50);
                inc(GameVars.MONEY, 15);
            }
            case EXTRA_AMMO -> {
                if (geti(GameVars.AMMO_CAP) < GameVars.MAX_AMMO_CAP) {
                    inc(GameVars.AMMO_CAP, 1);
                }
                inc(GameVars.SCORE, 25);
                inc(GameVars.MONEY, 10);
            }
            default -> {}
        }
    }
    
    private void checkWaveComplete() {
        if (waveManager.isWaveComplete() && !waveTransition) {
            waveTransition = true;
            
            inc(GameVars.MONEY, GameVars.WAVE_CLEAR_MONEY + geti(GameVars.LEVEL) * 10);
            
            Text clearText = new Text("WAVE CLEAR!");
            clearText.setFont(Font.font("Monospace", 32));
            clearText.setFill(Color.LIME);
            clearText.setTranslateX(getAppWidth() / 2 - 100);
            clearText.setTranslateY(getAppHeight() / 2);
            
            getGameScene().addUINode(clearText);
            
            runOnce(() -> {
                getGameScene().removeUINode(clearText);
                inc(GameVars.LEVEL, 1);
                startWave();
            }, Duration.seconds(2));
        }
    }
    
    private void triggerGameOver() {
        gameOver = true;
        
        Rectangle overlay = new Rectangle(getAppWidth(), getAppHeight());
        overlay.setFill(Color.rgb(0, 0, 0, 0.7));
        
        Text gameOverText = new Text("GAME OVER");
        gameOverText.setFont(Font.font("Monospace", 48));
        gameOverText.setFill(Color.RED);
        gameOverText.setTranslateX(getAppWidth() / 2 - 140);
        gameOverText.setTranslateY(getAppHeight() / 2 - 50);
        
        Text scoreText = new Text("Final Score: " + geti(GameVars.SCORE));
        scoreText.setFont(Font.font("Monospace", 24));
        scoreText.setFill(Color.WHITE);
        scoreText.setTranslateX(getAppWidth() / 2 - 100);
        scoreText.setTranslateY(getAppHeight() / 2);
        
        Text levelText = new Text("Level Reached: " + geti(GameVars.LEVEL));
        levelText.setFont(Font.font("Monospace", 24));
        levelText.setFill(Color.WHITE);
        levelText.setTranslateX(getAppWidth() / 2 - 100);
        levelText.setTranslateY(getAppHeight() / 2 + 35);
        
        Text restartText = new Text("Press R to Restart / Drücke R zum Neustarten");
        restartText.setFont(Font.font("Monospace", 18));
        restartText.setFill(Color.YELLOW);
        restartText.setTranslateX(getAppWidth() / 2 - 200);
        restartText.setTranslateY(getAppHeight() / 2 + 100);
        
        getGameScene().addUINode(overlay);
        getGameScene().addUINode(gameOverText);
        getGameScene().addUINode(scoreText);
        getGameScene().addUINode(levelText);
        getGameScene().addUINode(restartText);
    }
    
    private void restartGame() {
        getGameController().startNewGame();
    }
    
    private static final double MAX_TPF = 1.0 / 30.0;
    
    @Override
    protected void onUpdate(double tpf) {
        if (gameOver || player == null) return;
        
        tpf = Math.min(tpf, MAX_TPF);
        
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        
        if (movingLeft) {
            pc.moveLeft(tpf);
        }
        if (movingRight) {
            pc.moveRight(tpf);
        }
        
        if (waveManager != null && !waveTransition) {
            waveManager.update(tpf);
        }
    }
    
    private Text[] extraLetterTexts = new Text[5];
    private Rectangle ammoBar;
    private Rectangle weaponBar;
    private Rectangle livesBar;
    
    @Override
    protected void initUI() {
        int railWidth = GameVars.RAIL_WIDTH;
        int xOffset = 6;
        int yStart = 12;
        
        Text moneyLabel = new Text();
        moneyLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        moneyLabel.setFill(Color.YELLOW);
        moneyLabel.setTranslateX(xOffset);
        moneyLabel.setTranslateY(yStart + 12);
        moneyLabel.textProperty().bind(getip(GameVars.MONEY).asString("%d$"));
        
        DropShadow moneyShadow = new DropShadow(2, Color.BLACK);
        moneyLabel.setEffect(moneyShadow);
        getGameScene().addUINode(moneyLabel);
        
        int extraY = yStart + 35;
        Color[] letterColors = {
            Color.rgb(255, 80, 80),
            Color.rgb(80, 255, 80),
            Color.rgb(80, 180, 255),
            Color.rgb(255, 180, 80),
            Color.rgb(200, 80, 255)
        };
        
        for (int i = 0; i < 5; i++) {
            char letter = GameVars.EXTRA_LETTERS[i];
            Text letterText = new Text(String.valueOf(letter));
            letterText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
            letterText.setFill(Color.rgb(60, 60, 70));
            letterText.setTranslateX(xOffset + 20);
            letterText.setTranslateY(extraY + i * 22);
            
            extraLetterTexts[i] = letterText;
            final int idx = i;
            final Color litColor = letterColors[i];
            
            getip(GameVars.EXTRA_VARS[i]).addListener((obs, oldVal, newVal) -> {
                updateExtraLetter(idx, newVal.intValue() > 0, litColor);
            });
            
            getGameScene().addUINode(letterText);
        }
        
        int barsY = extraY + 5 * 22 + 15;
        int barWidth = railWidth - 14;
        int barHeight = 8;
        int barSpacing = 14;
        
        Rectangle ammoBarBg = createBarBackground(xOffset, barsY, barWidth, barHeight);
        ammoBar = createStatusBar(xOffset + 1, barsY + 1, barWidth - 2, barHeight - 2, Color.DEEPSKYBLUE);
        getGameScene().addUINode(ammoBarBg);
        getGameScene().addUINode(ammoBar);
        
        Rectangle weaponBarBg = createBarBackground(xOffset, barsY + barSpacing, barWidth, barHeight);
        weaponBar = createStatusBar(xOffset + 1, barsY + barSpacing + 1, barWidth - 2, barHeight - 2, Color.ORANGE);
        getGameScene().addUINode(weaponBarBg);
        getGameScene().addUINode(weaponBar);
        
        Rectangle livesBarBg = createBarBackground(xOffset, barsY + barSpacing * 2, barWidth, barHeight);
        livesBar = createStatusBar(xOffset + 1, barsY + barSpacing * 2 + 1, barWidth - 2, barHeight - 2, Color.LIMEGREEN);
        getGameScene().addUINode(livesBarBg);
        getGameScene().addUINode(livesBar);
        
        getip(GameVars.ACTIVE_BULLETS).addListener((obs, o, n) -> updateBars());
        getip(GameVars.AMMO_CAP).addListener((obs, o, n) -> updateBars());
        getip(GameVars.WEAPON_GRADE).addListener((obs, o, n) -> updateBars());
        getip(GameVars.LIVES).addListener((obs, o, n) -> updateBars());
        updateBars();
        
        Text scoreLabel = new Text();
        scoreLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        scoreLabel.setFill(Color.WHITE);
        scoreLabel.setTranslateX(getAppWidth() / 2 - 50);
        scoreLabel.setTranslateY(20);
        scoreLabel.textProperty().bind(getip(GameVars.SCORE).asString("SCORE %d"));
        
        DropShadow scoreShadow = new DropShadow(3, Color.BLACK);
        scoreLabel.setEffect(scoreShadow);
        getGameScene().addUINode(scoreLabel);
        
        Text levelLabel = new Text();
        levelLabel.setFont(Font.font("Monospace", 11));
        levelLabel.setFill(Color.LIGHTGRAY);
        levelLabel.setTranslateX(getAppWidth() / 2 - 30);
        levelLabel.setTranslateY(35);
        levelLabel.textProperty().bind(getip(GameVars.LEVEL).asString("WAVE %d"));
        getGameScene().addUINode(levelLabel);
    }
    
    private Rectangle createBarBackground(int x, int y, int width, int height) {
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(Color.rgb(20, 25, 35));
        bg.setStroke(Color.rgb(80, 90, 110));
        bg.setStrokeWidth(1);
        bg.setTranslateX(x);
        bg.setTranslateY(y);
        bg.setArcWidth(4);
        bg.setArcHeight(4);
        return bg;
    }
    
    private Rectangle createStatusBar(int x, int y, int width, int height, Color color) {
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.brighter()),
                new Stop(0.5, color),
                new Stop(1, color.darker()));
        
        Rectangle bar = new Rectangle(width, height);
        bar.setFill(gradient);
        bar.setTranslateX(x);
        bar.setTranslateY(y);
        bar.setArcWidth(3);
        bar.setArcHeight(3);
        
        Glow glow = new Glow(0.3);
        bar.setEffect(glow);
        
        return bar;
    }
    
    private void updateExtraLetter(int idx, boolean lit, Color litColor) {
        Text letterText = extraLetterTexts[idx];
        if (lit) {
            letterText.setFill(litColor);
            Glow glow = new Glow(0.6);
            DropShadow shadow = new DropShadow(6, litColor);
            glow.setInput(shadow);
            letterText.setEffect(glow);
        } else {
            letterText.setFill(Color.rgb(60, 60, 70));
            letterText.setEffect(null);
        }
    }
    
    private void updateBars() {
        int railWidth = GameVars.RAIL_WIDTH;
        int barWidth = railWidth - 16;
        
        int available = geti(GameVars.AMMO_CAP) - geti(GameVars.ACTIVE_BULLETS);
        int cap = geti(GameVars.AMMO_CAP);
        double ammoRatio = cap > 0 ? (double) available / cap : 0;
        ammoBar.setWidth(Math.max(1, barWidth * ammoRatio));
        
        int weapon = geti(GameVars.WEAPON_GRADE);
        double weaponRatio = (double) weapon / GameVars.MAX_WEAPON_GRADE;
        weaponBar.setWidth(Math.max(1, barWidth * weaponRatio));
        
        int lives = geti(GameVars.LIVES);
        double livesRatio = Math.min(1.0, (double) lives / 5);
        livesBar.setWidth(Math.max(1, barWidth * livesRatio));
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
