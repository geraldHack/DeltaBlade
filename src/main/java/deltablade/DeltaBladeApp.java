package deltablade;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import deltablade.components.BulletComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlayerComponent;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
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
        
        waveManager = new WaveManager();
        
        spawnPlayer();
        startWave();
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
    
    private void fire() {
        if (player == null || gameOver) return;
        
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        if (!pc.canFire()) return;
        
        pc.onFired();
        int grade = geti(GameVars.WEAPON_GRADE);
        
        double centerX = pc.getCenterX();
        double topY = pc.getTopY();
        
        spawnBullet(centerX - 2, topY);
        
        if (grade >= 2 && canFireMore()) {
            spawnBullet(centerX - 12, topY + 5);
        }
        
        if (grade >= 3 && canFireMore()) {
            spawnBullet(centerX + 8, topY + 5);
        }
    }
    
    private boolean canFireMore() {
        return geti(GameVars.ACTIVE_BULLETS) < geti(GameVars.AMMO_CAP);
    }
    
    private void spawnBullet(double x, double y) {
        spawn("playerBullet", x, y);
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
        if (random.nextDouble() < 0.25) {
            String pickupType = random.nextDouble() < 0.6 ? "weaponPickup" : "ammoPickup";
            spawn(pickupType, x - 10, y - 10);
        }
    }
    
    private void applyPickup(PickupComponent.PickupType type) {
        if (type == PickupComponent.PickupType.WEAPON_UPGRADE) {
            if (geti(GameVars.WEAPON_GRADE) < GameVars.MAX_WEAPON_GRADE) {
                inc(GameVars.WEAPON_GRADE, 1);
            }
            inc(GameVars.SCORE, 50);
        } else {
            if (geti(GameVars.AMMO_CAP) < GameVars.MAX_AMMO_CAP) {
                inc(GameVars.AMMO_CAP, 1);
            }
            inc(GameVars.SCORE, 25);
        }
    }
    
    private void checkWaveComplete() {
        if (waveManager.isWaveComplete() && !waveTransition) {
            waveTransition = true;
            
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
    
    @Override
    protected void onUpdate(double tpf) {
        if (gameOver || player == null) return;
        
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
    
    @Override
    protected void initUI() {
        Text scoreLabel = new Text();
        scoreLabel.setFont(Font.font("Monospace", 16));
        scoreLabel.setFill(Color.WHITE);
        scoreLabel.setTranslateX(10);
        scoreLabel.setTranslateY(25);
        scoreLabel.textProperty().bind(getip(GameVars.SCORE).asString("SCORE: %d"));
        
        Text levelLabel = new Text();
        levelLabel.setFont(Font.font("Monospace", 16));
        levelLabel.setFill(Color.WHITE);
        levelLabel.setTranslateX(10);
        levelLabel.setTranslateY(45);
        levelLabel.textProperty().bind(getip(GameVars.LEVEL).asString("LEVEL: %d"));
        
        Text livesLabel = new Text();
        livesLabel.setFont(Font.font("Monospace", 16));
        livesLabel.setFill(Color.RED);
        livesLabel.setTranslateX(getAppWidth() - 120);
        livesLabel.setTranslateY(25);
        livesLabel.textProperty().bind(getip(GameVars.LIVES).asString("LIVES: %d"));
        
        Text weaponLabel = new Text();
        weaponLabel.setFont(Font.font("Monospace", 16));
        weaponLabel.setFill(Color.LIME);
        weaponLabel.setTranslateX(getAppWidth() - 120);
        weaponLabel.setTranslateY(45);
        weaponLabel.textProperty().bind(getip(GameVars.WEAPON_GRADE).asString("WEAPON: %d"));
        
        Text ammoLabel = new Text();
        ammoLabel.setFont(Font.font("Monospace", 16));
        ammoLabel.setFill(Color.CYAN);
        ammoLabel.setTranslateX(getAppWidth() / 2 - 50);
        ammoLabel.setTranslateY(25);
        
        getip(GameVars.ACTIVE_BULLETS).addListener((obs, oldVal, newVal) -> {
            updateAmmoText(ammoLabel);
        });
        getip(GameVars.AMMO_CAP).addListener((obs, oldVal, newVal) -> {
            updateAmmoText(ammoLabel);
        });
        updateAmmoText(ammoLabel);
        
        Text enemiesLabel = new Text();
        enemiesLabel.setFont(Font.font("Monospace", 12));
        enemiesLabel.setFill(Color.GRAY);
        enemiesLabel.setTranslateX(getAppWidth() / 2 - 40);
        enemiesLabel.setTranslateY(45);
        enemiesLabel.textProperty().bind(getip(GameVars.ENEMIES_REMAINING).asString("Enemies: %d"));
        
        getGameScene().addUINode(scoreLabel);
        getGameScene().addUINode(levelLabel);
        getGameScene().addUINode(livesLabel);
        getGameScene().addUINode(weaponLabel);
        getGameScene().addUINode(ammoLabel);
        getGameScene().addUINode(enemiesLabel);
    }
    
    private void updateAmmoText(Text ammoLabel) {
        int available = geti(GameVars.AMMO_CAP) - geti(GameVars.ACTIVE_BULLETS);
        int cap = geti(GameVars.AMMO_CAP);
        ammoLabel.setText("AMMO: " + available + "/" + cap);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
