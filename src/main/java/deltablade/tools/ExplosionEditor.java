package deltablade.tools;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Standalone explosion editor for DeltaBlade.
 * Creates procedural pixel-art explosions and exports them as sprite sheets.
 * 
 * Run with: mvn javafx:run -Pexplosions
 * 
 * Integration note (future PR): To play exported sheets in-game, load the PNG
 * using EmbeddedTextures.decodePng(), then draw frame N based on elapsed time:
 *   int frame = (int)((elapsedMs / 1000.0) * fps) % frameCount;
 *   gc.drawImage(sheet, frame * frameW, 0, frameW, frameH, x, y, frameW, frameH);
 */
public class ExplosionEditor extends Application {

    private static final int PREVIEW_SIZE = 384;
    private static final int RENDER_SCALE = 6;
    
    private Canvas previewCanvas;
    private GraphicsContext previewGc;
    private WritableImage renderBuffer;
    
    private Slider durationSlider;
    private Slider frameCountSlider;
    private ComboBox<Integer> frameSizeCombo;
    private Slider particleCountSlider;
    private Slider sizeOverLifeSlider;
    private ColorPicker coreColorPicker;
    private ColorPicker midColorPicker;
    private ColorPicker smokeColorPicker;
    private Slider gravitySlider;
    private Slider velocitySlider;
    private Slider dragSlider;
    private CheckBox fireToggle;
    private CheckBox sparksToggle;
    private CheckBox smokeToggle;
    private CheckBox shockwaveToggle;
    private TextField seedField;
    private Slider frameSlider;
    private Label currentFrameLabel;
    private Label playbackTimeLabel;
    
    private boolean isPlaying = true;
    private boolean isLooping = true;
    private double playbackTime = 0;
    private long lastNanos = 0;
    private AnimationTimer animationTimer;
    
    private final Random random = new Random();
    private long currentSeed = System.currentTimeMillis();
    
    private List<Particle> particles = new ArrayList<>();
    private double shockwaveRadius = 0;
    private double shockwaveMaxRadius = 0;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("DeltaBlade Explosion Editor");
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        
        VBox previewPane = createPreviewPane();
        root.setCenter(previewPane);
        
        ScrollPane controlPane = createControlPane();
        root.setRight(controlPane);
        
        HBox playbackPane = createPlaybackPane();
        root.setBottom(playbackPane);
        
        Scene scene = new Scene(root, 900, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        applyPreset("schiff");
        startAnimation();
    }
    
    private VBox createPreviewPane() {
        VBox pane = new VBox(10);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(20));
        
        Label title = new Label("Vorschau");
        title.setStyle("-fx-text-fill: #eee; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        previewCanvas = new Canvas(PREVIEW_SIZE, PREVIEW_SIZE);
        previewGc = previewCanvas.getGraphicsContext2D();
        previewGc.setImageSmoothing(false);
        
        StackPane canvasHolder = new StackPane(previewCanvas);
        canvasHolder.setStyle("-fx-background-color: #000; -fx-border-color: #444; -fx-border-width: 2;");
        canvasHolder.setMaxSize(PREVIEW_SIZE + 4, PREVIEW_SIZE + 4);
        
        pane.getChildren().addAll(title, canvasHolder);
        return pane;
    }
    
    private ScrollPane createControlPane() {
        VBox controls = new VBox(12);
        controls.setPadding(new Insets(15));
        controls.setStyle("-fx-background-color: #16213e;");
        controls.setPrefWidth(320);
        
        Label header = new Label("Parameter");
        header.setStyle("-fx-text-fill: #fff; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        durationSlider = createLabeledSlider("Dauer (s)", 0.2, 2.0, 0.6, controls);
        frameCountSlider = createLabeledSlider("Frame-Anzahl", 4, 24, 8, controls);
        frameCountSlider.setBlockIncrement(1);
        frameCountSlider.setMajorTickUnit(4);
        frameCountSlider.setSnapToTicks(true);
        
        HBox frameSizeBox = new HBox(10);
        frameSizeBox.setAlignment(Pos.CENTER_LEFT);
        Label frameSizeLabel = new Label("Frame-Größe:");
        frameSizeLabel.setStyle("-fx-text-fill: #ccc;");
        frameSizeCombo = new ComboBox<>();
        frameSizeCombo.getItems().addAll(32, 48, 64, 96);
        frameSizeCombo.setValue(64);
        frameSizeCombo.setStyle("-fx-background-color: #2a2a4a;");
        frameSizeBox.getChildren().addAll(frameSizeLabel, frameSizeCombo);
        
        particleCountSlider = createLabeledSlider("Partikelzahl", 20, 300, 80, controls);
        sizeOverLifeSlider = createLabeledSlider("Größe über Lebenszeit", 0.2, 2.0, 1.0, controls);
        
        Label colorHeader = new Label("Farbverlauf");
        colorHeader.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        HBox coreBox = createColorPickerRow("Kern:", Color.WHITE, c -> coreColorPicker = c);
        HBox midBox = createColorPickerRow("Mitte:", Color.ORANGE, c -> midColorPicker = c);
        HBox smokeBox = createColorPickerRow("Rauch:", Color.GRAY, c -> smokeColorPicker = c);
        
        gravitySlider = createLabeledSlider("Gravity", -200, 200, 50, controls);
        velocitySlider = createLabeledSlider("Outward-Velocity", 20, 400, 150, controls);
        dragSlider = createLabeledSlider("Drag", 0.5, 5.0, 2.0, controls);
        
        Label layerHeader = new Label("Layer-Toggles");
        layerHeader.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        fireToggle = createToggle("Feuer", true);
        sparksToggle = createToggle("Funken", true);
        smokeToggle = createToggle("Rauch", true);
        shockwaveToggle = createToggle("Schockwelle", false);
        
        HBox toggleBox = new HBox(8, fireToggle, sparksToggle, smokeToggle, shockwaveToggle);
        toggleBox.setAlignment(Pos.CENTER_LEFT);
        
        HBox seedBox = new HBox(10);
        seedBox.setAlignment(Pos.CENTER_LEFT);
        Label seedLabel = new Label("Seed:");
        seedLabel.setStyle("-fx-text-fill: #ccc;");
        seedField = new TextField(String.valueOf(currentSeed));
        seedField.setPrefWidth(120);
        seedField.setStyle("-fx-background-color: #2a2a4a; -fx-text-fill: #fff;");
        Button randomizeBtn = new Button("Randomize");
        randomizeBtn.setStyle("-fx-background-color: #4a4a8a; -fx-text-fill: #fff;");
        randomizeBtn.setOnAction(e -> randomizeSeed());
        seedBox.getChildren().addAll(seedLabel, seedField, randomizeBtn);
        
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #444;");
        
        Label presetHeader = new Label("Presets");
        presetHeader.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        HBox presetBox1 = new HBox(8);
        HBox presetBox2 = new HBox(8);
        presetBox1.setAlignment(Pos.CENTER);
        presetBox2.setAlignment(Pos.CENTER);
        
        presetBox1.getChildren().addAll(
            createPresetButton("Treffer", "treffer"),
            createPresetButton("Schiff", "schiff"),
            createPresetButton("Tough", "tough")
        );
        presetBox2.getChildren().addAll(
            createPresetButton("Boss", "boss"),
            createPresetButton("Plasma", "plasma"),
            createPresetButton("Funken", "funken")
        );
        
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: #444;");
        
        Label exportHeader = new Label("Export");
        exportHeader.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        Button exportSheetBtn = new Button("Sprite Sheet exportieren...");
        exportSheetBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: #fff; -fx-font-weight: bold;");
        exportSheetBtn.setMaxWidth(Double.MAX_VALUE);
        exportSheetBtn.setOnAction(e -> exportSpriteSheet());
        
        Button exportFramesBtn = new Button("Einzelframes exportieren...");
        exportFramesBtn.setStyle("-fx-background-color: #1565c0; -fx-text-fill: #fff;");
        exportFramesBtn.setMaxWidth(Double.MAX_VALUE);
        exportFramesBtn.setOnAction(e -> exportIndividualFrames());
        
        controls.getChildren().addAll(
            header,
            durationSlider.getParent().getParent(),
            frameCountSlider.getParent().getParent(),
            frameSizeBox,
            particleCountSlider.getParent().getParent(),
            sizeOverLifeSlider.getParent().getParent(),
            colorHeader, coreBox, midBox, smokeBox,
            gravitySlider.getParent().getParent(),
            velocitySlider.getParent().getParent(),
            dragSlider.getParent().getParent(),
            layerHeader, toggleBox,
            seedBox,
            sep1,
            presetHeader, presetBox1, presetBox2,
            sep2,
            exportHeader, exportSheetBtn, exportFramesBtn
        );
        
        ScrollPane scroll = new ScrollPane(controls);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #16213e; -fx-background-color: #16213e;");
        return scroll;
    }
    
    private Slider createLabeledSlider(String labelText, double min, double max, double value, VBox parent) {
        VBox box = new VBox(2);
        HBox labelRow = new HBox();
        Label label = new Label(labelText + ":");
        label.setStyle("-fx-text-fill: #ccc;");
        Label valueLabel = new Label(String.format("%.2f", value));
        valueLabel.setStyle("-fx-text-fill: #8af;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        labelRow.getChildren().addAll(label, spacer, valueLabel);
        
        Slider slider = new Slider(min, max, value);
        slider.setStyle("-fx-control-inner-background: #2a2a4a;");
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.format("%.2f", newVal.doubleValue()));
            restartExplosion();
        });
        
        box.getChildren().addAll(labelRow, slider);
        return slider;
    }
    
    private HBox createColorPickerRow(String labelText, Color defaultColor, java.util.function.Consumer<ColorPicker> setter) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #ccc;");
        label.setMinWidth(50);
        ColorPicker picker = new ColorPicker(defaultColor);
        picker.setStyle("-fx-background-color: #2a2a4a;");
        picker.setOnAction(e -> restartExplosion());
        setter.accept(picker);
        box.getChildren().addAll(label, picker);
        return box;
    }
    
    private CheckBox createToggle(String text, boolean selected) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.setStyle("-fx-text-fill: #ccc;");
        cb.setOnAction(e -> restartExplosion());
        return cb;
    }
    
    private Button createPresetButton(String text, String presetId) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #3a3a6a; -fx-text-fill: #fff;");
        btn.setOnAction(e -> applyPreset(presetId));
        return btn;
    }
    
    private HBox createPlaybackPane() {
        HBox pane = new HBox(15);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(10, 20, 15, 20));
        pane.setStyle("-fx-background-color: #0f0f23;");
        
        Button playPauseBtn = new Button("▶");
        playPauseBtn.setStyle("-fx-background-color: #4a4a8a; -fx-text-fill: #fff; -fx-font-size: 14px;");
        playPauseBtn.setOnAction(e -> {
            isPlaying = !isPlaying;
            playPauseBtn.setText(isPlaying ? "⏸" : "▶");
            if (isPlaying) lastNanos = System.nanoTime();
        });
        
        Button restartBtn = new Button("⟲");
        restartBtn.setStyle("-fx-background-color: #4a4a8a; -fx-text-fill: #fff; -fx-font-size: 14px;");
        restartBtn.setOnAction(e -> restartExplosion());
        
        CheckBox loopToggle = new CheckBox("Loop");
        loopToggle.setSelected(true);
        loopToggle.setStyle("-fx-text-fill: #ccc;");
        loopToggle.setOnAction(e -> isLooping = loopToggle.isSelected());
        
        frameSlider = new Slider(0, 1, 0);
        frameSlider.setPrefWidth(200);
        frameSlider.setStyle("-fx-control-inner-background: #2a2a4a;");
        frameSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPlaying) {
                double duration = durationSlider.getValue();
                playbackTime = newVal.doubleValue() * duration;
            }
        });
        
        currentFrameLabel = new Label("Frame: 1/8");
        currentFrameLabel.setStyle("-fx-text-fill: #8af;");
        currentFrameLabel.setMinWidth(80);
        
        playbackTimeLabel = new Label("0.00s");
        playbackTimeLabel.setStyle("-fx-text-fill: #ccc;");
        playbackTimeLabel.setMinWidth(50);
        
        pane.getChildren().addAll(playPauseBtn, restartBtn, loopToggle, frameSlider, currentFrameLabel, playbackTimeLabel);
        return pane;
    }
    
    private void applyPreset(String presetId) {
        switch (presetId) {
            case "treffer" -> {
                durationSlider.setValue(0.25);
                frameCountSlider.setValue(6);
                frameSizeCombo.setValue(32);
                particleCountSlider.setValue(30);
                sizeOverLifeSlider.setValue(0.5);
                coreColorPicker.setValue(Color.WHITE);
                midColorPicker.setValue(Color.YELLOW);
                smokeColorPicker.setValue(Color.rgb(80, 80, 80));
                gravitySlider.setValue(0);
                velocitySlider.setValue(100);
                dragSlider.setValue(3.0);
                fireToggle.setSelected(false);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(false);
                shockwaveToggle.setSelected(false);
            }
            case "schiff" -> {
                durationSlider.setValue(0.6);
                frameCountSlider.setValue(8);
                frameSizeCombo.setValue(64);
                particleCountSlider.setValue(80);
                sizeOverLifeSlider.setValue(1.0);
                coreColorPicker.setValue(Color.WHITE);
                midColorPicker.setValue(Color.ORANGE);
                smokeColorPicker.setValue(Color.GRAY);
                gravitySlider.setValue(50);
                velocitySlider.setValue(150);
                dragSlider.setValue(2.0);
                fireToggle.setSelected(true);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(true);
                shockwaveToggle.setSelected(false);
            }
            case "tough" -> {
                durationSlider.setValue(0.8);
                frameCountSlider.setValue(10);
                frameSizeCombo.setValue(64);
                particleCountSlider.setValue(120);
                sizeOverLifeSlider.setValue(1.2);
                coreColorPicker.setValue(Color.LIGHTYELLOW);
                midColorPicker.setValue(Color.DARKORANGE);
                smokeColorPicker.setValue(Color.rgb(60, 50, 40));
                gravitySlider.setValue(30);
                velocitySlider.setValue(180);
                dragSlider.setValue(1.8);
                fireToggle.setSelected(true);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(true);
                shockwaveToggle.setSelected(true);
            }
            case "boss" -> {
                durationSlider.setValue(1.2);
                frameCountSlider.setValue(16);
                frameSizeCombo.setValue(96);
                particleCountSlider.setValue(200);
                sizeOverLifeSlider.setValue(1.5);
                coreColorPicker.setValue(Color.WHITE);
                midColorPicker.setValue(Color.ORANGERED);
                smokeColorPicker.setValue(Color.rgb(40, 35, 30));
                gravitySlider.setValue(40);
                velocitySlider.setValue(220);
                dragSlider.setValue(1.5);
                fireToggle.setSelected(true);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(true);
                shockwaveToggle.setSelected(true);
            }
            case "plasma" -> {
                durationSlider.setValue(0.5);
                frameCountSlider.setValue(8);
                frameSizeCombo.setValue(64);
                particleCountSlider.setValue(100);
                sizeOverLifeSlider.setValue(0.8);
                coreColorPicker.setValue(Color.WHITE);
                midColorPicker.setValue(Color.CYAN);
                smokeColorPicker.setValue(Color.rgb(80, 40, 120));
                gravitySlider.setValue(-20);
                velocitySlider.setValue(160);
                dragSlider.setValue(2.5);
                fireToggle.setSelected(true);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(false);
                shockwaveToggle.setSelected(true);
            }
            case "funken" -> {
                durationSlider.setValue(0.4);
                frameCountSlider.setValue(8);
                frameSizeCombo.setValue(48);
                particleCountSlider.setValue(50);
                sizeOverLifeSlider.setValue(0.3);
                coreColorPicker.setValue(Color.LIGHTYELLOW);
                midColorPicker.setValue(Color.ORANGE);
                smokeColorPicker.setValue(Color.DARKGRAY);
                gravitySlider.setValue(150);
                velocitySlider.setValue(200);
                dragSlider.setValue(1.5);
                fireToggle.setSelected(false);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(false);
                shockwaveToggle.setSelected(false);
            }
        }
        restartExplosion();
    }
    
    private void randomizeSeed() {
        currentSeed = System.currentTimeMillis();
        seedField.setText(String.valueOf(currentSeed));
        restartExplosion();
    }
    
    private void restartExplosion() {
        playbackTime = 0;
        try {
            currentSeed = Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException e) {
            currentSeed = System.currentTimeMillis();
            seedField.setText(String.valueOf(currentSeed));
        }
        initializeParticles();
    }
    
    private void initializeParticles() {
        particles.clear();
        random.setSeed(currentSeed);
        
        int frameSize = frameSizeCombo.getValue();
        double centerX = frameSize / 2.0;
        double centerY = frameSize / 2.0;
        double velocity = velocitySlider.getValue();
        
        int count = (int) particleCountSlider.getValue();
        
        if (fireToggle.isSelected()) {
            int fireCount = count / 3;
            for (int i = 0; i < fireCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = velocity * (0.3 + random.nextDouble() * 0.7);
                double size = 3 + random.nextDouble() * 4;
                particles.add(new Particle(
                    centerX, centerY,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.FIRE,
                    0.8 + random.nextDouble() * 0.2
                ));
            }
        }
        
        if (sparksToggle.isSelected()) {
            int sparkCount = count / 4;
            for (int i = 0; i < sparkCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = velocity * (0.8 + random.nextDouble() * 0.8);
                particles.add(new Particle(
                    centerX, centerY,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    1 + random.nextDouble() * 2, ParticleType.SPARK,
                    0.6 + random.nextDouble() * 0.4
                ));
            }
        }
        
        if (smokeToggle.isSelected()) {
            int smokeCount = count / 3;
            for (int i = 0; i < smokeCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = velocity * (0.2 + random.nextDouble() * 0.4);
                double size = 4 + random.nextDouble() * 6;
                particles.add(new Particle(
                    centerX + (random.nextDouble() - 0.5) * 4,
                    centerY + (random.nextDouble() - 0.5) * 4,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.SMOKE,
                    0.5 + random.nextDouble() * 0.5
                ));
            }
        }
        
        int coreCount = count / 10 + 3;
        for (int i = 0; i < coreCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = velocity * (0.1 + random.nextDouble() * 0.3);
            double size = 2 + random.nextDouble() * 3;
            particles.add(new Particle(
                centerX + (random.nextDouble() - 0.5) * 2,
                centerY + (random.nextDouble() - 0.5) * 2,
                Math.cos(angle) * speed, Math.sin(angle) * speed,
                size, ParticleType.CORE,
                0.3 + random.nextDouble() * 0.3
            ));
        }
        
        if (shockwaveToggle.isSelected()) {
            shockwaveRadius = 0;
            shockwaveMaxRadius = frameSize * 0.45;
        }
    }
    
    private void startAnimation() {
        lastNanos = System.nanoTime();
        
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                
                if (isPlaying) {
                    double duration = durationSlider.getValue();
                    playbackTime += deltaTime;
                    
                    if (playbackTime >= duration) {
                        if (isLooping) {
                            restartExplosion();
                        } else {
                            playbackTime = duration;
                            isPlaying = false;
                        }
                    }
                    
                    frameSlider.setValue(playbackTime / duration);
                }
                
                updateParticles(deltaTime);
                renderPreview();
                updateLabels();
            }
        };
        animationTimer.start();
        initializeParticles();
    }
    
    private void updateParticles(double deltaTime) {
        if (!isPlaying) return;
        
        double gravity = gravitySlider.getValue();
        double drag = dragSlider.getValue();
        double duration = durationSlider.getValue();
        double progress = playbackTime / duration;
        
        for (Particle p : particles) {
            p.vy += gravity * deltaTime;
            p.vx *= (1.0 - drag * deltaTime);
            p.vy *= (1.0 - drag * deltaTime);
            p.x += p.vx * deltaTime;
            p.y += p.vy * deltaTime;
            p.life = Math.max(0, 1.0 - (progress / p.lifeFactor));
        }
        
        if (shockwaveToggle.isSelected() && progress < 0.8) {
            shockwaveRadius = shockwaveMaxRadius * (progress / 0.8);
        }
    }
    
    private void renderPreview() {
        int frameSize = frameSizeCombo.getValue();
        
        if (renderBuffer == null || renderBuffer.getWidth() != frameSize) {
            renderBuffer = new WritableImage(frameSize, frameSize);
        }
        
        renderFrameToImage(renderBuffer, playbackTime);
        
        previewGc.setFill(Color.BLACK);
        previewGc.fillRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
        
        drawStarfield();
        
        previewGc.setImageSmoothing(false);
        double scale = (double) PREVIEW_SIZE / frameSize;
        previewGc.drawImage(renderBuffer, 0, 0, frameSize, frameSize, 0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
    }
    
    private void drawStarfield() {
        random.setSeed(42);
        previewGc.setFill(Color.WHITE);
        for (int i = 0; i < 30; i++) {
            double x = random.nextDouble() * PREVIEW_SIZE;
            double y = random.nextDouble() * PREVIEW_SIZE;
            double size = 1 + random.nextDouble();
            double opacity = 0.3 + random.nextDouble() * 0.5;
            previewGc.setGlobalAlpha(opacity);
            previewGc.fillRect(x, y, size, size);
        }
        previewGc.setGlobalAlpha(1.0);
        random.setSeed(currentSeed);
    }
    
    private void renderFrameToImage(WritableImage img, double time) {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, 0x00000000);
            }
        }
        
        double duration = durationSlider.getValue();
        double progress = Math.min(1.0, time / duration);
        double sizeMultiplier = sizeOverLifeSlider.getValue();
        
        Color coreColor = coreColorPicker.getValue();
        Color midColor = midColorPicker.getValue();
        Color smokeColor = smokeColorPicker.getValue();
        
        double centerX = w / 2.0;
        double centerY = h / 2.0;
        
        if (shockwaveToggle.isSelected() && progress < 0.8) {
            double radius = shockwaveMaxRadius * (progress / 0.8);
            double ringAlpha = Math.max(0, 1.0 - progress / 0.8) * 0.6;
            drawRing(pw, w, h, centerX, centerY, radius, midColor, ringAlpha);
        }
        
        List<Particle> sortedParticles = new ArrayList<>(particles);
        sortedParticles.sort((a, b) -> {
            int ao = a.type == ParticleType.SMOKE ? 0 : (a.type == ParticleType.FIRE ? 1 : (a.type == ParticleType.SPARK ? 2 : 3));
            int bo = b.type == ParticleType.SMOKE ? 0 : (b.type == ParticleType.FIRE ? 1 : (b.type == ParticleType.SPARK ? 2 : 3));
            return Integer.compare(ao, bo);
        });
        
        for (Particle p : sortedParticles) {
            if (p.life <= 0) continue;
            
            double px = p.x;
            double py = p.y;
            double size = p.baseSize * (0.5 + 0.5 * p.life) * sizeMultiplier;
            double alpha = p.life;
            
            Color color;
            switch (p.type) {
                case CORE -> {
                    color = coreColor;
                    alpha *= 0.9;
                }
                case FIRE -> {
                    double blend = p.life;
                    color = interpolateColor(midColor, coreColor, blend * 0.5);
                    alpha *= 0.8;
                }
                case SPARK -> {
                    color = interpolateColor(midColor, coreColor, p.life);
                    alpha *= p.life;
                }
                case SMOKE -> {
                    color = smokeColor;
                    alpha *= 0.4 * p.life;
                    size *= 1.5;
                }
                default -> color = Color.WHITE;
            }
            
            drawPixelBlob(pw, w, h, px, py, size, color, alpha, p.type == ParticleType.SPARK);
        }
    }
    
    private void drawPixelBlob(PixelWriter pw, int w, int h, double cx, double cy, double size, Color color, double alpha, boolean isSpark) {
        int radius = (int) Math.ceil(size / 2);
        int icx = (int) cx;
        int icy = (int) cy;
        
        if (isSpark) {
            if (icx >= 0 && icx < w && icy >= 0 && icy < h) {
                blendPixel(pw, icx, icy, color, alpha);
            }
            return;
        }
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = icx + dx;
                int py = icy + dy;
                if (px < 0 || px >= w || py < 0 || py >= h) continue;
                
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist <= size / 2) {
                    double falloff = 1.0 - (dist / (size / 2));
                    blendPixel(pw, px, py, color, alpha * falloff);
                }
            }
        }
    }
    
    private void drawRing(PixelWriter pw, int w, int h, double cx, double cy, double radius, Color color, double alpha) {
        double thickness = 2.0;
        int iRadius = (int) Math.ceil(radius + thickness);
        
        for (int dy = -iRadius; dy <= iRadius; dy++) {
            for (int dx = -iRadius; dx <= iRadius; dx++) {
                int px = (int) cx + dx;
                int py = (int) cy + dy;
                if (px < 0 || px >= w || py < 0 || py >= h) continue;
                
                double dist = Math.sqrt(dx * dx + dy * dy);
                double ringDist = Math.abs(dist - radius);
                if (ringDist < thickness) {
                    double falloff = 1.0 - (ringDist / thickness);
                    blendPixel(pw, px, py, color, alpha * falloff);
                }
            }
        }
    }
    
    private void blendPixel(PixelWriter pw, int x, int y, Color color, double alpha) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        int a = (int) Math.min(255, alpha * 255);
        
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        pw.setArgb(x, y, argb);
    }
    
    private Color interpolateColor(Color c1, Color c2, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            c1.getRed() + (c2.getRed() - c1.getRed()) * t,
            c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t,
            c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t,
            c1.getOpacity() + (c2.getOpacity() - c1.getOpacity()) * t
        );
    }
    
    private void updateLabels() {
        int frameCount = (int) frameCountSlider.getValue();
        double duration = durationSlider.getValue();
        int currentFrame = Math.min(frameCount, (int) (playbackTime / duration * frameCount) + 1);
        currentFrameLabel.setText("Frame: " + currentFrame + "/" + frameCount);
        playbackTimeLabel.setText(String.format("%.2fs", playbackTime));
    }
    
    private void exportSpriteSheet() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sprite Sheet speichern");
        fileChooser.setInitialFileName("explosion_sheet.png");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        
        File file = fileChooser.showSaveDialog(previewCanvas.getScene().getWindow());
        if (file == null) return;
        
        try {
            int frameSize = frameSizeCombo.getValue();
            int frameCount = (int) frameCountSlider.getValue();
            double duration = durationSlider.getValue();
            double fps = frameCount / duration;
            
            int cols = Math.min(8, frameCount);
            int rows = (frameCount + cols - 1) / cols;
            
            WritableImage sheet = new WritableImage(cols * frameSize, rows * frameSize);
            WritableImage frameBuffer = new WritableImage(frameSize, frameSize);
            PixelWriter sheetPw = sheet.getPixelWriter();
            PixelReader framePr = frameBuffer.getPixelReader();
            
            initializeParticles();
            
            for (int i = 0; i < frameCount; i++) {
                double frameTime = (i / (double) frameCount) * duration;
                
                simulateToTime(frameTime);
                renderFrameToImage(frameBuffer, frameTime);
                
                int col = i % cols;
                int row = i / cols;
                int destX = col * frameSize;
                int destY = row * frameSize;
                
                for (int y = 0; y < frameSize; y++) {
                    for (int x = 0; x < frameSize; x++) {
                        sheetPw.setArgb(destX + x, destY + y, framePr.getArgb(x, y));
                    }
                }
            }
            
            byte[] pngBytes = encodePng8BitRgba(sheet);
            Files.write(file.toPath(), pngBytes);
            
            String jsonPath = file.getAbsolutePath().replace(".png", ".json");
            String json = String.format(
                "{\n  \"frameWidth\": %d,\n  \"frameHeight\": %d,\n  \"frameCount\": %d,\n  \"fps\": %.2f,\n  \"columns\": %d,\n  \"rows\": %d\n}",
                frameSize, frameSize, frameCount, fps, cols, rows
            );
            Files.writeString(Path.of(jsonPath), json);
            
            restartExplosion();
            
            showAlert(Alert.AlertType.INFORMATION, "Export erfolgreich",
                "Sprite Sheet gespeichert:\n" + file.getName() + "\n" + 
                "Metadaten: " + Path.of(jsonPath).getFileName());
                
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export fehlgeschlagen", e.getMessage());
        }
    }
    
    private void exportIndividualFrames() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Ordner für Einzelframes wählen");
        
        File dir = dirChooser.showDialog(previewCanvas.getScene().getWindow());
        if (dir == null) return;
        
        try {
            int frameSize = frameSizeCombo.getValue();
            int frameCount = (int) frameCountSlider.getValue();
            double duration = durationSlider.getValue();
            double fps = frameCount / duration;
            
            WritableImage frameBuffer = new WritableImage(frameSize, frameSize);
            
            initializeParticles();
            
            for (int i = 0; i < frameCount; i++) {
                double frameTime = (i / (double) frameCount) * duration;
                
                simulateToTime(frameTime);
                renderFrameToImage(frameBuffer, frameTime);
                
                String filename = String.format("explosion_%03d.png", i);
                byte[] pngBytes = encodePng8BitRgba(frameBuffer);
                Files.write(Path.of(dir.getAbsolutePath(), filename), pngBytes);
            }
            
            String jsonPath = Path.of(dir.getAbsolutePath(), "explosion_meta.json").toString();
            String json = String.format(
                "{\n  \"frameWidth\": %d,\n  \"frameHeight\": %d,\n  \"frameCount\": %d,\n  \"fps\": %.2f\n}",
                frameSize, frameSize, frameCount, fps
            );
            Files.writeString(Path.of(jsonPath), json);
            
            restartExplosion();
            
            showAlert(Alert.AlertType.INFORMATION, "Export erfolgreich",
                frameCount + " Frames exportiert nach:\n" + dir.getAbsolutePath());
                
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export fehlgeschlagen", e.getMessage());
        }
    }
    
    private void simulateToTime(double targetTime) {
        double duration = durationSlider.getValue();
        double progress = Math.min(1.0, targetTime / duration);
        double gravity = gravitySlider.getValue();
        double drag = dragSlider.getValue();
        
        initializeParticles();
        
        double simTime = 0;
        double dt = 1.0 / 120.0;
        
        while (simTime < targetTime) {
            double step = Math.min(dt, targetTime - simTime);
            
            for (Particle p : particles) {
                p.vy += gravity * step;
                p.vx *= (1.0 - drag * step);
                p.vy *= (1.0 - drag * step);
                p.x += p.vx * step;
                p.y += p.vy * step;
            }
            
            simTime += step;
        }
        
        double finalProgress = Math.min(1.0, targetTime / duration);
        for (Particle p : particles) {
            p.life = Math.max(0, 1.0 - (finalProgress / p.lifeFactor));
        }
        
        if (shockwaveToggle.isSelected()) {
            shockwaveRadius = shockwaveMaxRadius * Math.min(1.0, finalProgress / 0.8);
        }
    }
    
    /**
     * Encode a WritableImage as a PNG byte array.
     * Format: 8-bit RGBA (color type 6), non-interlaced.
     * Compatible with DeltaBlade's EmbeddedTextures.decodePng().
     */
    private byte[] encodePng8BitRgba(WritableImage img) throws IOException {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        PixelReader pr = img.getPixelReader();
        
        byte[] rawData = new byte[h * (1 + w * 4)];
        int idx = 0;
        
        for (int y = 0; y < h; y++) {
            rawData[idx++] = 0;
            for (int x = 0; x < w; x++) {
                int argb = pr.getArgb(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                rawData[idx++] = (byte) r;
                rawData[idx++] = (byte) g;
                rawData[idx++] = (byte) b;
                rawData[idx++] = (byte) a;
            }
        }
        
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(rawData);
        deflater.finish();
        
        byte[] compressedBuffer = new byte[rawData.length + 1024];
        int compressedLen = deflater.deflate(compressedBuffer);
        deflater.end();
        
        byte[] compressed = new byte[compressedLen];
        System.arraycopy(compressedBuffer, 0, compressed, 0, compressedLen);
        
        verifyInflate(compressed, w, h);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        out.write(new byte[] {(byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        writeInt(ihdr, w);
        writeInt(ihdr, h);
        ihdr.write(8);
        ihdr.write(6);
        ihdr.write(0);
        ihdr.write(0);
        ihdr.write(0);
        writeChunk(out, "IHDR", ihdr.toByteArray());
        
        writeChunk(out, "IDAT", compressed);
        
        writeChunk(out, "IEND", new byte[0]);
        
        return out.toByteArray();
    }
    
    private void verifyInflate(byte[] compressed, int w, int h) throws IOException {
        try {
            java.util.zip.Inflater inflater = new java.util.zip.Inflater();
            inflater.setInput(compressed);
            byte[] output = new byte[h * (1 + w * 4) + 1024];
            int total = 0;
            while (!inflater.finished()) {
                int count = inflater.inflate(output, total, output.length - total);
                if (count == 0 && inflater.needsInput()) break;
                total += count;
            }
            inflater.end();
            
            int expected = h * (1 + w * 4);
            if (total < expected) {
                throw new IOException("Inflate verification failed: got " + total + " bytes, expected " + expected);
            }
        } catch (java.util.zip.DataFormatException e) {
            throw new IOException("Inflate verification failed: " + e.getMessage());
        }
    }
    
    private void writeChunk(OutputStream out, String type, byte[] data) throws IOException {
        writeInt(out, data.length);
        
        byte[] typeBytes = type.getBytes("ASCII");
        out.write(typeBytes);
        
        out.write(data);
        
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt(out, (int) crc.getValue());
    }
    
    private void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    private enum ParticleType {
        CORE, FIRE, SPARK, SMOKE
    }
    
    private static class Particle {
        double x, y;
        double vx, vy;
        double baseSize;
        double life;
        double lifeFactor;
        ParticleType type;
        
        Particle(double x, double y, double vx, double vy, double size, ParticleType type, double lifeFactor) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.baseSize = size;
            this.type = type;
            this.lifeFactor = lifeFactor;
            this.life = 1.0;
        }
    }
}
