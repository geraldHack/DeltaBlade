package deltablade.tools;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

    private static final int PREVIEW_SIZE = 400;
    private static final int HIRES_SCALE = 4;
    
    private Canvas previewCanvas;
    private GraphicsContext previewGc;
    private WritableImage hiresBuffer;
    private WritableImage renderBuffer;
    private int[][] accumulatorR;
    private int[][] accumulatorG;
    private int[][] accumulatorB;
    private int[][] accumulatorA;
    
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
    private Slider flashIntensitySlider;
    private Slider debrisSlider;
    private CheckBox fireToggle;
    private CheckBox sparksToggle;
    private CheckBox smokeToggle;
    private CheckBox shockwaveToggle;
    private CheckBox debrisToggle;
    private CheckBox flashToggle;
    private TextField seedField;
    private Slider frameSlider;
    private Label currentFrameLabel;
    private Label playbackTimeLabel;
    private Label presetNameLabel;
    
    private boolean isPlaying = true;
    private boolean isLooping = true;
    private double playbackTime = 0;
    private long lastNanos = 0;
    private AnimationTimer animationTimer;
    
    private final Random random = new Random();
    private long currentSeed = 12345L;
    private String currentPresetName = "Schiff";
    
    private List<Particle> particles = new ArrayList<>();
    private double shockwaveRadius = 0;
    private double shockwaveMaxRadius = 0;
    private double flashAlpha = 0;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("DeltaBlade Explosion Editor v1.0");
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0d1117, #161b22);");
        
        VBox leftPane = createPreviewPane();
        root.setLeft(leftPane);
        
        ScrollPane controlPane = createControlPane();
        root.setCenter(controlPane);
        
        VBox bottomPane = createBottomPane();
        root.setBottom(bottomPane);
        
        Scene scene = new Scene(root, 1000, 720);
        scene.getStylesheets().add("data:text/css," + getCustomCSS());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
        
        applyPreset("schiff");
        startAnimation();
    }
    
    private String getCustomCSS() {
        return """
            .slider .track { -fx-background-color: #21262d; -fx-background-radius: 4; }
            .slider .thumb { -fx-background-color: #58a6ff; -fx-background-radius: 8; }
            .check-box .box { -fx-background-color: #21262d; -fx-border-color: #30363d; }
            .check-box:selected .mark { -fx-background-color: #58a6ff; }
            .combo-box { -fx-background-color: #21262d; -fx-border-color: #30363d; }
            .combo-box .list-cell { -fx-text-fill: #c9d1d9; -fx-background-color: #21262d; }
            .combo-box-popup .list-view { -fx-background-color: #21262d; }
            .combo-box-popup .list-cell:hover { -fx-background-color: #30363d; }
            .scroll-pane { -fx-background: #0d1117; -fx-background-color: #0d1117; }
            .scroll-pane .viewport { -fx-background-color: #0d1117; }
            .scroll-bar { -fx-background-color: #161b22; }
            .scroll-bar .thumb { -fx-background-color: #30363d; -fx-background-radius: 4; }
            .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-background-color: transparent; }
            .color-picker { -fx-background-color: #21262d; }
            """.replace("\n", " ");
    }
    
    private VBox createPreviewPane() {
        VBox pane = new VBox(15);
        pane.setAlignment(Pos.TOP_CENTER);
        pane.setPadding(new Insets(20, 15, 20, 20));
        pane.setStyle("-fx-background-color: transparent;");
        pane.setPrefWidth(450);
        
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("VORSCHAU");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#8b949e"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        presetNameLabel = new Label("Schiff");
        presetNameLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        presetNameLabel.setTextFill(Color.web("#58a6ff"));
        
        headerBox.getChildren().addAll(title, spacer, presetNameLabel);
        
        previewCanvas = new Canvas(PREVIEW_SIZE, PREVIEW_SIZE);
        previewGc = previewCanvas.getGraphicsContext2D();
        previewGc.setImageSmoothing(false);
        
        StackPane canvasHolder = new StackPane(previewCanvas);
        canvasHolder.setStyle(
            "-fx-background-color: #010409;" +
            "-fx-border-color: #30363d;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );
        canvasHolder.setMaxSize(PREVIEW_SIZE + 4, PREVIEW_SIZE + 4);
        
        DropShadow shadow = new DropShadow(15, Color.rgb(0, 0, 0, 0.5));
        canvasHolder.setEffect(shadow);
        
        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER);
        
        Label sizeInfo = new Label();
        sizeInfo.setFont(Font.font("Monospace", 11));
        sizeInfo.setTextFill(Color.web("#8b949e"));
        frameSizeCombo = new ComboBox<>();
        frameSizeCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) sizeInfo.setText(n + "×" + n + " px");
        });
        
        infoBox.getChildren().add(sizeInfo);
        
        pane.getChildren().addAll(headerBox, canvasHolder, infoBox);
        return pane;
    }
    
    private ScrollPane createControlPane() {
        VBox controls = new VBox(14);
        controls.setPadding(new Insets(20, 20, 20, 10));
        controls.setStyle("-fx-background-color: transparent;");
        controls.setPrefWidth(340);
        
        controls.getChildren().add(createSection("TIMING", 
            createSliderRow("Dauer", "s", 0.15, 2.0, 0.6, s -> durationSlider = s),
            createSliderRow("Frames", "", 4, 24, 8, s -> { frameCountSlider = s; s.setSnapToTicks(true); s.setMajorTickUnit(1); }),
            createFrameSizeRow()
        ));
        
        controls.getChildren().add(createSection("PARTIKEL",
            createSliderRow("Anzahl", "", 20, 400, 100, s -> particleCountSlider = s),
            createSliderRow("Größe/Leben", "×", 0.2, 2.5, 1.0, s -> sizeOverLifeSlider = s),
            createSliderRow("Debris", "", 0, 30, 8, s -> debrisSlider = s)
        ));
        
        controls.getChildren().add(createSection("FARBEN",
            createColorRow("Kern:", Color.WHITE, c -> coreColorPicker = c),
            createColorRow("Feuer:", Color.ORANGE, c -> midColorPicker = c),
            createColorRow("Rauch:", Color.rgb(60, 55, 50), c -> smokeColorPicker = c)
        ));
        
        controls.getChildren().add(createSection("PHYSIK",
            createSliderRow("Velocity", "", 30, 500, 180, s -> velocitySlider = s),
            createSliderRow("Gravity", "", -300, 300, 60, s -> gravitySlider = s),
            createSliderRow("Drag", "", 0.3, 6.0, 2.2, s -> dragSlider = s),
            createSliderRow("Flash", "", 0, 1.5, 0.8, s -> flashIntensitySlider = s)
        ));
        
        controls.getChildren().add(createSection("LAYER",
            createTogglesRow()
        ));
        
        controls.getChildren().add(createSection("SEED",
            createSeedRow()
        ));
        
        controls.getChildren().add(createSection("PRESETS",
            createPresetsGrid()
        ));
        
        controls.getChildren().add(createSection("EXPORT",
            createExportButtons()
        ));
        
        ScrollPane scroll = new ScrollPane(controls);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }
    
    private VBox createSection(String title, Node... content) {
        VBox section = new VBox(8);
        
        Label header = new Label(title);
        header.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        header.setTextFill(Color.web("#8b949e"));
        
        VBox contentBox = new VBox(6);
        contentBox.getChildren().addAll(content);
        contentBox.setPadding(new Insets(0, 0, 0, 4));
        
        section.getChildren().addAll(header, contentBox);
        return section;
    }
    
    private HBox createSliderRow(String label, String unit, double min, double max, double value, java.util.function.Consumer<Slider> setter) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(label + ":");
        nameLabel.setFont(Font.font("Monospace", 12));
        nameLabel.setTextFill(Color.web("#c9d1d9"));
        nameLabel.setMinWidth(80);
        
        Slider slider = new Slider(min, max, value);
        slider.setPrefWidth(140);
        HBox.setHgrow(slider, Priority.ALWAYS);
        
        Label valueLabel = new Label(formatValue(value, unit));
        valueLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        valueLabel.setTextFill(Color.web("#58a6ff"));
        valueLabel.setMinWidth(50);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        
        slider.valueProperty().addListener((obs, o, n) -> {
            valueLabel.setText(formatValue(n.doubleValue(), unit));
            restartExplosion();
        });
        
        setter.accept(slider);
        row.getChildren().addAll(nameLabel, slider, valueLabel);
        return row;
    }
    
    private String formatValue(double value, String unit) {
        if (unit.isEmpty() && value == Math.floor(value)) {
            return String.format("%.0f", value);
        }
        return String.format("%.2f%s", value, unit);
    }
    
    private HBox createFrameSizeRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label("Größe:");
        nameLabel.setFont(Font.font("Monospace", 12));
        nameLabel.setTextFill(Color.web("#c9d1d9"));
        nameLabel.setMinWidth(80);
        
        frameSizeCombo.getItems().addAll(32, 48, 64, 96);
        frameSizeCombo.setValue(64);
        frameSizeCombo.setOnAction(e -> restartExplosion());
        
        row.getChildren().addAll(nameLabel, frameSizeCombo);
        return row;
    }
    
    private HBox createColorRow(String label, Color defaultColor, java.util.function.Consumer<ColorPicker> setter) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(label);
        nameLabel.setFont(Font.font("Monospace", 12));
        nameLabel.setTextFill(Color.web("#c9d1d9"));
        nameLabel.setMinWidth(50);
        
        ColorPicker picker = new ColorPicker(defaultColor);
        picker.setOnAction(e -> restartExplosion());
        setter.accept(picker);
        
        row.getChildren().addAll(nameLabel, picker);
        return row;
    }
    
    private FlowPane createTogglesRow() {
        FlowPane flow = new FlowPane(10, 6);
        
        fireToggle = createToggle("Feuer", true);
        sparksToggle = createToggle("Funken", true);
        smokeToggle = createToggle("Rauch", true);
        shockwaveToggle = createToggle("Schock", false);
        debrisToggle = createToggle("Debris", true);
        flashToggle = createToggle("Flash", true);
        
        flow.getChildren().addAll(fireToggle, sparksToggle, smokeToggle, shockwaveToggle, debrisToggle, flashToggle);
        return flow;
    }
    
    private CheckBox createToggle(String text, boolean selected) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.setFont(Font.font("Monospace", 11));
        cb.setTextFill(Color.web("#c9d1d9"));
        cb.setOnAction(e -> restartExplosion());
        return cb;
    }
    
    private HBox createSeedRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        
        seedField = new TextField(String.valueOf(currentSeed));
        seedField.setFont(Font.font("Monospace", 11));
        seedField.setPrefWidth(100);
        seedField.setStyle("-fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d;");
        seedField.setOnAction(e -> restartExplosion());
        
        Button randomBtn = createStyledButton("🎲 Neu", "#238636", "#2ea043");
        randomBtn.setOnAction(e -> randomizeSeed());
        
        Button copyBtn = createStyledButton("📋", "#30363d", "#3d444d");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(
                java.util.Collections.singletonMap(javafx.scene.input.DataFormat.PLAIN_TEXT, seedField.getText())
            );
        });
        
        row.getChildren().addAll(seedField, randomBtn, copyBtn);
        return row;
    }
    
    private GridPane createPresetsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        
        String[][] presets = {
            {"Treffer", "treffer", "#f85149"},
            {"Schiff", "schiff", "#f0883e"},
            {"Tough", "tough", "#a371f7"},
            {"Boss", "boss", "#db61a2"},
            {"Plasma", "plasma", "#3fb950"},
            {"Funken", "funken", "#58a6ff"}
        };
        
        for (int i = 0; i < presets.length; i++) {
            String name = presets[i][0];
            String id = presets[i][1];
            String color = presets[i][2];
            
            Button btn = new Button(name);
            btn.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
            btn.setPrefWidth(90);
            btn.setStyle(String.format(
                "-fx-background-color: %s20; -fx-text-fill: %s; -fx-border-color: %s40; -fx-border-radius: 4; -fx-background-radius: 4;",
                color, color, color
            ));
            btn.setOnMouseEntered(e -> btn.setStyle(String.format(
                "-fx-background-color: %s40; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-radius: 4; -fx-background-radius: 4;",
                color, color, color
            )));
            btn.setOnMouseExited(e -> btn.setStyle(String.format(
                "-fx-background-color: %s20; -fx-text-fill: %s; -fx-border-color: %s40; -fx-border-radius: 4; -fx-background-radius: 4;",
                color, color, color
            )));
            btn.setOnAction(e -> {
                applyPreset(id);
                presetNameLabel.setText(name);
                presetNameLabel.setTextFill(Color.web(color));
            });
            
            grid.add(btn, i % 3, i / 3);
        }
        
        return grid;
    }
    
    private VBox createExportButtons() {
        VBox box = new VBox(6);
        
        Button sheetBtn = createStyledButton("📦 Sprite Sheet exportieren...", "#238636", "#2ea043");
        sheetBtn.setMaxWidth(Double.MAX_VALUE);
        sheetBtn.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        sheetBtn.setOnAction(e -> exportSpriteSheet());
        
        Button framesBtn = createStyledButton("🖼 Einzelframes exportieren...", "#1f6feb", "#388bfd");
        framesBtn.setMaxWidth(Double.MAX_VALUE);
        framesBtn.setOnAction(e -> exportIndividualFrames());
        
        box.getChildren().addAll(sheetBtn, framesBtn);
        return box;
    }
    
    private Button createStyledButton(String text, String bgColor, String hoverColor) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Monospace", 11));
        btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 6;", bgColor));
        btn.setOnMouseEntered(e -> btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 6;", hoverColor)));
        btn.setOnMouseExited(e -> btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 6;", bgColor)));
        return btn;
    }
    
    private VBox createBottomPane() {
        VBox bottom = new VBox(0);
        
        HBox playbackPane = new HBox(12);
        playbackPane.setAlignment(Pos.CENTER);
        playbackPane.setPadding(new Insets(12, 20, 12, 20));
        playbackPane.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 1 0 0 0;");
        
        Button playPauseBtn = new Button("⏸");
        playPauseBtn.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        playPauseBtn.setPrefSize(40, 32);
        playPauseBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-background-radius: 6;");
        playPauseBtn.setOnAction(e -> {
            isPlaying = !isPlaying;
            playPauseBtn.setText(isPlaying ? "⏸" : "▶");
            playPauseBtn.setStyle(isPlaying ? 
                "-fx-background-color: #238636; -fx-text-fill: white; -fx-background-radius: 6;" :
                "-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-background-radius: 6;");
            if (isPlaying) lastNanos = System.nanoTime();
        });
        
        Button restartBtn = new Button("⟲");
        restartBtn.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        restartBtn.setPrefSize(40, 32);
        restartBtn.setStyle("-fx-background-color: #30363d; -fx-text-fill: #c9d1d9; -fx-background-radius: 6;");
        restartBtn.setOnAction(e -> restartExplosion());
        
        CheckBox loopToggle = new CheckBox("Loop");
        loopToggle.setSelected(true);
        loopToggle.setFont(Font.font("Monospace", 11));
        loopToggle.setTextFill(Color.web("#8b949e"));
        loopToggle.setOnAction(e -> isLooping = loopToggle.isSelected());
        
        frameSlider = new Slider(0, 1, 0);
        frameSlider.setPrefWidth(250);
        HBox.setHgrow(frameSlider, Priority.ALWAYS);
        frameSlider.valueProperty().addListener((obs, o, n) -> {
            if (!isPlaying) {
                double duration = durationSlider.getValue();
                playbackTime = n.doubleValue() * duration;
            }
        });
        
        currentFrameLabel = new Label("Frame: 1/8");
        currentFrameLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        currentFrameLabel.setTextFill(Color.web("#58a6ff"));
        currentFrameLabel.setMinWidth(90);
        
        playbackTimeLabel = new Label("0.00s");
        playbackTimeLabel.setFont(Font.font("Monospace", 12));
        playbackTimeLabel.setTextFill(Color.web("#8b949e"));
        playbackTimeLabel.setMinWidth(50);
        
        playbackPane.getChildren().addAll(playPauseBtn, restartBtn, loopToggle, frameSlider, currentFrameLabel, playbackTimeLabel);
        
        bottom.getChildren().add(playbackPane);
        return bottom;
    }
    
    private void applyPreset(String presetId) {
        currentPresetName = presetId.substring(0, 1).toUpperCase() + presetId.substring(1);
        
        switch (presetId) {
            case "treffer" -> {
                durationSlider.setValue(0.2);
                frameCountSlider.setValue(6);
                frameSizeCombo.setValue(32);
                particleCountSlider.setValue(40);
                sizeOverLifeSlider.setValue(0.4);
                coreColorPicker.setValue(Color.WHITE);
                midColorPicker.setValue(Color.YELLOW);
                smokeColorPicker.setValue(Color.rgb(70, 65, 60));
                gravitySlider.setValue(0);
                velocitySlider.setValue(120);
                dragSlider.setValue(4.0);
                flashIntensitySlider.setValue(1.2);
                debrisSlider.setValue(3);
                fireToggle.setSelected(false);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(false);
                shockwaveToggle.setSelected(false);
                debrisToggle.setSelected(true);
                flashToggle.setSelected(true);
            }
            case "schiff" -> {
                durationSlider.setValue(0.5);
                frameCountSlider.setValue(8);
                frameSizeCombo.setValue(64);
                particleCountSlider.setValue(100);
                sizeOverLifeSlider.setValue(1.0);
                coreColorPicker.setValue(Color.WHITE);
                midColorPicker.setValue(Color.ORANGE);
                smokeColorPicker.setValue(Color.rgb(50, 45, 40));
                gravitySlider.setValue(60);
                velocitySlider.setValue(180);
                dragSlider.setValue(2.2);
                flashIntensitySlider.setValue(0.9);
                debrisSlider.setValue(10);
                fireToggle.setSelected(true);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(true);
                shockwaveToggle.setSelected(false);
                debrisToggle.setSelected(true);
                flashToggle.setSelected(true);
            }
            case "tough" -> {
                durationSlider.setValue(0.7);
                frameCountSlider.setValue(10);
                frameSizeCombo.setValue(64);
                particleCountSlider.setValue(150);
                sizeOverLifeSlider.setValue(1.3);
                coreColorPicker.setValue(Color.LIGHTYELLOW);
                midColorPicker.setValue(Color.DARKORANGE);
                smokeColorPicker.setValue(Color.rgb(55, 45, 35));
                gravitySlider.setValue(40);
                velocitySlider.setValue(200);
                dragSlider.setValue(1.9);
                flashIntensitySlider.setValue(1.0);
                debrisSlider.setValue(15);
                fireToggle.setSelected(true);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(true);
                shockwaveToggle.setSelected(true);
                debrisToggle.setSelected(true);
                flashToggle.setSelected(true);
            }
            case "boss" -> {
                durationSlider.setValue(1.0);
                frameCountSlider.setValue(14);
                frameSizeCombo.setValue(96);
                particleCountSlider.setValue(280);
                sizeOverLifeSlider.setValue(1.6);
                coreColorPicker.setValue(Color.WHITE);
                midColorPicker.setValue(Color.ORANGERED);
                smokeColorPicker.setValue(Color.rgb(35, 30, 25));
                gravitySlider.setValue(50);
                velocitySlider.setValue(260);
                dragSlider.setValue(1.5);
                flashIntensitySlider.setValue(1.3);
                debrisSlider.setValue(25);
                fireToggle.setSelected(true);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(true);
                shockwaveToggle.setSelected(true);
                debrisToggle.setSelected(true);
                flashToggle.setSelected(true);
            }
            case "plasma" -> {
                durationSlider.setValue(0.45);
                frameCountSlider.setValue(8);
                frameSizeCombo.setValue(64);
                particleCountSlider.setValue(120);
                sizeOverLifeSlider.setValue(0.7);
                coreColorPicker.setValue(Color.WHITE);
                midColorPicker.setValue(Color.CYAN);
                smokeColorPicker.setValue(Color.rgb(60, 30, 90));
                gravitySlider.setValue(-30);
                velocitySlider.setValue(190);
                dragSlider.setValue(2.8);
                flashIntensitySlider.setValue(1.1);
                debrisSlider.setValue(5);
                fireToggle.setSelected(true);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(false);
                shockwaveToggle.setSelected(true);
                debrisToggle.setSelected(false);
                flashToggle.setSelected(true);
            }
            case "funken" -> {
                durationSlider.setValue(0.35);
                frameCountSlider.setValue(8);
                frameSizeCombo.setValue(48);
                particleCountSlider.setValue(60);
                sizeOverLifeSlider.setValue(0.25);
                coreColorPicker.setValue(Color.LIGHTYELLOW);
                midColorPicker.setValue(Color.ORANGE);
                smokeColorPicker.setValue(Color.DARKGRAY);
                gravitySlider.setValue(200);
                velocitySlider.setValue(250);
                dragSlider.setValue(1.2);
                flashIntensitySlider.setValue(0.6);
                debrisSlider.setValue(0);
                fireToggle.setSelected(false);
                sparksToggle.setSelected(true);
                smokeToggle.setSelected(false);
                shockwaveToggle.setSelected(false);
                debrisToggle.setSelected(false);
                flashToggle.setSelected(true);
            }
        }
        restartExplosion();
    }
    
    private void randomizeSeed() {
        currentSeed = System.nanoTime();
        seedField.setText(String.valueOf(currentSeed));
        restartExplosion();
    }
    
    private void restartExplosion() {
        playbackTime = 0;
        try {
            currentSeed = Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException e) {
            currentSeed = System.nanoTime();
            seedField.setText(String.valueOf(currentSeed));
        }
        initializeParticles();
    }
    
    private void initializeParticles() {
        particles.clear();
        random.setSeed(currentSeed);
        
        int frameSize = frameSizeCombo.getValue();
        int hiresSize = frameSize * HIRES_SCALE;
        double centerX = hiresSize / 2.0;
        double centerY = hiresSize / 2.0;
        double velocity = velocitySlider.getValue() * HIRES_SCALE;
        
        int count = (int) particleCountSlider.getValue();
        
        if (smokeToggle.isSelected()) {
            int smokeCount = count / 3;
            for (int i = 0; i < smokeCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = velocity * (0.15 + random.nextDouble() * 0.35);
                double size = (5 + random.nextDouble() * 8) * HIRES_SCALE;
                double delay = random.nextDouble() * 0.15;
                particles.add(new Particle(
                    centerX + (random.nextDouble() - 0.5) * 6 * HIRES_SCALE,
                    centerY + (random.nextDouble() - 0.5) * 6 * HIRES_SCALE,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.SMOKE,
                    0.6 + random.nextDouble() * 0.4,
                    delay
                ));
            }
        }
        
        if (fireToggle.isSelected()) {
            int fireCount = count / 3;
            for (int i = 0; i < fireCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = velocity * (0.25 + random.nextDouble() * 0.6);
                double size = (3 + random.nextDouble() * 5) * HIRES_SCALE;
                particles.add(new Particle(
                    centerX + (random.nextDouble() - 0.5) * 3 * HIRES_SCALE,
                    centerY + (random.nextDouble() - 0.5) * 3 * HIRES_SCALE,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.FIRE,
                    0.7 + random.nextDouble() * 0.3,
                    0
                ));
            }
        }
        
        if (sparksToggle.isSelected()) {
            int sparkCount = count / 4;
            for (int i = 0; i < sparkCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = velocity * (0.7 + random.nextDouble() * 0.9);
                double size = (1 + random.nextDouble() * 2) * HIRES_SCALE;
                particles.add(new Particle(
                    centerX, centerY,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.SPARK,
                    0.5 + random.nextDouble() * 0.5,
                    0
                ));
            }
        }
        
        int coreCount = count / 8 + 5;
        for (int i = 0; i < coreCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = velocity * (0.05 + random.nextDouble() * 0.2);
            double size = (2 + random.nextDouble() * 4) * HIRES_SCALE;
            particles.add(new Particle(
                centerX + (random.nextDouble() - 0.5) * 2 * HIRES_SCALE,
                centerY + (random.nextDouble() - 0.5) * 2 * HIRES_SCALE,
                Math.cos(angle) * speed, Math.sin(angle) * speed,
                size, ParticleType.CORE,
                0.25 + random.nextDouble() * 0.25,
                0
            ));
        }
        
        if (debrisToggle.isSelected()) {
            int debrisCount = (int) debrisSlider.getValue();
            for (int i = 0; i < debrisCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = velocity * (0.4 + random.nextDouble() * 0.8);
                double size = (1.5 + random.nextDouble() * 2.5) * HIRES_SCALE;
                particles.add(new Particle(
                    centerX, centerY,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.DEBRIS,
                    0.8 + random.nextDouble() * 0.2,
                    0
                ));
            }
        }
        
        if (shockwaveToggle.isSelected()) {
            shockwaveRadius = 0;
            shockwaveMaxRadius = hiresSize * 0.42;
        }
        
        if (flashToggle.isSelected()) {
            flashAlpha = flashIntensitySlider.getValue();
        } else {
            flashAlpha = 0;
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
                
                renderPreview();
                updateLabels();
            }
        };
        animationTimer.start();
        initializeParticles();
    }
    
    private void renderPreview() {
        int frameSize = frameSizeCombo.getValue();
        
        if (renderBuffer == null || renderBuffer.getWidth() != frameSize) {
            renderBuffer = new WritableImage(frameSize, frameSize);
        }
        
        renderFrameToImage(renderBuffer, playbackTime);
        
        previewGc.setFill(Color.rgb(1, 4, 9));
        previewGc.fillRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
        
        drawStarfield();
        
        previewGc.setImageSmoothing(false);
        double scale = (double) PREVIEW_SIZE / frameSize;
        double offsetX = (PREVIEW_SIZE - frameSize * scale) / 2;
        double offsetY = (PREVIEW_SIZE - frameSize * scale) / 2;
        previewGc.drawImage(renderBuffer, offsetX, offsetY, frameSize * scale, frameSize * scale);
    }
    
    private void drawStarfield() {
        Random starRand = new Random(42);
        for (int i = 0; i < 50; i++) {
            double x = starRand.nextDouble() * PREVIEW_SIZE;
            double y = starRand.nextDouble() * PREVIEW_SIZE;
            double size = 1 + starRand.nextDouble() * 1.5;
            double brightness = 0.2 + starRand.nextDouble() * 0.4;
            previewGc.setFill(Color.gray(brightness));
            previewGc.fillRect((int)x, (int)y, (int)Math.ceil(size), (int)Math.ceil(size));
        }
    }
    
    private void renderFrameToImage(WritableImage img, double time) {
        int frameSize = (int) img.getWidth();
        int hiresSize = frameSize * HIRES_SCALE;
        
        if (hiresBuffer == null || hiresBuffer.getWidth() != hiresSize) {
            hiresBuffer = new WritableImage(hiresSize, hiresSize);
            accumulatorR = new int[hiresSize][hiresSize];
            accumulatorG = new int[hiresSize][hiresSize];
            accumulatorB = new int[hiresSize][hiresSize];
            accumulatorA = new int[hiresSize][hiresSize];
        }
        
        for (int y = 0; y < hiresSize; y++) {
            for (int x = 0; x < hiresSize; x++) {
                accumulatorR[y][x] = 0;
                accumulatorG[y][x] = 0;
                accumulatorB[y][x] = 0;
                accumulatorA[y][x] = 0;
            }
        }
        
        double duration = durationSlider.getValue();
        double progress = Math.min(1.0, time / duration);
        double sizeMultiplier = sizeOverLifeSlider.getValue();
        double gravity = gravitySlider.getValue() * HIRES_SCALE;
        double drag = dragSlider.getValue();
        
        Color coreColor = coreColorPicker.getValue();
        Color midColor = midColorPicker.getValue();
        Color smokeColor = smokeColorPicker.getValue();
        
        double centerX = hiresSize / 2.0;
        double centerY = hiresSize / 2.0;
        
        simulateParticles(time, gravity, drag, duration);
        
        if (flashToggle.isSelected() && progress < 0.15) {
            double flashProgress = progress / 0.15;
            double currentFlash = flashAlpha * (1.0 - flashProgress * flashProgress);
            if (currentFlash > 0.05) {
                int flashR = (int)(coreColor.getRed() * 255);
                int flashG = (int)(coreColor.getGreen() * 255);
                int flashB = (int)(coreColor.getBlue() * 255);
                double flashRadius = hiresSize * 0.3 * (0.5 + flashProgress * 0.5);
                
                for (int y = 0; y < hiresSize; y++) {
                    for (int x = 0; x < hiresSize; x++) {
                        double dx = x - centerX;
                        double dy = y - centerY;
                        double dist = Math.sqrt(dx*dx + dy*dy);
                        if (dist < flashRadius) {
                            double falloff = 1.0 - (dist / flashRadius);
                            falloff = falloff * falloff;
                            int alpha = (int)(currentFlash * falloff * 255);
                            accumulatorR[y][x] = Math.min(255 * 4, accumulatorR[y][x] + flashR * alpha / 255);
                            accumulatorG[y][x] = Math.min(255 * 4, accumulatorG[y][x] + flashG * alpha / 255);
                            accumulatorB[y][x] = Math.min(255 * 4, accumulatorB[y][x] + flashB * alpha / 255);
                            accumulatorA[y][x] = Math.min(255 * 4, accumulatorA[y][x] + alpha);
                        }
                    }
                }
            }
        }
        
        if (shockwaveToggle.isSelected() && progress > 0.02 && progress < 0.7) {
            double shockProgress = (progress - 0.02) / 0.68;
            double radius = shockwaveMaxRadius * shockProgress;
            double ringAlpha = Math.max(0, 0.7 * (1.0 - shockProgress));
            double thickness = 3.0 * HIRES_SCALE * (1.0 - shockProgress * 0.5);
            
            int mr = (int)(midColor.getRed() * 255);
            int mg = (int)(midColor.getGreen() * 255);
            int mb = (int)(midColor.getBlue() * 255);
            
            for (int y = 0; y < hiresSize; y++) {
                for (int x = 0; x < hiresSize; x++) {
                    double dx = x - centerX;
                    double dy = y - centerY;
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    double ringDist = Math.abs(dist - radius);
                    if (ringDist < thickness) {
                        double falloff = 1.0 - (ringDist / thickness);
                        int alpha = (int)(ringAlpha * falloff * 255);
                        accumulatorR[y][x] = Math.min(255 * 4, accumulatorR[y][x] + mr * alpha / 255);
                        accumulatorG[y][x] = Math.min(255 * 4, accumulatorG[y][x] + mg * alpha / 255);
                        accumulatorB[y][x] = Math.min(255 * 4, accumulatorB[y][x] + mb * alpha / 255);
                        accumulatorA[y][x] = Math.min(255 * 4, accumulatorA[y][x] + alpha);
                    }
                }
            }
        }
        
        List<Particle> sortedParticles = new ArrayList<>(particles);
        sortedParticles.sort((a, b) -> {
            int ao = getLayerOrder(a.type);
            int bo = getLayerOrder(b.type);
            return Integer.compare(ao, bo);
        });
        
        for (Particle p : sortedParticles) {
            if (p.life <= 0 || progress < p.delay) continue;
            
            double adjustedProgress = (progress - p.delay) / (1.0 - p.delay);
            double life = Math.max(0, 1.0 - (adjustedProgress / p.lifeFactor));
            
            double size = p.baseSize * (0.4 + 0.6 * life) * sizeMultiplier;
            double alpha = life;
            
            Color color;
            boolean additive = false;
            
            switch (p.type) {
                case CORE -> {
                    color = coreColor;
                    alpha *= 0.95;
                    additive = true;
                }
                case FIRE -> {
                    double blend = life * life;
                    color = interpolateColor(smokeColor, interpolateColor(midColor, coreColor, blend), life);
                    alpha *= 0.85;
                    additive = true;
                }
                case SPARK -> {
                    color = interpolateColor(midColor, coreColor, life);
                    alpha *= life;
                    additive = true;
                    size = Math.max(HIRES_SCALE, size * 0.6);
                }
                case SMOKE -> {
                    color = smokeColor;
                    alpha *= 0.5 * life;
                    size *= 1.8 - life * 0.6;
                    additive = false;
                }
                case DEBRIS -> {
                    color = interpolateColor(Color.gray(0.3), midColor, life * 0.5);
                    alpha *= 0.9;
                    additive = false;
                }
                default -> {
                    color = Color.WHITE;
                    additive = true;
                }
            }
            
            drawParticle(hiresSize, p.x, p.y, size, color, alpha, p.type == ParticleType.SPARK, additive);
        }
        
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < frameSize; y++) {
            for (int x = 0; x < frameSize; x++) {
                int totalR = 0, totalG = 0, totalB = 0, totalA = 0;
                
                for (int sy = 0; sy < HIRES_SCALE; sy++) {
                    for (int sx = 0; sx < HIRES_SCALE; sx++) {
                        int hx = x * HIRES_SCALE + sx;
                        int hy = y * HIRES_SCALE + sy;
                        totalR += accumulatorR[hy][hx];
                        totalG += accumulatorG[hy][hx];
                        totalB += accumulatorB[hy][hx];
                        totalA += accumulatorA[hy][hx];
                    }
                }
                
                int samples = HIRES_SCALE * HIRES_SCALE;
                int r = Math.min(255, totalR / samples);
                int g = Math.min(255, totalG / samples);
                int b = Math.min(255, totalB / samples);
                int a = Math.min(255, totalA / samples);
                
                pw.setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }
    
    private int getLayerOrder(ParticleType type) {
        return switch (type) {
            case SMOKE -> 0;
            case DEBRIS -> 1;
            case FIRE -> 2;
            case SPARK -> 3;
            case CORE -> 4;
        };
    }
    
    private void simulateParticles(double targetTime, double gravity, double drag, double duration) {
        for (Particle p : particles) {
            if (targetTime < p.delay) continue;
            
            double simTime = targetTime - p.delay;
            double simDuration = duration - p.delay;
            
            random.setSeed(currentSeed + (long)(p.x * 1000 + p.y * 1000000));
            double px = p.startX;
            double py = p.startY;
            double vx = p.startVx;
            double vy = p.startVy;
            
            double dt = 1.0 / 120.0;
            double t = 0;
            
            while (t < simTime) {
                double step = Math.min(dt, simTime - t);
                vy += gravity * step;
                vx *= Math.pow(1.0 - drag * 0.1, step * 60);
                vy *= Math.pow(1.0 - drag * 0.1, step * 60);
                px += vx * step;
                py += vy * step;
                t += step;
            }
            
            p.x = px;
            p.y = py;
            p.vx = vx;
            p.vy = vy;
            
            double progress = simTime / Math.max(0.001, simDuration);
            p.life = Math.max(0, 1.0 - (progress / p.lifeFactor));
        }
    }
    
    private void drawParticle(int size, double cx, double cy, double psize, Color color, double alpha, boolean isSpark, boolean additive) {
        int icx = (int) cx;
        int icy = (int) cy;
        int radius = (int) Math.ceil(psize / 2);
        
        int r = (int)(color.getRed() * 255);
        int g = (int)(color.getGreen() * 255);
        int b = (int)(color.getBlue() * 255);
        
        if (isSpark) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int px = icx + dx;
                    int py = icy + dy;
                    if (px < 0 || px >= size || py < 0 || py >= size) continue;
                    double falloff = (dx == 0 && dy == 0) ? 1.0 : 0.3;
                    int a = (int)(alpha * falloff * 255);
                    if (additive) {
                        accumulatorR[py][px] = Math.min(255 * 4, accumulatorR[py][px] + r * a / 255);
                        accumulatorG[py][px] = Math.min(255 * 4, accumulatorG[py][px] + g * a / 255);
                        accumulatorB[py][px] = Math.min(255 * 4, accumulatorB[py][px] + b * a / 255);
                    } else {
                        accumulatorR[py][px] = r;
                        accumulatorG[py][px] = g;
                        accumulatorB[py][px] = b;
                    }
                    accumulatorA[py][px] = Math.min(255 * 4, accumulatorA[py][px] + a);
                }
            }
            return;
        }
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = icx + dx;
                int py = icy + dy;
                if (px < 0 || px >= size || py < 0 || py >= size) continue;
                
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist <= psize / 2) {
                    double falloff = 1.0 - (dist / (psize / 2));
                    falloff = falloff * falloff;
                    int a = (int)(alpha * falloff * 255);
                    
                    if (additive) {
                        accumulatorR[py][px] = Math.min(255 * 4, accumulatorR[py][px] + r * a / 255);
                        accumulatorG[py][px] = Math.min(255 * 4, accumulatorG[py][px] + g * a / 255);
                        accumulatorB[py][px] = Math.min(255 * 4, accumulatorB[py][px] + b * a / 255);
                    } else {
                        int existingA = accumulatorA[py][px];
                        if (existingA < a) {
                            accumulatorR[py][px] = r;
                            accumulatorG[py][px] = g;
                            accumulatorB[py][px] = b;
                        }
                    }
                    accumulatorA[py][px] = Math.min(255 * 4, accumulatorA[py][px] + a);
                }
            }
        }
    }
    
    private Color interpolateColor(Color c1, Color c2, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            c1.getRed() + (c2.getRed() - c1.getRed()) * t,
            c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t,
            c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t,
            1.0
        );
    }
    
    private void updateLabels() {
        int frameCount = (int) frameCountSlider.getValue();
        double duration = durationSlider.getValue();
        int currentFrame = Math.min(frameCount, (int) (playbackTime / duration * frameCount) + 1);
        currentFrameLabel.setText("Frame: " + currentFrame + "/" + frameCount);
        playbackTimeLabel.setText(String.format("%.2fs", playbackTime));
    }
    
    private void exportSampleExplosions() {
        try {
            Path explosionsDir = Path.of("src/main/resources/assets/textures/explosions");
            Files.createDirectories(explosionsDir);
            
            String[] presets = {"treffer", "schiff", "tough", "boss", "plasma", "funken"};
            
            for (String preset : presets) {
                applyPreset(preset);
                currentSeed = preset.hashCode() * 12345L;
                seedField.setText(String.valueOf(currentSeed));
                
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
                
                for (int i = 0; i < frameCount; i++) {
                    double frameTime = (i / (double) frameCount) * duration;
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
                
                String filename = "explosion_" + preset + ".png";
                byte[] pngBytes = encodePng8BitRgba(sheet);
                Files.write(explosionsDir.resolve(filename), pngBytes);
                
                String jsonFilename = "explosion_" + preset + ".json";
                String json = String.format(
                    "{\n  \"name\": \"%s\",\n  \"frameWidth\": %d,\n  \"frameHeight\": %d,\n  \"frameCount\": %d,\n  \"fps\": %.2f,\n  \"columns\": %d,\n  \"rows\": %d\n}",
                    preset, frameSize, frameSize, frameCount, fps, cols, rows
                );
                Files.writeString(explosionsDir.resolve(jsonFilename), json);
            }
            
            applyPreset("schiff");
            System.out.println("Sample explosions exported to: " + explosionsDir.toAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Failed to export sample explosions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void exportSpriteSheet() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sprite Sheet speichern");
        fileChooser.setInitialFileName("explosion_" + currentPresetName.toLowerCase() + ".png");
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
            
            for (int i = 0; i < frameCount; i++) {
                double frameTime = (i / (double) frameCount) * duration;
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
            
            showAlert(Alert.AlertType.INFORMATION, "Export erfolgreich",
                "Sprite Sheet gespeichert:\n" + file.getName() + "\n\n" + 
                "Größe: " + (cols * frameSize) + "×" + (rows * frameSize) + " px\n" +
                "Frames: " + frameCount + " @ " + String.format("%.1f", fps) + " FPS");
                
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
            
            for (int i = 0; i < frameCount; i++) {
                double frameTime = (i / (double) frameCount) * duration;
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
            
            showAlert(Alert.AlertType.INFORMATION, "Export erfolgreich",
                frameCount + " Frames exportiert nach:\n" + dir.getAbsolutePath());
                
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export fehlgeschlagen", e.getMessage());
        }
    }
    
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
        CORE, FIRE, SPARK, SMOKE, DEBRIS
    }
    
    private static class Particle {
        double x, y;
        double vx, vy;
        double startX, startY;
        double startVx, startVy;
        double baseSize;
        double life;
        double lifeFactor;
        double delay;
        ParticleType type;
        
        Particle(double x, double y, double vx, double vy, double size, ParticleType type, double lifeFactor, double delay) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.startY = y;
            this.vx = vx;
            this.vy = vy;
            this.startVx = vx;
            this.startVy = vy;
            this.baseSize = size;
            this.type = type;
            this.lifeFactor = lifeFactor;
            this.life = 1.0;
            this.delay = delay;
        }
    }
}
