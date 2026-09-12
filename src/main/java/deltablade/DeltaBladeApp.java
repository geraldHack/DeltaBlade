package deltablade;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import deltablade.components.BackgroundScrollComponent;
import deltablade.components.BulletComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.ExtraLetterPickupComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlayerComponent;
import deltablade.components.RankMarkerComponent;
import deltablade.components.StarComponent;
import deltablade.minigames.CognitiveTestGame;
import deltablade.minigames.MeteorStormGame;
import deltablade.minigames.Minigame;
import deltablade.minigames.MinigameHost;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

public class DeltaBladeApp extends GameApplication implements MinigameHost {
    
    private Entity player;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean holdingFire = false;
    private boolean gameOver = false;
    private boolean showingTitleScreen = true;
    private boolean gameStarted = false;
    
    private WaveManager waveManager;
    private boolean waveTransition = false;
    private static final double GET_READY_DURATION = 2.4;
    private double getReadyRemaining = 0;
    private boolean getReadySoundQueued = false;
    private boolean minigameActive = false;
    private Minigame activeMinigame;
    private boolean warping = false;
    private double warpElapsed = 0;
    private Rectangle warpFlash;
    private double extraTime = GameVars.EXTRA_TIME_MAX;
    private boolean extraTimeWarned = false;
    private boolean ufoSpawnedThisWave = false;
    private double extraTimePulse = 0;
    
    private static final Random random = new Random();
    private int frameCount = 0;
    private static final int WARMUP_FRAMES = 5;
    
    private List<Node> titleScreenNodes = new ArrayList<>();
    private List<Animation> extraLetterAnimations = new ArrayList<>();
    private List<Node> activeBanners = new ArrayList<>();
    private int scorePopupSeq = 0;
    private boolean optionsOpen = false;
    private boolean enginePausedByOptions = false;
    private Node optionsRoot;
    private HighScoreOverlay highScoreOverlay;
    private Node highScoreRoot;
    private Text titleHiLine;
    private Timeline titleHiRotate;
    private boolean titleHiGlobal;
    private static boolean escFilterInstalled = false;
    
    private Timeline activeShake = null;
    private List<Node> shakenViewNodes = new ArrayList<>();
    
    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("DeltaBlade");
        settings.setVersion(AppVersion.current());
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
        
        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);
        settings.setScaleAffectedOnResize(true);
        settings.setFullScreenAllowed(true);
        // LaunchServices (Doppelklick) startet mit cwd "/" oder dem read-only DMG.
        // FXGL legt sonst "logs/" an und beendet sich still mit Read-only file system.
        settings.setFileSystemWriteAllowed(false);
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
        vars.put(GameVars.AUTOFIRE, false);
        vars.put(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 0);
        vars.put(GameVars.SQUAD_COMBOS, 0);
        vars.put(GameVars.COGNITIVE_WINS, 0);
        vars.put(GameVars.METEOR_WINS, 0);
        vars.put(GameVars.RANK, 0);
        vars.put(GameVars.RANK_MASK, 0);
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
        
        getInput().addAction(new UserAction("Move Up") {
            @Override
            protected void onAction() {
                movingUp = true;
            }
            @Override
            protected void onActionEnd() {
                movingUp = false;
            }
        }, KeyCode.UP);

        getInput().addAction(new UserAction("Move Up W") {
            @Override
            protected void onAction() {
                movingUp = true;
            }
            @Override
            protected void onActionEnd() {
                movingUp = false;
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Move Down") {
            @Override
            protected void onAction() {
                movingDown = true;
            }
            @Override
            protected void onActionEnd() {
                movingDown = false;
            }
        }, KeyCode.DOWN);

        getInput().addAction(new UserAction("Move Down S") {
            @Override
            protected void onAction() {
                movingDown = true;
            }
            @Override
            protected void onActionEnd() {
                movingDown = false;
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("Fire") {
            @Override
            protected void onActionBegin() {
                holdingFire = true;
                if (!gameOver) {
                    fire();
                }
            }
            @Override
            protected void onAction() {
                holdingFire = true;
                if (!gameOver && !minigameActive && getb(GameVars.AUTOFIRE)) {
                    fire();
                }
            }
            @Override
            protected void onActionEnd() {
                holdingFire = false;
            }
        }, KeyCode.SPACE);
        
        bindTestDigit("Test 1", KeyCode.DIGIT1, KeyCode.NUMPAD1);
        bindTestDigit("Test 2", KeyCode.DIGIT2, KeyCode.NUMPAD2);
        bindTestDigit("Test 3", KeyCode.DIGIT3, KeyCode.NUMPAD3);
        bindTestDigit("Test 4", KeyCode.DIGIT4, KeyCode.NUMPAD4);
        bindTestDigit("Test 5", KeyCode.DIGIT5, KeyCode.NUMPAD5);
        bindTestDigit("Test 6", KeyCode.DIGIT6, KeyCode.NUMPAD6);
        bindTestDigit("Test 7", KeyCode.DIGIT7, KeyCode.NUMPAD7);
        bindTestDigit("Test 8", KeyCode.DIGIT8, KeyCode.NUMPAD8);
        bindTestDigit("Test 9", KeyCode.DIGIT9, KeyCode.NUMPAD9);

        getInput().addAction(new UserAction("Fire X") {
            @Override
            protected void onActionBegin() {
                holdingFire = true;
                if (!gameOver) {
                    fire();
                }
            }
            @Override
            protected void onAction() {
                holdingFire = true;
                if (!gameOver && !minigameActive && getb(GameVars.AUTOFIRE)) {
                    fire();
                }
            }
            @Override
            protected void onActionEnd() {
                holdingFire = false;
            }
        }, KeyCode.X);

        getInput().addAction(new UserAction("Minigame Bonus") {
            @Override
            protected void onActionBegin() {
                if (minigameActive && activeMinigame != null) {
                    activeMinigame.onBonusKey();
                }
            }
        }, KeyCode.B);

        getInput().addAction(new UserAction("Minigame Bonus Click") {
            @Override
            protected void onActionBegin() {
                if (minigameActive && activeMinigame != null) {
                    activeMinigame.onBonusKey();
                }
            }
        }, MouseButton.SECONDARY);
        
        getInput().addAction(new UserAction("Restart") {
            @Override
            protected void onActionBegin() {
                if (gameOver && !optionsOpen && !enteringHighScoreName()) {
                    restartGame();
                }
            }
        }, KeyCode.R);
    }

    private void bindTestDigit(String name, KeyCode... codes) {
        for (int i = 0; i < codes.length; i++) {
            KeyCode code = codes[i];
            getInput().addAction(new UserAction(name + " " + code) {
                @Override
                protected void onActionBegin() {
                    TestMode.feed(code).ifPresent(DeltaBladeApp.this::dropTestMinigame);
                }
            }, code);
        }
    }
    
    private static boolean initialWindowSizeSet = false;
    
    @Override
    protected void initGame() {
        getGameScene().clearUINodes();
        
        gameOver = false;
        movingLeft = false;
        movingRight = false;
        waveTransition = false;
        getReadyRemaining = 0;
        getReadySoundQueued = false;
        showingTitleScreen = true;
        gameStarted = false;
        optionsOpen = false;
        enginePausedByOptions = false;
        optionsRoot = null;
        highScoreOverlay = null;
        highScoreRoot = null;
        player = null;
        waveManager = null;
        minigameActive = false;
        activeMinigame = null;
        stopWarpVisuals();
        holdingFire = false;
        movingUp = false;
        movingDown = false;
        frameCount = 0;
        
        getGameWorld().addEntityFactory(new DeltaBladeFactory());
        applySystemCursor();
        installTestKeyFilter();
        
        getGameScene().getViewport().setX(0);
        getGameScene().getViewport().setY(0);
        
        if (!initialWindowSizeSet) {
            initialWindowSizeSet = true;
            setInitialWindowSize();
        }
        
        spawn("background", new com.almasb.fxgl.entity.SpawnData(0, 0)
                .put("width", getAppWidth())
                .put("height", getAppHeight()));
        
        spawnStars();
        spawnSideRails();
        
        showTitleScreen();
    }
    
    private void setInitialWindowSize() {
        Platform.runLater(() -> {
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            double screenHeight = screen.getVisualBounds().getHeight();
            double screenWidth = screen.getVisualBounds().getWidth();
            
            double margin = 120;
            double availableHeight = screenHeight - margin;
            double availableWidth = screenWidth - margin;
            
            double maxScaleByHeight = availableHeight / 600.0;
            double maxScaleByWidth = availableWidth / 800.0;
            double maxScale = Math.min(maxScaleByHeight, maxScaleByWidth);
            
            int integerScale = (int) maxScale;
            double scale;
            if (integerScale >= 2) {
                scale = integerScale;
            } else if (maxScale >= 1.5) {
                scale = Math.floor(maxScale * 2) / 2;
            } else {
                scale = Math.max(1.0, maxScale);
            }
            
            double windowWidth = 800 * scale;
            double windowHeight = 600 * scale;
            
            javafx.stage.Stage stage = getPrimaryStage();
            stage.setWidth(windowWidth);
            stage.setHeight(windowHeight);
            stage.centerOnScreen();
        });
    }
    
    private void showTitleScreen() {
        titleScreenNodes.clear();
        
        String preloadError = EmbeddedTextures.preloadAll();
        
        Rectangle overlay = new Rectangle(getAppWidth(), getAppHeight());
        overlay.setFill(Color.rgb(0, 0, 0, 0.85));
        
        Text title = new Text("DELTABLADE");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 56));
        title.setFill(Color.CYAN);
        title.setStroke(Color.WHITE);
        title.setStrokeWidth(2);
        
        DropShadow titleGlow = new DropShadow(20, Color.CYAN);
        Glow glow = new Glow(0.6);
        glow.setInput(titleGlow);
        title.setEffect(glow);
        
        centerHorizontally(title, 150);
        
        Text subtitle = new Text("- ARCADE SHOOTER -");
        subtitle.setFont(Font.font("Monospace", 16));
        subtitle.setFill(Color.LIGHTGRAY);
        centerHorizontally(subtitle, 188);

        Text versionLine = new Text(AppVersion.label());
        versionLine.setFont(Font.font("Monospace", 12));
        versionLine.setFill(Color.rgb(120, 160, 190));
        centerHorizontally(versionLine, 208);

        titleHiGlobal = false;
        titleHiLine = new Text(titleHiText(false));
        titleHiLine.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        titleHiLine.setFill(Color.GOLD);
        centerHorizontally(titleHiLine, 226);
        
        Button startButton = createMenuButton("START GAME");
        startButton.setOnAction(e -> startActualGame());
        
        Button optionsButton = createMenuButton("OPTIONEN");
        optionsButton.setOnAction(e -> showOptions());

        Button hiscoreButton = createMenuButton("HISCORE");
        hiscoreButton.setOnAction(e -> showHighScoreTable(null, null, true, true));
        
        VBox menuBox = new VBox(12, startButton, optionsButton, hiscoreButton);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setPrefWidth(260);
        menuBox.setTranslateX(getAppWidth() / 2.0 - 130);
        menuBox.setTranslateY(240);
        
        Text controls = new Text("Pfeiltasten/A,D = Bewegen | SPACE/X = Feuer (tippen)");
        controls.setFont(Font.font("Monospace", 12));
        controls.setFill(Color.GRAY);
        centerHorizontally(controls, 455);
        
        Text extraInfo = new Text("B = Schüsse gleichzeitig | W = Waffe | EXTRA = Leben");
        extraInfo.setFont(Font.font("Monospace", 12));
        extraInfo.setFill(Color.GOLD);
        centerHorizontally(extraInfo, 478);
        
        Text optionsHint = new Text("ESC = Optionen");
        optionsHint.setFont(Font.font("Monospace", 11));
        optionsHint.setFill(Color.rgb(120, 160, 190));
        centerHorizontally(optionsHint, 500);

        titleScreenNodes.add(overlay);
        titleScreenNodes.add(title);
        titleScreenNodes.add(subtitle);
        titleScreenNodes.add(versionLine);
        titleScreenNodes.add(titleHiLine);
        titleScreenNodes.add(menuBox);
        titleScreenNodes.add(controls);
        titleScreenNodes.add(extraInfo);
        titleScreenNodes.add(optionsHint);
        
        for (Node node : titleScreenNodes) {
            getGameScene().addUINode(node);
        }
        
        installEscFilter();
        MusicHelper.applyFromStore();
        SoundHelper.applyMasterVolume();
        
        if (preloadError != null) {
            showBanner(preloadError, Color.RED);
        }
        startTitleHiRotate();
    }
    
    private void centerHorizontally(Node node, double y) {
        var bounds = node.getLayoutBounds();
        node.setTranslateX((getAppWidth() - bounds.getWidth()) / 2.0 - bounds.getMinX());
        node.setTranslateY(y);
    }

    private Button createMenuButton(String label) {
        Button button = new Button(label);
        button.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
        button.setPrefWidth(240);
        String base =
            "-fx-background-color: linear-gradient(to bottom, #2a5298, #1e3c72);" +
            "-fx-text-fill: white;" +
            "-fx-padding: 10 40;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #4a90d9;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: linear-gradient(to bottom, #3a6ab8, #2e4c82);" +
            "-fx-text-fill: white;" +
            "-fx-padding: 10 40;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #6ab0f9;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        return button;
    }
    
    /**
     * FXGL's bundled cursor image is corrupt pixel data, not a real pointer.
     * Replace it with the OS cursor so we don't show a giant blob.
     */
    private void applySystemCursor() {
        getGameScene().setCursor(Cursor.DEFAULT);
        Platform.runLater(() -> {
            var scene = getPrimaryStage().getScene();
            if (scene != null) {
                scene.setCursor(Cursor.DEFAULT);
            }
        });
    }

    private void installEscFilter() {
        if (escFilterInstalled) {
            return;
        }
        Platform.runLater(() -> {
            var scene = getPrimaryStage().getScene();
            if (scene == null) {
                return;
            }
            scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (highScoreOverlay != null) {
                    KeyCode code = e.getCode();
                    boolean fireHeld = holdingFire && (code == KeyCode.SPACE || code == KeyCode.X);
                    if (enteringHighScoreName() && fireHeld) {
                        e.consume();
                        return;
                    }
                    if (highScoreOverlay.handleKey(e)) {
                        e.consume();
                        return;
                    }
                }
                if (enteringHighScoreName()) {
                    e.consume();
                    return;
                }
                if (e.getCode() == KeyCode.ESCAPE) {
                    toggleOptions();
                    e.consume();
                }
            });
            escFilterInstalled = true;
        });
    }

    private void installTestKeyFilter() {
        var root = getGameScene().getRoot();
        if (Boolean.TRUE.equals(root.getProperties().get("dbTestKeys"))) {
            return;
        }
        root.getProperties().put("dbTestKeys", true);
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            TestMode.feed(e.getCode()).ifPresent(this::dropTestMinigame);
        });
    }
    
    private void toggleOptions() {
        if (optionsOpen) {
            hideOptions();
        } else {
            showOptions();
        }
    }
    
    private void showOptions() {
        if (optionsOpen || enteringHighScoreName()) {
            return;
        }
        OptionsOverlay overlay = new OptionsOverlay(this::hideOptions);
        optionsRoot = overlay.getRoot();
        getGameScene().addUINode(optionsRoot);
        optionsOpen = true;
        
        if (gameStarted && !showingTitleScreen) {
            getGameController().pauseEngine();
            enginePausedByOptions = true;
        }
    }
    
    private void hideOptions() {
        if (!optionsOpen) {
            return;
        }
        if (optionsRoot != null) {
            getGameScene().removeUINode(optionsRoot);
            optionsRoot = null;
        }
        optionsOpen = false;
        if (enginePausedByOptions) {
            getGameController().resumeEngine();
            enginePausedByOptions = false;
        }
    }
    
    /**
     * Show a high-contrast full-width banner at the top of the screen.
     * Visible over title screen and during gameplay.
     */
    private void showBanner(String message, Color textColor) {
        showBanner(message, textColor, 2.0);
    }
    
    private void showBanner(String message, Color textColor, double durationSeconds) {
        int railWidth = GameVars.RAIL_WIDTH;
        int innerWidth = getAppWidth() - 2 * railWidth;
        
        Rectangle bar = new Rectangle(innerWidth, 48);
        bar.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 16, 28, 0.96)),
                new Stop(0.5, Color.rgb(16, 28, 44, 0.96)),
                new Stop(1, Color.rgb(8, 16, 28, 0.96))));
        bar.setStroke(textColor);
        bar.setStrokeWidth(1.5);
        bar.setTranslateX(railWidth);
        bar.setTranslateY(48);
        
        Rectangle topLine = new Rectangle(innerWidth, 2);
        topLine.setFill(textColor);
        topLine.setOpacity(0.85);
        topLine.setTranslateX(railWidth);
        topLine.setTranslateY(48);
        
        Rectangle bottomLine = new Rectangle(innerWidth, 2);
        bottomLine.setFill(textColor);
        bottomLine.setOpacity(0.85);
        bottomLine.setTranslateX(railWidth);
        bottomLine.setTranslateY(94);
        
        Text text = new Text(message);
        text.setFont(Font.font("Monospace", FontWeight.BOLD, 26));
        text.setFill(textColor);
        text.setStroke(Color.rgb(0, 0, 0, 0.8));
        text.setStrokeWidth(1.2);
        DropShadow glow = new DropShadow(18, textColor);
        glow.setSpread(0.25);
        text.setEffect(glow);
        
        double textWidth = text.getLayoutBounds().getWidth();
        text.setTranslateX(railWidth + (innerWidth - textWidth) / 2);
        text.setTranslateY(80);
        
        Group banner = new Group(bar, topLine, bottomLine, text);
        
        activeBanners.add(banner);
        getGameScene().addUINode(banner);
        
        runOnce(() -> {
            getGameScene().removeUINode(banner);
            activeBanners.remove(banner);
        }, Duration.seconds(durationSeconds));
    }
    
    private void hideTitleScreen() {
        stopTitleHiRotate();
        for (Node node : titleScreenNodes) {
            getGameScene().removeUINode(node);
        }
        titleScreenNodes.clear();
        titleHiLine = null;
    }
    
    private void startActualGame() {
        startActualGame(true);
    }

    private void startActualGame(boolean getReady) {
        hideOptions();
        hideHighScoreOverlay();
        hideTitleScreen();
        showingTitleScreen = false;
        gameStarted = true;
        frameCount = 0;
        
        resetGameVars();
        
        waveManager = new WaveManager();
        spawnPlayer();
        if (getReady) {
            startGetReady();
        } else {
            beginWave();
        }
    }
    
    private void resetGameVars() {
        set(GameVars.SCORE, 0);
        set(GameVars.LIVES, GameVars.INITIAL_LIVES);
        set(GameVars.LEVEL, 1);
        set(GameVars.WEAPON_GRADE, 1);
        set(GameVars.AMMO_CAP, GameVars.INITIAL_AMMO_CAP);
        set(GameVars.ACTIVE_BULLETS, 0);
        set(GameVars.ENEMIES_REMAINING, 0);
        set(GameVars.MONEY, 0);
        set(GameVars.AUTOFIRE, false);
        set(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 0);
        set(GameVars.SQUAD_COMBOS, 0);
        set(GameVars.COGNITIVE_WINS, 0);
        set(GameVars.METEOR_WINS, 0);
        set(GameVars.RANK, 0);
        set(GameVars.RANK_MASK, 0);
        extraTime = GameVars.EXTRA_TIME_MAX;
        extraTimeWarned = false;
        ufoSpawnedThisWave = false;
        for (String var : GameVars.EXTRA_VARS) {
            set(var, 0);
        }
        updateRankHud();
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
            double size = 1.0 + random.nextDouble() * 1.2;
            double opacity = 0.25 + random.nextDouble() * 0.35;
            double scrollSpeed = 15 + random.nextDouble() * 10;
            
            spawn("scrollingStar", new com.almasb.fxgl.entity.SpawnData(x, y)
                    .put("size", size)
                    .put("opacity", opacity)
                    .put("scrollSpeed", scrollSpeed)
                    .put("isNear", false));
        }
        
        for (int i = 0; i < 40; i++) {
            double x = random.nextDouble() * getAppWidth();
            double y = random.nextDouble() * getAppHeight();
            double size = 1.5 + random.nextDouble() * 2.0;
            double opacity = 0.5 + random.nextDouble() * 0.5;
            double scrollSpeed = 35 + random.nextDouble() * 25;
            
            spawn("scrollingStar", new com.almasb.fxgl.entity.SpawnData(x, y)
                    .put("size", size)
                    .put("opacity", opacity)
                    .put("scrollSpeed", scrollSpeed)
                    .put("isNear", true));
        }
    }
    
    private void spawnPlayer() {
        player = spawn("player", getAppWidth() / 2 - 20, getAppHeight() - 60);
    }
    
    private void startGetReady() {
        if (gameOver || showingTitleScreen || waveManager == null) {
            return;
        }
        waveTransition = true;
        getReadyRemaining = GET_READY_DURATION;
        getReadySoundQueued = true;
        extraTime = GameVars.EXTRA_TIME_MAX;
        extraTimeWarned = false;
        ufoSpawnedThisWave = false;
        updateExtraTimeBar();
        showWaveAnnouncement(WaveManager.resolveWaveType(geti(GameVars.LEVEL)));
    }

    private void cancelGetReady() {
        getReadyRemaining = 0;
        getReadySoundQueued = false;
    }

    private void updateGetReady(double tpf) {
        if (getReadyRemaining <= 0) {
            return;
        }
        if (getReadySoundQueued) {
            getReadySoundQueued = false;
            playWaveIntroSound(WaveManager.resolveWaveType(geti(GameVars.LEVEL)));
        }
        getReadyRemaining -= tpf;
        if (getReadyRemaining > 0) {
            return;
        }
        getReadyRemaining = 0;
        beginWave();
    }

    private void beginWave() {
        if (waveManager == null || gameOver || showingTitleScreen || warping || minigameActive) {
            return;
        }
        cancelGetReady();
        waveTransition = false;
        extraTime = GameVars.EXTRA_TIME_MAX;
        extraTimeWarned = false;
        ufoSpawnedThisWave = false;
        waveManager.startWave(geti(GameVars.LEVEL));
        updateExtraTimeBar();
    }

    private void showWaveAnnouncement() {
        WaveManager.WaveType waveType = waveManager != null
                ? waveManager.getCurrentWaveType()
                : WaveManager.resolveWaveType(geti(GameVars.LEVEL));
        showWaveAnnouncement(waveType);
    }

    private void showWaveAnnouncement(WaveManager.WaveType waveType) {
        String message;
        Color color;
        
        switch (waveType) {
            case BOSS -> {
                message = "BOSS - WAVE " + geti(GameVars.LEVEL);
                color = Color.MAGENTA;
            }
            case KAMIKAZE -> {
                message = "KAMIKAZE - WAVE " + geti(GameVars.LEVEL);
                color = Color.ORANGERED;
            }
            case BONUS -> {
                message = "BONUS - WAVE " + geti(GameVars.LEVEL);
                color = Color.GOLD;
            }
            case RANK -> {
                message = "RANK - WAVE " + geti(GameVars.LEVEL);
                color = Color.HOTPINK;
            }
            default -> {
                message = "WAVE " + geti(GameVars.LEVEL);
                color = Color.YELLOW;
            }
        }
        
        showBanner(message, color, GET_READY_DURATION);
    }

    private void playWaveIntroSound(WaveManager.WaveType waveType) {
        if (waveType == WaveManager.WaveType.BONUS) {
            SoundHelper.play("bonus_round.wav");
        } else {
            SoundHelper.play("get_ready.wav");
        }
    }

    public void showBonusPerfect(int bonus) {
        boolean roundClear = bonus >= GameVars.BONUS_ROUND_PERFECT_SCORE;
        showBanner("PERFECT", Color.GOLD, roundClear ? 2.2 : 1.6);
        showScorePopup(bonus, Color.GOLD, roundClear ? 48 : 36);
        SoundHelper.play("bonus_perfect.wav");
    }

    public void showSquadCombo(int bonus) {
        showBanner("SQUAD COMBO", Color.ORANGE, 1.8);
        showScorePopup(bonus, Color.ORANGE, 36);
        if (comboPlate != null) {
            comboPlate.setStroke(Color.GOLD);
            comboPlate.setStrokeWidth(2);
            runOnce(() -> {
                if (comboPlate != null) {
                    comboPlate.setStroke(Color.rgb(255, 160, 40, 0.45));
                    comboPlate.setStrokeWidth(1.2);
                }
            }, Duration.seconds(1.8));
        }
    }

    private void showScorePopup(int amount, Color color, double fontSize) {
        Text text = new Text("+" + String.format(Locale.GERMAN, "%,d", amount));
        text.setFont(Font.font("Monospace", FontWeight.BOLD, fontSize));
        text.setFill(Color.WHITE);
        text.setStroke(color);
        text.setStrokeWidth(1.8);
        DropShadow glow = new DropShadow(32, color);
        glow.setSpread(0.5);
        text.setEffect(glow);

        double width = text.getLayoutBounds().getWidth();
        text.setTranslateX(-width / 2.0);

        Group popup = new Group(text);
        popup.setMouseTransparent(true);
        popup.setTranslateX(getAppWidth() / 2.0);
        popup.setTranslateY(170 + (scorePopupSeq % 3) * 22);
        popup.setScaleX(0.45);
        popup.setScaleY(0.45);
        scorePopupSeq++;

        getGameScene().addUINode(popup);
        activeBanners.add(popup);

        ScaleTransition pop = new ScaleTransition(Duration.millis(160), popup);
        pop.setToX(1.2);
        pop.setToY(1.2);
        pop.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(Duration.millis(110), popup);
        settle.setToX(1.0);
        settle.setToY(1.0);

        TranslateTransition rise = new TranslateTransition(Duration.seconds(1.2), popup);
        rise.setByY(-64);
        rise.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.85), popup);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(Duration.millis(350));

        SequentialTransition bump = new SequentialTransition(pop, settle);
        ParallelTransition all = new ParallelTransition(bump, rise, fade);
        extraLetterAnimations.add(all);
        all.setOnFinished(e -> {
            extraLetterAnimations.remove(all);
            getGameScene().removeUINode(popup);
            activeBanners.remove(popup);
        });
        all.play();
    }
    
    private static final double BULLET_SPEED = -500;
    
    private void fire() {
        if (minigameActive) {
            if (activeMinigame != null) {
                activeMinigame.onFirePress();
            }
            return;
        }
        if (player == null || gameOver || showingTitleScreen || optionsOpen || warping) return;
        
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        int grade = geti(GameVars.WEAPON_GRADE);
        
        if (!pc.canFire(grade)) return;
        
        pc.onFired();
        SoundHelper.play("shot.wav");
        
        double centerX = pc.getCenterX();
        double topY = pc.getTopY();
        
        if (grade == 1) {
            spawnPlayerShot(centerX - 6, topY, 0);
        } else if (grade == 2) {
            spawnPlayerShot(centerX - 12, topY, 0);
            spawnPlayerShot(centerX + 4, topY, 0);
        } else if (grade == 3) {
            spawnPlayerShot(centerX - 10, topY, -16);
            spawnPlayerShot(centerX - 6, topY, 0);
            spawnPlayerShot(centerX + 2, topY, 16);
        } else {
            spawnPlayerShot(centerX - 14, topY, -20);
            spawnPlayerShot(centerX - 8, topY, -6);
            spawnPlayerShot(centerX, topY, 6);
            spawnPlayerShot(centerX + 6, topY, 20);
        }
    }

    private void spawnPlayerShot(double x, double y, double angleDeg) {
        double rad = Math.toRadians(angleDeg);
        double speed = Math.abs(BULLET_SPEED);
        spawnBullet(x, y, Math.sin(rad) * speed, -Math.cos(rad) * speed);
    }
    
    private void spawnBullet(double x, double y, double speedX, double speedY) {
        spawn("playerBullet", new com.almasb.fxgl.entity.SpawnData(x, y)
                .put("speedX", speedX)
                .put("speedY", speedY));
        inc(GameVars.ACTIVE_BULLETS, 1);
    }

    @Override
    public void spawnExplosion(double centerX, double centerY, String size) {
        spawn("explosion", new com.almasb.fxgl.entity.SpawnData(centerX, centerY)
                .put("size", size));
        
        if ("ship".equals(size) || "big".equals(size)) {
            spawnShockwaveRing(centerX, centerY, size);
            triggerScreenShake(size);
        }
        
        if ("big".equals(size)) {
            spawnFlashOverlay();
        }
    }
    
    private void spawnChipDamageSparks(double hitX, double hitY) {
        for (int i = 0; i < 2; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2 * (8 + random.nextDouble() * 10);
            double offsetY = (random.nextDouble() - 0.5) * 2 * (8 + random.nextDouble() * 10);
            long delayMs = (long) (random.nextDouble() * 50);
            
            runOnce(() -> {
                spawnExplosion(hitX + offsetX, hitY + offsetY, "hit");
            }, Duration.millis(delayMs));
        }
    }
    
    private void flashEnemySprite(Entity enemy) {
        if (enemy == null || !enemy.isActive()) return;
        
        Node view = enemy.getViewComponent().getChildren().isEmpty() 
            ? null : enemy.getViewComponent().getChildren().get(0);
        if (view == null) return;
        
        javafx.scene.effect.ColorAdjust flash = new javafx.scene.effect.ColorAdjust();
        flash.setBrightness(0.7);
        view.setEffect(flash);
        
        runOnce(() -> {
            if (enemy.isActive()) {
                view.setEffect(null);
            }
        }, Duration.millis(80));
    }
    
    private void spawnShockwaveRing(double centerX, double centerY, String size) {
        double maxRadius = "big".equals(size) ? 160 : 110;
        double duration = "big".equals(size) ? 0.35 : 0.25;
        
        Circle ring = new Circle(10);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.rgb(255, 200, 100, 0.9));
        ring.setStrokeWidth(4);
        
        Entity shockwaveEntity = entityBuilder()
                .at(centerX, centerY)
                .view(ring)
                .zIndex(95)
                .build();
        getGameWorld().addEntity(shockwaveEntity);
        
        ScaleTransition scale = new ScaleTransition(Duration.seconds(duration), ring);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(maxRadius / 10.0);
        scale.setToY(maxRadius / 10.0);
        scale.setInterpolator(Interpolator.EASE_OUT);
        
        FadeTransition fade = new FadeTransition(Duration.seconds(duration), ring);
        fade.setFromValue(0.9);
        fade.setToValue(0.0);
        
        Timeline strokeFade = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(ring.strokeWidthProperty(), 4)),
            new KeyFrame(Duration.seconds(duration), new KeyValue(ring.strokeWidthProperty(), 1))
        );
        
        ParallelTransition combo = new ParallelTransition(scale, fade, strokeFade);
        combo.setOnFinished(e -> {
            if (shockwaveEntity.isActive()) {
                shockwaveEntity.removeFromWorld();
            }
        });
        combo.play();
    }
    
    private void resetShakenViews() {
        for (Node node : shakenViewNodes) {
            node.setTranslateX(0);
            node.setTranslateY(0);
        }
        shakenViewNodes.clear();
    }
    
    private void triggerScreenShake(String size) {
        double intensity = "big".equals(size) ? 12 : 6;
        double shakeDuration = 0.18;
        int shakeSteps = 5;
        double stepDuration = shakeDuration / shakeSteps;
        
        if (activeShake != null) {
            activeShake.stop();
        }
        resetShakenViews();
        
        shakenViewNodes.clear();
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (e.isActive()) shakenViewNodes.add(e.getViewComponent().getParent());
        }
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.PLAYER)) {
            if (e.isActive()) shakenViewNodes.add(e.getViewComponent().getParent());
        }
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.ENEMY_BULLET)) {
            if (e.isActive()) shakenViewNodes.add(e.getViewComponent().getParent());
        }
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.PLAYER_BULLET)) {
            if (e.isActive()) shakenViewNodes.add(e.getViewComponent().getParent());
        }
        
        List<Node> currentNodes = new ArrayList<>(shakenViewNodes);
        
        Timeline shake = new Timeline();
        for (int i = 0; i < shakeSteps; i++) {
            double decay = 1.0 - (double) i / shakeSteps;
            final double offsetX = (random.nextDouble() - 0.5) * 2 * intensity * decay;
            final double offsetY = (random.nextDouble() - 0.5) * 2 * intensity * decay;
            shake.getKeyFrames().add(new KeyFrame(Duration.seconds(i * stepDuration),
                e -> {
                    for (Node node : currentNodes) {
                        node.setTranslateX(offsetX);
                        node.setTranslateY(offsetY);
                    }
                }
            ));
        }
        shake.getKeyFrames().add(new KeyFrame(Duration.seconds(shakeDuration),
            e -> {
                for (Node node : currentNodes) {
                    node.setTranslateX(0);
                    node.setTranslateY(0);
                }
            }
        ));
        
        shake.setOnFinished(e -> {
            for (Node node : currentNodes) {
                node.setTranslateX(0);
                node.setTranslateY(0);
            }
            if (activeShake == shake) {
                activeShake = null;
                shakenViewNodes.clear();
            }
        });
        
        activeShake = shake;
        shake.play();
    }
    
    private void spawnFlashOverlay() {
        Rectangle flash = new Rectangle(getAppWidth() + 40, getAppHeight() + 40);
        flash.setFill(Color.rgb(255, 220, 180, 0.10));
        flash.setMouseTransparent(true);
        
        Entity flashEntity = entityBuilder()
                .at(-20, -20)
                .view(flash)
                .zIndex(500)
                .build();
        getGameWorld().addEntity(flashEntity);
        
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.08), flash);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            if (flashEntity.isActive()) {
                flashEntity.removeFromWorld();
            }
        });
        fadeOut.play();
    }
    
    public void spawnEnemyBullet(double x, double y) {
        spawnEnemyBullet(x, y, 0, 250, false);
    }

    public void spawnEnemyShot(EnemyComponent.EnemyType type, double x, double y) {
        switch (type) {
            case FAST, TOUGH -> {
                if (geti(GameVars.LEVEL) < GameVars.ANGLED_FIRE_MIN_LEVEL) {
                    spawnEnemyBullet(x, y, 0, 250, false);
                    SoundHelper.play("enemy_shot.wav");
                } else {
                    spawnAngledEnemyShot(x, y);
                }
            }
            case UFO -> {
                spawnEnemyBullet(x, y, 0, 165, true);
                SoundHelper.play("missile_launch.wav");
            }
            case BOSS -> {
                spawnEnemyBullet(x, y, 0, 250, false);
                SoundHelper.play("enemy_shot.wav");
                if (random.nextDouble() < 0.55) {
                    spawnEnemyBullet(x, y, -90, 240, false);
                    spawnEnemyBullet(x, y, 90, 240, false);
                }
            }
            default -> {
                spawnEnemyBullet(x, y, 0, 250, false);
                SoundHelper.play("enemy_shot.wav");
            }
        }
    }

    private void spawnAngledEnemyShot(double x, double y) {
        double angle = Math.toRadians(22 + random.nextDouble() * 13);
        if (random.nextBoolean()) {
            angle = -angle;
        }
        try {
            var target = getGameWorld().getSingleton(EntityType.PLAYER);
            double dx = target.getX() + target.getWidth() / 2 - x;
            if (dx != 0) {
                angle = Math.copySign(angle, dx);
            }
        } catch (Exception ignored) {
        }
        double speed = 280;
        spawnEnemyBullet(x, y, Math.sin(angle) * speed, Math.cos(angle) * speed, false);
        SoundHelper.play("enemy_shot.wav");
    }

    private void spawnEnemyBullet(double x, double y, double speedX, double speedY, boolean homing) {
        spawn("enemyBullet", new com.almasb.fxgl.entity.SpawnData(x - 5, y)
                .put("speedX", speedX)
                .put("speedY", speedY)
                .put("homing", homing));
    }
    
    public void onSquadMemberSettled(int squadId) {
        waveManager.markSquadSettled(squadId);
    }
    
    public void onEnemyLeftScreen(int squadId) {
        waveManager.onEnemyEscaped(squadId);
        inc(GameVars.ENEMIES_REMAINING, -1);
        waveManager.checkKillBonuses();
        checkWaveComplete();
    }
    
    @Override
    protected void initPhysics() {
        onCollisionBegin(EntityType.PLAYER_BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            bullet.getComponent(BulletComponent.class).onHit();
            bullet.removeFromWorld();
            
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            ec.hit();

            double hitX = bullet.getX() + bullet.getWidth() / 2;
            double hitY = bullet.getY() + bullet.getHeight() / 2;
            
            boolean isToughOrBoss = ec.getType() == EnemyComponent.EnemyType.TOUGH || ec.isBoss();
            boolean isNonFatal = !ec.isDead();
            
            if (isToughOrBoss && isNonFatal) {
                spawnExplosion(hitX, hitY, "boss_hit");
                spawnChipDamageSparks(hitX, hitY);
                flashEnemySprite(enemy);
            } else {
                spawnExplosion(hitX, hitY, "hit");
            }
            
            if (ec.isDead()) {
                inc(GameVars.SCORE, ec.getScoreValue());
                inc(GameVars.ENEMIES_REMAINING, -1);
                
                waveManager.onEnemyDestroyed(ec.getSquadId(), ec.isEntering());

                double deathX = enemy.getX() + enemy.getWidth() / 2;
                double deathY = enemy.getY() + enemy.getHeight() / 2;
                
                boolean isBoss = ec.isBoss();
                
                if (isBoss) {
                    spawnBossDeathSequence(deathX, deathY);
                    spawnBossCoins(deathX, deathY);
                    trySpawnRankMarker(deathX, deathY, ec.getType());
                } else {
                    String explosionSize = (ec.getType() == EnemyComponent.EnemyType.TOUGH) ? "big" : "ship";
                    spawnExplosion(deathX, deathY, explosionSize);
                    SoundHelper.play("explode_ship.wav");
                    trySpawnPickup(deathX, deathY, ec.getType());
                    trySpawnRankMarker(deathX, deathY, ec.getType());
                }

                enemy.removeFromWorld();
                waveManager.checkKillBonuses();
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
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isHarmlessOnContact()) {
                return;
            }
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

        onCollisionBegin(EntityType.RANK_MARKER, EntityType.PLAYER, (marker, playerEntity) -> {
            collectRankMarker(marker.getComponent(RankMarkerComponent.class).getColorIndex());
            marker.removeFromWorld();
        });
        
        onCollisionBegin(EntityType.EXTRA_LETTER_PICKUP, EntityType.PLAYER, (letterOrb, playerEntity) -> {
            ExtraLetterPickupComponent lpc = letterOrb.getComponent(ExtraLetterPickupComponent.class);
            collectExtraLetter(lpc.getLetter(), lpc.getLetterIndex());
            letterOrb.removeFromWorld();
        });
        
        onCollisionBegin(EntityType.COIN, EntityType.PLAYER, (coin, playerEntity) -> {
            deltablade.components.CoinComponent cc = coin.getComponent(deltablade.components.CoinComponent.class);
            inc(GameVars.MONEY, cc.getValue());
            coin.removeFromWorld();
            SoundHelper.play("money.wav");
        });
    }
    
    private void playerHit(PlayerComponent pc) {
        if (minigameActive || warping) {
            return;
        }
        inc(GameVars.LIVES, -1);
        pc.makeInvulnerable();
        
        set(GameVars.AUTOFIRE, false);

        if (player != null) {
            double explosionX = player.getX() + player.getWidth() / 2;
            double explosionY = player.getY() + player.getHeight() / 2;
            spawnExplosion(explosionX, explosionY, "ship");
            SoundHelper.play("explode_ship.wav");
        }
        
        if (geti(GameVars.WEAPON_GRADE) > 1) {
            inc(GameVars.WEAPON_GRADE, -1);
        }
        
        if (geti(GameVars.LIVES) <= 0) {
            triggerGameOver();
        }
    }
    
    private void trySpawnPickup(double x, double y, EnemyComponent.EnemyType enemyType) {
        boolean extraLetterInWorld = !getGameWorld().getEntitiesByType(EntityType.EXTRA_LETTER_PICKUP).isEmpty();
        boolean extraLetterSpawnedThisWave = geti(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE) > 0;
        
        if (!extraLetterInWorld && !extraLetterSpawnedThisWave && random.nextDouble() < GameVars.EXTRA_LETTER_DROP_CHANCE) {
            int letterIndex = getRandomUnownedLetterIndex();
            if (letterIndex >= 0) {
                char letter = GameVars.EXTRA_LETTERS[letterIndex];
                spawn("extraLetterOrb", new com.almasb.fxgl.entity.SpawnData(x - 14, y - 14)
                        .put("letter", letter)
                        .put("letterIndex", letterIndex));
                set(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 1);
                return;
            }
        }
        
        double roll = random.nextDouble();

        if (roll < GameVars.MINIGAME_DROP_CHANCE) {
            spawn(random.nextBoolean() ? "meteorPickup" : "cognitivePickup", x - 14, y - 14);
            return;
        }
        roll -= GameVars.MINIGAME_DROP_CHANCE;
        
        if (roll < GameVars.AUTOFIRE_DROP_CHANCE) {
            if (!getb(GameVars.AUTOFIRE)) {
                spawn("autofirePickup", x - 14, y - 14);
            }
            return;
        }
        roll -= GameVars.AUTOFIRE_DROP_CHANCE;
        
        if (roll < GameVars.TIME_PICKUP_DROP_CHANCE) {
            spawn("timePickup", x - 14, y - 14);
            return;
        }
        roll -= GameVars.TIME_PICKUP_DROP_CHANCE;

        if (roll < GameVars.PICKUP_DROP_CHANCE) {
            String pickupType = random.nextBoolean() ? "weaponPickup" : "ammoPickup";
            spawn(pickupType, x - 14, y - 14);
            return;
        }
        roll -= GameVars.PICKUP_DROP_CHANCE;
        
        int level = geti(GameVars.LEVEL);
        boolean canDropViolet = level >= 8 && enemyType == EnemyComponent.EnemyType.TOUGH;
        if (canDropViolet && roll < GameVars.COIN_VIOLET_DROP_CHANCE) {
            spawnCoin(x, y, "violet");
            return;
        }
        if (canDropViolet) roll -= GameVars.COIN_VIOLET_DROP_CHANCE;
        
        if (roll < GameVars.COIN_WHITE_DROP_CHANCE) {
            spawnCoin(x, y, "white");
            return;
        }
        roll -= GameVars.COIN_WHITE_DROP_CHANCE;
        
        if (roll < GameVars.COIN_GREEN_DROP_CHANCE) {
            spawnCoin(x, y, "green");
            return;
        }
        roll -= GameVars.COIN_GREEN_DROP_CHANCE;
        
        if (roll < GameVars.COIN_BLUE_DROP_CHANCE) {
            spawnCoin(x, y, "blue");
        }
    }
    
    private int getRandomUnownedLetterIndex() {
        List<Integer> unownedIndices = new ArrayList<>();
        for (int i = 0; i < GameVars.EXTRA_VARS.length; i++) {
            if (geti(GameVars.EXTRA_VARS[i]) == 0) {
                unownedIndices.add(i);
            }
        }
        if (unownedIndices.isEmpty()) {
            return -1;
        }
        return unownedIndices.get(random.nextInt(unownedIndices.size()));
    }
    
    private void spawnCoin(double x, double y, String coinType) {
        spawn("coin", new com.almasb.fxgl.entity.SpawnData(x - 8, y - 8)
                .put("coinType", coinType));
    }
    
    private void spawnBossCoins(double x, double y) {
        if (random.nextDouble() < 0.85) {
            spawnCoin(x, y, "violet");
        }
        
        int extraCoins = 2 + random.nextInt(2);
        for (int i = 0; i < extraCoins; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 40;
            double offsetY = (random.nextDouble() - 0.5) * 30;
            double coinRoll = random.nextDouble();
            String type;
            if (coinRoll < 0.5) {
                type = "green";
            } else if (coinRoll < 0.85) {
                type = "blue";
            } else {
                type = "white";
            }
            spawnCoin(x + offsetX, y + offsetY, type);
        }
    }
    
    /**
     * Boss death: 3-5 staggered big explosions with irregular audio.
     * First explosion is immediate with shockwave/flash; others are subdued.
     */
    private void spawnBossDeathSequence(double centerX, double centerY) {
        int boomCount = 3 + random.nextInt(3);
        
        spawnExplosion(centerX, centerY, "big");
        SoundHelper.play("explode_boss.wav");
        
        double accumulatedDelay = 0;
        for (int i = 1; i < boomCount; i++) {
            double delay = 0.08 + random.nextDouble() * 0.27;
            accumulatedDelay += delay;
            
            final double offsetX = (random.nextDouble() - 0.5) * 90;
            final double offsetY = (random.nextDouble() - 0.5) * 70;
            final double finalDelay = accumulatedDelay;
            
            runOnce(() -> {
                spawnBossFollowupExplosion(centerX + offsetX, centerY + offsetY);
                SoundHelper.play("explode_boss.wav");
            }, Duration.seconds(finalDelay));
        }
    }
    
    private void spawnBossFollowupExplosion(double x, double y) {
        spawn("explosion", new com.almasb.fxgl.entity.SpawnData(x, y).put("size", "big"));
    }
    
    private void collectExtraLetter(char letter, int letterIndex) {
        String varName = GameVars.EXTRA_VARS[letterIndex];
        if (geti(varName) == 0) {
            set(varName, 1);
            inc(GameVars.SCORE, 25);
            showBanner(String.valueOf(letter), LETTER_COLORS[letterIndex], 1.0);
            SoundHelper.play("extra.wav");
            
            if (isExtraComplete()) {
                inc(GameVars.LIVES, 1);
                resetExtraLetters();
                showExtraLifeFlash();
                SoundHelper.play("extra_life.wav");
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
        showBanner("+1 LIFE!", Color.GOLD, 1.5);
    }
    
    private void applyPickup(PickupComponent.PickupType type) {
        switch (type) {
            case WEAPON_UPGRADE -> {
                if (geti(GameVars.WEAPON_GRADE) < GameVars.MAX_WEAPON_GRADE) {
                    inc(GameVars.WEAPON_GRADE, 1);
                }
                inc(GameVars.SCORE, 50);
                showBanner("WAFFE", Color.GOLD, 1.2);
                SoundHelper.play("weapon.wav");
            }
            case EXTRA_AMMO -> {
                if (geti(GameVars.AMMO_CAP) < GameVars.MAX_AMMO_CAP) {
                    inc(GameVars.AMMO_CAP, 1);
                }
                inc(GameVars.SCORE, 25);
                showBanner("MUNI", Color.CYAN, 1.2);
                SoundHelper.play("ammo.wav");
            }
            case AUTOFIRE -> {
                set(GameVars.AUTOFIRE, true);
                inc(GameVars.SCORE, 100);
                showBanner("AUTO", Color.CYAN, 1.2);
                SoundHelper.play("autofire.wav");
            }
            case METEOR -> {
                inc(GameVars.SCORE, 25);
                startMinigame(new MeteorStormGame());
            }
            case COGNITIVE -> {
                inc(GameVars.SCORE, 25);
                SoundHelper.play("cognitive_test.mp3");
                startMinigame(new CognitiveTestGame());
            }
            case EXTRA_TIME -> {
                extraTime = Math.min(GameVars.EXTRA_TIME_MAX, extraTime + GameVars.EXTRA_TIME_REFILL);
                inc(GameVars.SCORE, 25);
                showBanner("TIME", Color.LIMEGREEN, 1.1);
                SoundHelper.play("extra_time.wav");
                updateExtraTimeBar();
            }
            default -> {}
        }
    }

    private void trySpawnRankMarker(double x, double y, EnemyComponent.EnemyType enemyType) {
        if (enemyType != EnemyComponent.EnemyType.TOUGH && enemyType != EnemyComponent.EnemyType.BOSS) {
            return;
        }
        boolean rankWave = waveManager != null && waveManager.getCurrentWaveType() == WaveManager.WaveType.RANK;
        double chance = rankWave ? 1.0 : GameVars.RANK_MARKER_DROP_CHANCE;
        if (enemyType == EnemyComponent.EnemyType.BOSS) {
            chance = 1.0;
        }
        if (random.nextDouble() > chance) {
            return;
        }
        int mask = geti(GameVars.RANK_MASK);
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < GameVars.RANK_COLOR_COUNT; i++) {
            if ((mask & (1 << i)) == 0) {
                missing.add(i);
            }
        }
        int colorIndex = missing.isEmpty()
                ? random.nextInt(GameVars.RANK_COLOR_COUNT)
                : missing.get(random.nextInt(missing.size()));
        spawn("rankMarker", new com.almasb.fxgl.entity.SpawnData(x - 10, y - 10)
                .put("colorIndex", colorIndex));
    }

    private void collectRankMarker(int colorIndex) {
        int bit = 1 << colorIndex;
        int mask = geti(GameVars.RANK_MASK);
        if ((mask & bit) == 0) {
            mask |= bit;
            set(GameVars.RANK_MASK, mask);
        }
        inc(GameVars.SCORE, 50);
        SoundHelper.play("rank_marker.wav");
        updateRankHud();
        if (mask == (1 << GameVars.RANK_COLOR_COUNT) - 1) {
            inc(GameVars.RANK, 1);
            inc(GameVars.SCORE, GameVars.RANK_UP_SCORE);
            set(GameVars.RANK_MASK, 0);
            showBanner(RankNames.banner(geti(GameVars.RANK)), Color.GOLD, 2.0);
            SoundHelper.play("rank_up.wav");
            updateRankHud();
        }
    }
    
    private void checkWaveComplete() {
        if (minigameActive || warping || gameOver) {
            return;
        }
        if (waveManager.isWaveComplete() && !waveTransition) {
            waveTransition = true;
            if (waveManager.getCurrentWaveType() == WaveManager.WaveType.KAMIKAZE) {
                startWarp();
                return;
            }
            double pause = waveManager.getCurrentWaveType() == WaveManager.WaveType.BONUS ? 2.1 : 1.2;
            runOnce(() -> {
                if (gameOver || showingTitleScreen) {
                    return;
                }
                continueToNextWave();
            }, Duration.seconds(pause));
        }
    }
    
    public boolean isCombatStopped() {
        return gameOver || showingTitleScreen;
    }

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }
        gameOver = true;
        cancelGetReady();
        if (waveManager != null) {
            waveManager.stop();
        }
        final int score = geti(GameVars.SCORE);
        final int wave = geti(GameVars.LEVEL);
        Platform.runLater(() -> {
            clearWaveEntities();
            HighScoreClient.fetch(ignored -> {
                if (!gameOver || showingTitleScreen) {
                    return;
                }
                boolean localOk = HighScoreStore.qualifies(score);
                boolean globalOk = HighScoreClient.hasCache()
                        && HighScoreStore.qualifies(score, HighScoreClient.cachedEntries());
                if (localOk || globalOk) {
                    showHighScoreNameEntry(score, wave);
                } else {
                    showHighScoreTable(null, null, false, true);
                }
            });
        });
    }

    private void startTitleHiRotate() {
        stopTitleHiRotate();
        refreshTitleHi();
        HighScoreClient.fetch(ignored -> refreshTitleHi());
        titleHiRotate = new Timeline(new KeyFrame(Duration.seconds(3.5), e -> {
            titleHiGlobal = !titleHiGlobal;
            refreshTitleHi();
        }));
        titleHiRotate.setCycleCount(Animation.INDEFINITE);
        titleHiRotate.play();
    }

    private void stopTitleHiRotate() {
        if (titleHiRotate != null) {
            titleHiRotate.stop();
            titleHiRotate = null;
        }
    }

    private void refreshTitleHi() {
        if (!showingTitleScreen || titleHiLine == null) {
            return;
        }
        if (titleHiGlobal && !HighScoreClient.hasCache()) {
            titleHiGlobal = false;
        }
        titleHiLine.setText(titleHiText(titleHiGlobal));
        centerHorizontally(titleHiLine, 226);
    }

    private String titleHiText(boolean global) {
        var table = global ? HighScoreClient.cachedEntries() : HighScoreStore.entries();
        String tag = global ? "GLOBAL" : "LOKAL";
        if (table.isEmpty()) {
            return "HI " + tag + "  ---  000000";
        }
        var top = table.getFirst();
        return "HI " + tag + "  " + top.name() + "  " + HighScoreStore.formatScore(top.score());
    }

    private boolean enteringHighScoreName() {
        return highScoreOverlay != null && highScoreOverlay.isNameEntry();
    }

    private void showHighScoreNameEntry(int score, int wave) {
        hideHighScoreOverlay();
        highScoreOverlay = HighScoreOverlay.nameEntry(score, wave, this::showHighScoreTableAfterSave);
        highScoreRoot = highScoreOverlay.getRoot();
        getGameScene().addUINode(highScoreRoot);
    }

    private void showHighScoreTableAfterSave(String name, int score, int wave, int localRank) {
        Integer localHighlight = localRank >= 0 ? localRank : null;
        showHighScoreTable(localHighlight, null, false, false);
        HighScoreClient.submit(name, score, wave, result -> {
            if (highScoreOverlay == null || highScoreOverlay.isNameEntry()) {
                return;
            }
            if (result.ok()) {
                Integer globalHighlight = result.rank() >= 0 ? result.rank() : null;
                highScoreOverlay.setGlobalEntries(result.entries(), globalHighlight);
            } else {
                HighScoreClient.fetch(list -> {
                    if (list != null && highScoreOverlay != null && !highScoreOverlay.isNameEntry()) {
                        highScoreOverlay.setGlobalEntries(list);
                    }
                });
            }
        });
    }

    private void showHighScoreTable(Integer localHighlight, Integer globalHighlight, boolean fromTitle, boolean fetchRemote) {
        hideHighScoreOverlay();
        String closeLabel = fromTitle ? "ZURÜCK" : "R = NEUSTART";
        Runnable onClose = fromTitle ? this::hideHighScoreOverlay : null;
        highScoreOverlay = HighScoreOverlay.table(localHighlight, globalHighlight, closeLabel, onClose);
        highScoreRoot = highScoreOverlay.getRoot();
        getGameScene().addUINode(highScoreRoot);
        if (fetchRemote) {
            HighScoreClient.fetch(list -> {
                if (list != null && highScoreOverlay != null && !highScoreOverlay.isNameEntry()) {
                    highScoreOverlay.setGlobalEntries(list);
                }
            });
        }
    }

    private void hideHighScoreOverlay() {
        if (highScoreOverlay != null) {
            highScoreOverlay.dispose();
            highScoreOverlay = null;
        }
        if (highScoreRoot != null) {
            getGameScene().removeUINode(highScoreRoot);
            highScoreRoot = null;
        }
    }
    
    private void restartGame() {
        hideOptions();
        stopExtraLetterAnimations();
        if (activeShake != null) {
            activeShake.stop();
            activeShake = null;
        }
        resetShakenViews();
        var viewport = getGameScene().getViewport();
        viewport.setX(0);
        viewport.setY(0);
        getGameController().startNewGame();
    }
    
    private void stopExtraLetterAnimations() {
        for (Animation anim : extraLetterAnimations) {
            anim.stop();
        }
        extraLetterAnimations.clear();
    }
    
    private void dropTestMinigame(TestMode.Drop drop) {
        if (drop == null) {
            return;
        }
        if (drop.spawnName() == null) {
            showBanner(drop.banner(), Color.LIGHTGRAY, 1.2);
            return;
        }
        if ("WARP".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            startWarp();
            return;
        }
        if ("UFO".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            ufoSpawnedThisWave = false;
            spawnHurryUpUfo();
            showBanner(drop.banner(), Color.CYAN, 1.2);
            return;
        }
        if ("BONUS".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            cancelGetReady();
            clearWaveEntities();
            extraTime = GameVars.EXTRA_TIME_MAX;
            extraTimeWarned = false;
            ufoSpawnedThisWave = false;
            waveTransition = false;
            waveManager.startWave(geti(GameVars.LEVEL), WaveManager.WaveType.BONUS);
            updateExtraTimeBar();
            showWaveAnnouncement();
            SoundHelper.play("bonus_round.wav");
            showBanner(drop.banner(), Color.GOLD, 1.2);
            return;
        }
        if (gameOver || optionsOpen || minigameActive || warping) {
            return;
        }
        if (!gameStarted || showingTitleScreen || player == null) {
            if (showingTitleScreen) {
                startActualGame(false);
            }
            if (player == null) {
                return;
            }
        }
        double x = player.getX() + player.getWidth() / 2 - 14;
        double y = Math.max(56, player.getY() - 90);
        spawn(drop.spawnName(), x, y);
        showBanner(drop.banner(), Color.LIGHTGRAY, 1.2);
    }

    private void startMinigame(Minigame game) {
        if (game == null || gameOver || showingTitleScreen || warping) {
            return;
        }
        waveTransition = true;
        cancelGetReady();
        minigameActive = true;
        clearWaveEntities();
        activeMinigame = game;
        game.start(this);
    }

    private void clearWaveEntities() {
        for (EntityType type : List.of(
                EntityType.ENEMY,
                EntityType.PLAYER_BULLET,
                EntityType.ENEMY_BULLET,
                EntityType.PICKUP,
                EntityType.RANK_MARKER,
                EntityType.EXTRA_LETTER_PICKUP,
                EntityType.COIN,
                EntityType.MINIGAME_HAZARD)) {
            for (Entity entity : List.copyOf(getGameWorld().getEntitiesByType(type))) {
                if (entity.isActive()) {
                    entity.removeFromWorld();
                }
            }
        }
        set(GameVars.ACTIVE_BULLETS, 0);
        set(GameVars.ENEMIES_REMAINING, 0);
    }

    @Override
    public void finishMinigame(boolean won, int scoreBonus, int moneyBonus, String banner, Color color) {
        if (activeMinigame != null) {
            activeMinigame.cleanup();
            activeMinigame = null;
        }
        minigameActive = false;
        MusicHelper.applyFromStore();
        hidePlayer(false);
        for (Entity coin : List.copyOf(getGameWorld().getEntitiesByType(EntityType.COIN))) {
            if (coin.isActive()) {
                coin.removeFromWorld();
            }
        }

        if (scoreBonus > 0) {
            inc(GameVars.SCORE, scoreBonus);
        }
        if (moneyBonus > 0) {
            inc(GameVars.MONEY, moneyBonus);
        }
        if (banner != null && !banner.isBlank()) {
            showBanner(banner, color, 2.0);
        }

        runOnce(() -> {
            continueToNextWave();
        }, Duration.seconds(2));
    }

    private static final double WARP_DURATION = 8.9;

    private void startWarp() {
        if (warping || gameOver || showingTitleScreen) {
            return;
        }
        warping = true;
        waveTransition = true;
        cancelGetReady();
        warpElapsed = 0;
        clearWaveEntities();
        showBanner("WARP", Color.CYAN, 2.2);
        MusicHelper.stop();
        SoundHelper.play("warp.mp3");
        ensureWarpFlash();
        applyWarpVisuals(0);
    }

    private void updateWarp(double tpf) {
        warpElapsed += tpf;
        applyWarpVisuals(warpIntensity(warpElapsed / WARP_DURATION));
        if (warpElapsed >= WARP_DURATION) {
            finishWarp();
        }
    }

    private static double warpIntensity(double t) {
        double x = Math.max(0, Math.min(1, t));
        if (x < 0.14) {
            return easeIn(x / 0.14);
        }
        if (x > 0.82) {
            return 1.0 - easeIn((x - 0.82) / 0.18);
        }
        return 1.0;
    }

    private static double easeIn(double t) {
        return t * t;
    }

    private void finishWarp() {
        stopWarpVisuals();
        MusicHelper.applyFromStore();
        // Shop comes later; warp is the hook, then the next wave cycle.
        continueToNextWave();
    }

    private void continueToNextWave() {
        if (gameOver || showingTitleScreen) {
            return;
        }
        inc(GameVars.LEVEL, 1);
        startGetReady();
    }

    private void ensureWarpFlash() {
        if (warpFlash != null) {
            return;
        }
        warpFlash = new Rectangle(getAppWidth(), getAppHeight());
        warpFlash.setFill(Color.rgb(140, 210, 255, 0.0));
        warpFlash.setMouseTransparent(true);
        getGameScene().addUINode(warpFlash);
    }

    private void applyWarpVisuals(double intensity) {
        StarComponent.setWarp(intensity);
        BackgroundScrollComponent.setWarp(intensity);
        if (warpFlash != null) {
            warpFlash.setFill(Color.rgb(150, 220, 255, 0.04 + intensity * 0.16));
        }
    }

    private void stopWarpVisuals() {
        warping = false;
        warpElapsed = 0;
        StarComponent.setWarp(0);
        BackgroundScrollComponent.setWarp(0);
        SoundHelper.stop("warp.mp3");
        if (warpFlash != null) {
            getGameScene().removeUINode(warpFlash);
            warpFlash = null;
        }
    }

    @Override
    public void displayBanner(String message, Color color, double seconds) {
        showBanner(message, color, seconds);
    }

    @Override
    public Entity player() {
        return player;
    }

    @Override
    public boolean holdingFire() {
        return holdingFire;
    }

    @Override
    public boolean movingLeft() {
        return movingLeft;
    }

    @Override
    public boolean movingRight() {
        return movingRight;
    }

    @Override
    public boolean movingUp() {
        return movingUp;
    }

    @Override
    public boolean movingDown() {
        return movingDown;
    }

    @Override
    public double playLeft() {
        return GameVars.RAIL_WIDTH;
    }

    @Override
    public double playRight() {
        return getAppWidth() - GameVars.RAIL_WIDTH;
    }

    @Override
    public double playTop() {
        return 48;
    }

    @Override
    public double playBottom() {
        return getAppHeight();
    }

    @Override
    public void addUi(Node node) {
        getGameScene().addUINode(node);
    }

    @Override
    public void removeUi(Node node) {
        getGameScene().removeUINode(node);
    }

    @Override
    public void hidePlayer(boolean hide) {
        if (player != null && player.isActive()) {
            player.getViewComponent().setOpacity(hide ? 0.0 : 1.0);
        }
    }

    @Override
    public void playMusicOverride(String fileName) {
        MusicHelper.playOverride(fileName);
    }

    @Override
    public void restoreMusic() {
        MusicHelper.applyFromStore();
    }

    @Override
    public void spawnCoinAt(double x, double y, String coinType) {
        spawnCoin(x, y, coinType);
    }

    private static final double MAX_TPF = 1.0 / 45.0;
    
    @Override
    protected void onUpdate(double tpf) {
        if (showingTitleScreen || gameOver || optionsOpen || player == null) return;
        
        frameCount++;
        if (frameCount <= WARMUP_FRAMES) {
            tpf = Math.min(tpf, 0.008);
        } else {
            tpf = Math.min(tpf, MAX_TPF);
        }

        if (minigameActive && activeMinigame != null) {
            if (activeMinigame.usesPlayerShip()) {
                PlayerComponent pc = player.getComponent(PlayerComponent.class);
                if (movingLeft) {
                    pc.moveLeft(tpf);
                }
                if (movingRight) {
                    pc.moveRight(tpf);
                }
                pc.updateIdle();
            }
            activeMinigame.update(tpf);
            return;
        }

        if (warping) {
            PlayerComponent warpPilot = player.getComponent(PlayerComponent.class);
            if (movingLeft) {
                warpPilot.moveLeft(tpf);
            }
            if (movingRight) {
                warpPilot.moveRight(tpf);
            }
            warpPilot.updateIdle();
            updateWarp(tpf);
            return;
        }
        
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        
        if (movingLeft) {
            pc.moveLeft(tpf);
        }
        if (movingRight) {
            pc.moveRight(tpf);
        }
        pc.updateIdle();

        updateGetReady(tpf);
        
        if (waveManager != null && !waveTransition) {
            waveManager.update(tpf);
            updateExtraTime(tpf);
        }
    }

    private void updateExtraTime(double tpf) {
        if (extraTime <= 0) {
            updateExtraTimeBar();
            return;
        }
        extraTime = Math.max(0, extraTime - tpf);
        extraTimePulse += tpf;
        if (!extraTimeWarned && extraTime <= 5) {
            extraTimeWarned = true;
            SoundHelper.play("hurry_warning.wav");
            showBanner("HURRY UP", Color.ORANGERED, 1.2);
        }
        if (extraTime <= 0) {
            extraTime = 0;
            boolean bonus = waveManager != null && waveManager.getCurrentWaveType() == WaveManager.WaveType.BONUS;
            if (!bonus) {
                spawnHurryUpUfo();
            }
        }
        updateExtraTimeBar();
    }

    private void spawnHurryUpUfo() {
        if (ufoSpawnedThisWave || gameOver || showingTitleScreen || warping || minigameActive) {
            return;
        }
        ufoSpawnedThisWave = true;
        double x = getAppWidth() / 2.0 - 48;
        double y = 56;
        spawn("enemy", new com.almasb.fxgl.entity.SpawnData(x, y)
                .put("enemyType", EnemyComponent.EnemyType.UFO)
                .put("level", geti(GameVars.LEVEL)));
        inc(GameVars.ENEMIES_REMAINING, 1);
        SoundHelper.play("ufo_appear.wav");
        showBanner("UFO", Color.CYAN, 1.3);
    }
    
    private Group[] extraLetterGroups = new Group[5];
    private Text[] extraLetterTexts = new Text[5];
    private Group[] extraFlipWrappers = new Group[5];
    private Rectangle ammoBar;
    private Rectangle weaponBar;
    private Rectangle livesBar;
    private Rectangle extraTimeBar;
    private double extraTimeBarMaxWidth;
    private Circle[] rankDots = new Circle[GameVars.RANK_COLOR_COUNT];
    private Text rankTitleText;
    private Rectangle autoLamp;
    private Text autoLampText;
    private Rectangle comboPlate;
    
    private static final Color[] LETTER_COLORS = {
        Color.rgb(255, 80, 80),
        Color.rgb(80, 255, 80),
        Color.rgb(80, 180, 255),
        Color.rgb(255, 180, 80),
        Color.rgb(200, 80, 255)
    };
    
    @Override
    protected void initUI() {
        int railWidth = GameVars.RAIL_WIDTH;
        int xOffset = 4;
        int extraY = 44;
        int letterSpacing = 36;
        
        for (int i = 0; i < 5; i++) {
            char letter = GameVars.EXTRA_LETTERS[i];
            Group letterGroup = createAnimatedLetterSlot(letter, i, xOffset + 22, extraY + i * letterSpacing);
            extraLetterGroups[i] = letterGroup;
            
            final int idx = i;
            getip(GameVars.EXTRA_VARS[i]).addListener((obs, oldVal, newVal) -> {
                updateExtraLetter(idx, newVal.intValue() > 0, LETTER_COLORS[idx]);
            });
            
            getGameScene().addUINode(letterGroup);
        }
        
        int barsY = extraY + 5 * letterSpacing + 18;
        int barWidth = railWidth - 26;
        int barHeight = 8;
        int barSpacing = 28;
        int labelOffset = 14;
        
        Text ammoLabelB = new Text("B");
        ammoLabelB.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        ammoLabelB.setFill(Color.DEEPSKYBLUE);
        ammoLabelB.setTranslateX(xOffset);
        ammoLabelB.setTranslateY(barsY + barHeight);
        getGameScene().addUINode(ammoLabelB);
        
        Rectangle ammoBarBg = createBarBackground(xOffset + labelOffset, barsY, barWidth, barHeight);
        ammoBar = createStatusBar(xOffset + labelOffset + 1, barsY + 1, barWidth - 2, barHeight - 2, Color.DEEPSKYBLUE);
        getGameScene().addUINode(ammoBarBg);
        getGameScene().addUINode(ammoBar);
        
        Text weaponLabelW = new Text("W");
        weaponLabelW.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        weaponLabelW.setFill(Color.ORANGE);
        weaponLabelW.setTranslateX(xOffset);
        weaponLabelW.setTranslateY(barsY + barSpacing + barHeight);
        getGameScene().addUINode(weaponLabelW);
        
        Rectangle weaponBarBg = createBarBackground(xOffset + labelOffset, barsY + barSpacing, barWidth, barHeight);
        weaponBar = createStatusBar(xOffset + labelOffset + 1, barsY + barSpacing + 1, barWidth - 2, barHeight - 2, Color.ORANGE);
        getGameScene().addUINode(weaponBarBg);
        getGameScene().addUINode(weaponBar);
        
        Rectangle livesBarBg = createBarBackground(xOffset + labelOffset, barsY + barSpacing * 2, barWidth, barHeight);
        livesBar = createStatusBar(xOffset + labelOffset + 1, barsY + barSpacing * 2 + 1, barWidth - 2, barHeight - 2, Color.LIMEGREEN);
        getGameScene().addUINode(livesBarBg);
        getGameScene().addUINode(livesBar);

        Text timeLabelT = new Text("T");
        timeLabelT.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        timeLabelT.setFill(Color.LIMEGREEN);
        timeLabelT.setTranslateX(xOffset);
        timeLabelT.setTranslateY(barsY + barSpacing * 3 + barHeight);
        getGameScene().addUINode(timeLabelT);

        Rectangle timeBarBg = createBarBackground(xOffset + labelOffset, barsY + barSpacing * 3, barWidth, barHeight);
        extraTimeBar = createStatusBar(xOffset + labelOffset + 1, barsY + barSpacing * 3 + 1, barWidth - 2, barHeight - 2, Color.LIME);
        extraTimeBarMaxWidth = barWidth - 2;
        getGameScene().addUINode(timeBarBg);
        getGameScene().addUINode(extraTimeBar);

        int rankY = barsY + barSpacing * 4 + 18;
        double dotRadius = 5.2;
        double dotGapX = 17;
        double dotGapY = 16;
        double dotStartX = xOffset + 11;
        for (int i = 0; i < GameVars.RANK_COLOR_COUNT; i++) {
            int col = i % 3;
            int row = i / 3;
            Circle dot = new Circle(dotRadius);
            dot.setCenterX(dotStartX + col * dotGapX);
            dot.setCenterY(rankY + row * dotGapY);
            dot.setStrokeWidth(2.1);
            rankDots[i] = dot;
            styleRankDot(dot, i, false);
            getGameScene().addUINode(dot);
        }
        rankTitleText = new Text();
        rankTitleText.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        rankTitleText.setFill(Color.GOLD);
        rankTitleText.setWrappingWidth(railWidth - 8);
        rankTitleText.setTranslateX(xOffset);
        rankTitleText.setTranslateY(rankY + dotGapY + 16);
        getGameScene().addUINode(rankTitleText);
        updateRankHud();
        
        int autoY = rankY + 52;
        autoLamp = new Rectangle(12, 12);
        autoLamp.setFill(Color.rgb(40, 40, 40));
        autoLamp.setStroke(Color.rgb(80, 80, 80));
        autoLamp.setStrokeWidth(1);
        autoLamp.setArcWidth(3);
        autoLamp.setArcHeight(3);
        autoLamp.setTranslateX(xOffset);
        autoLamp.setTranslateY(autoY);
        getGameScene().addUINode(autoLamp);
        
        autoLampText = new Text("AUTO");
        autoLampText.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        autoLampText.setFill(Color.rgb(60, 60, 60));
        autoLampText.setTranslateX(xOffset + 15);
        autoLampText.setTranslateY(autoY + 10);
        getGameScene().addUINode(autoLampText);
        
        int moneyY = autoY + 36;
        Text moneyLabel = new Text();
        moneyLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        moneyLabel.setFill(Color.GOLD);
        moneyLabel.setTranslateX(xOffset);
        moneyLabel.setTranslateY(moneyY);
        moneyLabel.textProperty().bind(getip(GameVars.MONEY).asString("$%d"));
        DropShadow moneyShadow = new DropShadow(2, Color.BLACK);
        moneyLabel.setEffect(moneyShadow);
        getGameScene().addUINode(moneyLabel);
        
        int legendY = moneyY + 24;
        Text coinCaption = new Text("COINS");
        coinCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        coinCaption.setFill(Color.rgb(160, 160, 170));
        coinCaption.setTranslateX(xOffset);
        coinCaption.setTranslateY(legendY);
        getGameScene().addUINode(coinCaption);
        legendY += 14;
        int legendRowH = 18;
        Color[] legendColors = {
            Color.rgb(240, 240, 240),
            Color.rgb(100, 220, 100),
            Color.rgb(100, 150, 255),
            Color.rgb(200, 100, 255)
        };
        int[] legendValues = {10, 50, 100, 1000};
        for (int i = 0; i < 4; i++) {
            Circle dot = new Circle(4);
            dot.setFill(legendColors[i]);
            dot.setCenterX(xOffset + 4);
            dot.setCenterY(legendY + i * legendRowH);
            getGameScene().addUINode(dot);
            
            Text valueText = new Text("$" + legendValues[i]);
            valueText.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
            valueText.setFill(Color.GOLD);
            valueText.setTranslateX(xOffset + 12);
            valueText.setTranslateY(legendY + i * legendRowH + 3);
            DropShadow legendShadow = new DropShadow(1, Color.BLACK);
            valueText.setEffect(legendShadow);
            getGameScene().addUINode(valueText);
        }
        
        getip(GameVars.ACTIVE_BULLETS).addListener((obs, o, n) -> updateBars());
        getip(GameVars.AMMO_CAP).addListener((obs, o, n) -> updateBars());
        getip(GameVars.WEAPON_GRADE).addListener((obs, o, n) -> updateBars());
        getip(GameVars.LIVES).addListener((obs, o, n) -> updateBars());
        getbp(GameVars.AUTOFIRE).addListener((obs, o, n) -> updateAutoLamp());
        updateBars();
        updateAutoLamp();
        
        int playX = railWidth;
        int playW = getAppWidth() - 2 * railWidth;

        Rectangle hudStrip = new Rectangle(playW, 40);
        hudStrip.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(6, 12, 22, 0.82)),
                new Stop(1, Color.rgb(6, 12, 22, 0.35))));
        hudStrip.setTranslateX(playX);
        hudStrip.setTranslateY(4);
        getGameScene().addUINode(hudStrip);

        Rectangle hudEdge = new Rectangle(playW, 1);
        hudEdge.setFill(Color.rgb(60, 210, 255, 0.45));
        hudEdge.setTranslateX(playX);
        hudEdge.setTranslateY(43);
        getGameScene().addUINode(hudEdge);

        Text scoreCaption = new Text("SCORE");
        scoreCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        scoreCaption.setFill(Color.rgb(90, 210, 240));
        scoreCaption.setTranslateX(playX + 12);
        scoreCaption.setTranslateY(16);
        getGameScene().addUINode(scoreCaption);

        Text scoreLabel = new Text();
        scoreLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
        scoreLabel.setFill(Color.WHITE);
        scoreLabel.setTranslateX(playX + 12);
        scoreLabel.setTranslateY(36);
        scoreLabel.textProperty().bind(getip(GameVars.SCORE).asString("%06d"));
        DropShadow scoreGlow = new DropShadow(12, Color.CYAN);
        scoreGlow.setSpread(0.15);
        scoreLabel.setEffect(scoreGlow);
        getGameScene().addUINode(scoreLabel);

        int comboPanelW = 92;
        int comboX = playX + (playW - comboPanelW) / 2;

        comboPlate = new Rectangle(comboPanelW, 30);
        comboPlate.setArcWidth(6);
        comboPlate.setArcHeight(6);
        comboPlate.setFill(Color.rgb(10, 18, 32, 0.9));
        comboPlate.setStroke(Color.rgb(255, 160, 40, 0.45));
        comboPlate.setStrokeWidth(1.2);
        comboPlate.setTranslateX(comboX);
        comboPlate.setTranslateY(8);
        getGameScene().addUINode(comboPlate);

        Text comboCaption = new Text("COMBO");
        comboCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        comboCaption.setFill(Color.rgb(255, 170, 60));
        comboCaption.setTranslateX(comboX + 8);
        comboCaption.setTranslateY(19);
        getGameScene().addUINode(comboCaption);

        Text comboLabel = new Text();
        comboLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        comboLabel.setFill(Color.rgb(255, 200, 90));
        comboLabel.setTranslateX(comboX + 52);
        comboLabel.setTranslateY(31);
        comboLabel.textProperty().bind(getip(GameVars.SQUAD_COMBOS).asString("x%d"));
        DropShadow comboGlow = new DropShadow(10, Color.ORANGE);
        comboGlow.setSpread(0.2);
        comboLabel.setEffect(comboGlow);
        getGameScene().addUINode(comboLabel);

        int wavePanelW = 86;
        int waveX = playX + playW - wavePanelW - 8;

        Rectangle wavePlate = new Rectangle(wavePanelW, 30);
        wavePlate.setArcWidth(6);
        wavePlate.setArcHeight(6);
        wavePlate.setFill(Color.rgb(10, 18, 32, 0.9));
        wavePlate.setStroke(Color.rgb(255, 210, 70, 0.7));
        wavePlate.setStrokeWidth(1.2);
        wavePlate.setTranslateX(waveX);
        wavePlate.setTranslateY(8);
        getGameScene().addUINode(wavePlate);

        Text waveCaption = new Text("WAVE");
        waveCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        waveCaption.setFill(Color.rgb(255, 210, 70));
        waveCaption.setTranslateX(waveX + 8);
        waveCaption.setTranslateY(19);
        getGameScene().addUINode(waveCaption);

        Text levelLabel = new Text();
        levelLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        levelLabel.setFill(Color.rgb(255, 230, 120));
        levelLabel.setTranslateX(waveX + 44);
        levelLabel.setTranslateY(31);
        levelLabel.textProperty().bind(getip(GameVars.LEVEL).asString("%02d"));
        DropShadow waveGlow = new DropShadow(10, Color.GOLD);
        waveGlow.setSpread(0.2);
        levelLabel.setEffect(waveGlow);
        getGameScene().addUINode(levelLabel);
        
        Text livesLabel = new Text();
        livesLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        livesLabel.setFill(Color.LIMEGREEN);
        livesLabel.setTranslateX(xOffset);
        livesLabel.setTranslateY(getAppHeight() - 20);
        livesLabel.textProperty().bind(getip(GameVars.LIVES).asString("\u2665 %d"));
        
        DropShadow livesShadow = new DropShadow(3, Color.BLACK);
        livesLabel.setEffect(livesShadow);
        getGameScene().addUINode(livesLabel);
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
    
    private Group createAnimatedLetterSlot(char letter, int index, double x, double y) {
        Rectangle frame = new Rectangle(24, 24);
        frame.setFill(Color.rgb(25, 30, 40));
        frame.setStroke(Color.rgb(60, 70, 90));
        frame.setStrokeWidth(1);
        frame.setArcWidth(4);
        frame.setArcHeight(4);
        frame.setTranslateX(-12);
        frame.setTranslateY(-18);
        
        Text letterText = new Text(String.valueOf(letter));
        letterText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
        letterText.setFill(Color.rgb(45, 50, 60));
        letterText.setTranslateX(-6);
        letterText.setTranslateY(0);
        extraLetterTexts[index] = letterText;
        
        Group flipWrapper = new Group(letterText);
        extraFlipWrappers[index] = flipWrapper;
        
        Group letterGroup = new Group(frame, flipWrapper);
        letterGroup.setTranslateX(x);
        letterGroup.setTranslateY(y);
        
        startIdleFlipAnimation(index);
        
        return letterGroup;
    }
    
    private void startIdleFlipAnimation(int index) {
        Group flipWrapper = extraFlipWrappers[index];
        if (flipWrapper == null) return;
        
        double cycleDuration = 2.5 + index * 0.3;
        
        Timeline flipTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(flipWrapper.scaleXProperty(), 1.0)),
            new KeyFrame(Duration.seconds(cycleDuration * 0.25), 
                new KeyValue(flipWrapper.scaleXProperty(), 0.18)),
            new KeyFrame(Duration.seconds(cycleDuration * 0.5), 
                new KeyValue(flipWrapper.scaleXProperty(), -1.0)),
            new KeyFrame(Duration.seconds(cycleDuration * 0.75), 
                new KeyValue(flipWrapper.scaleXProperty(), -0.18)),
            new KeyFrame(Duration.seconds(cycleDuration), 
                new KeyValue(flipWrapper.scaleXProperty(), 1.0))
        );
        flipTimeline.setCycleCount(Animation.INDEFINITE);
        flipTimeline.setDelay(Duration.millis(index * 400));
        flipTimeline.play();
        extraLetterAnimations.add(flipTimeline);
    }
    
    private void updateExtraLetter(int idx, boolean lit, Color litColor) {
        Text letterText = extraLetterTexts[idx];
        Group letterGroup = extraLetterGroups[idx];
        
        if (lit) {
            letterText.setFill(litColor);
            
            Rectangle frame = (Rectangle) letterGroup.getChildren().get(0);
            frame.setStroke(litColor.darker());
            
        } else {
            letterText.setFill(Color.rgb(45, 50, 60));
            
            Rectangle frame = (Rectangle) letterGroup.getChildren().get(0);
            frame.setStroke(Color.rgb(60, 70, 90));
        }
    }
    
    private void updateBars() {
        int railWidth = GameVars.RAIL_WIDTH;
        int barWidth = railWidth - 28;
        
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
        updateExtraTimeBar();
    }

    private void updateExtraTimeBar() {
        if (extraTimeBar == null) {
            return;
        }
        double ratio = GameVars.EXTRA_TIME_MAX > 0 ? extraTime / GameVars.EXTRA_TIME_MAX : 0;
        extraTimeBar.setWidth(Math.max(1, extraTimeBarMaxWidth * ratio));
        if (extraTime > 0 && extraTime <= 5) {
            extraTimeBar.setOpacity(0.35 + 0.65 * (0.5 + 0.5 * Math.sin(extraTimePulse * 14)));
        } else {
            extraTimeBar.setOpacity(1);
        }
    }

    private void updateRankHud() {
        if (rankDots == null) {
            return;
        }
        int mask = 0;
        int rank = 0;
        try {
            mask = geti(GameVars.RANK_MASK);
            rank = geti(GameVars.RANK);
        } catch (Exception ignored) {
            return;
        }
        if (rankTitleText != null) {
            rankTitleText.setText(RankNames.shortTitle(rank));
        }
        for (int i = 0; i < rankDots.length; i++) {
            if (rankDots[i] == null) {
                continue;
            }
            styleRankDot(rankDots[i], i, (mask & (1 << i)) != 0);
        }
    }

    private void styleRankDot(Circle dot, int colorIndex, boolean collected) {
        Color color = RankMarkerComponent.COLORS[colorIndex];
        if (collected) {
            dot.setFill(color);
            dot.setStroke(Color.WHITE);
            DropShadow glow = new DropShadow(6, color);
            glow.setSpread(0.22);
            dot.setEffect(glow);
        } else {
            dot.setFill(Color.rgb(10, 10, 14, 0.85));
            dot.setStroke(color);
            dot.setEffect(null);
        }
        dot.setOpacity(1);
    }
    
    private void updateAutoLamp() {
        boolean autoOn = getb(GameVars.AUTOFIRE);
        if (autoLamp != null) {
            if (autoOn) {
                autoLamp.setFill(Color.CYAN);
                autoLamp.setStroke(Color.WHITE);
                DropShadow glow = new DropShadow(8, Color.CYAN);
                autoLamp.setEffect(glow);
            } else {
                autoLamp.setFill(Color.rgb(40, 40, 40));
                autoLamp.setStroke(Color.rgb(80, 80, 80));
                autoLamp.setEffect(null);
            }
        }
        if (autoLampText != null) {
            autoLampText.setFill(autoOn ? Color.CYAN : Color.rgb(60, 60, 60));
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
