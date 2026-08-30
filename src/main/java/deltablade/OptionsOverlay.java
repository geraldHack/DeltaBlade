package deltablade;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

/**
 * Arcade-styled options panel: music on/off, volume, track picker.
 */
public final class OptionsOverlay {

    private static final String BUTTON_BASE =
            "-fx-background-color: linear-gradient(to bottom, #2a5298, #1e3c72);"
                    + "-fx-text-fill: white;"
                    + "-fx-padding: 10 28;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #4a90d9;"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 8;"
                    + "-fx-cursor: hand;";

    private static final String BUTTON_HOVER =
            "-fx-background-color: linear-gradient(to bottom, #3a6ab8, #2e4c82);"
                    + "-fx-text-fill: white;"
                    + "-fx-padding: 10 28;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #6ab0f9;"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 8;"
                    + "-fx-cursor: hand;";

    private static final String TOGGLE_ON =
            "-fx-background-color: linear-gradient(to bottom, #1f8a7a, #0e5c52);"
                    + "-fx-text-fill: white;"
                    + "-fx-padding: 8 22;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #4ecdc4;"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 8;"
                    + "-fx-cursor: hand;";

    private static final String TOGGLE_OFF =
            "-fx-background-color: linear-gradient(to bottom, #3a3f4a, #2a2e36);"
                    + "-fx-text-fill: #c8c8c8;"
                    + "-fx-padding: 8 22;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #6a7080;"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 8;"
                    + "-fx-cursor: hand;";

    private static boolean stylesheetAdded;

    private final Group root = new Group();
    private Button musicToggle;
    private Slider volumeSlider;
    private Text volumeValue;
    private Text trackName;
    private Button prevTrack;
    private Button nextTrack;

    public OptionsOverlay(Runnable onClose) {
        double width = getAppWidth();
        double height = getAppHeight();

        Rectangle dimmer = new Rectangle(width, height);
        dimmer.setFill(Color.rgb(0, 0, 0, 0.72));

        double panelW = 460;
        double panelH = 360;
        double panelX = (width - panelW) / 2.0;
        double panelY = (height - panelH) / 2.0;

        Rectangle panel = new Rectangle(panelW, panelH);
        panel.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(12, 22, 40, 0.97)),
                new Stop(1, Color.rgb(8, 14, 26, 0.97))));
        panel.setStroke(Color.rgb(74, 144, 217));
        panel.setStrokeWidth(2);
        panel.setArcWidth(12);
        panel.setArcHeight(12);
        panel.setTranslateX(panelX);
        panel.setTranslateY(panelY);
        panel.setEffect(new DropShadow(24, Color.rgb(0, 220, 255, 0.25)));

        Rectangle accent = new Rectangle(panelW, 3);
        accent.setFill(Color.CYAN);
        accent.setTranslateX(panelX);
        accent.setTranslateY(panelY);

        Text title = new Text("OPTIONEN");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.CYAN);
        title.setEffect(new Glow(0.45));

        musicToggle = new Button();
        musicToggle.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        musicToggle.setPrefWidth(92);
        musicToggle.setOnAction(e -> toggleMusic());

        HBox musicRow = labeledRow("Hintergrundmusik", musicToggle);

        volumeValue = new Text();
        volumeValue.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        volumeValue.setFill(Color.rgb(180, 240, 255));
        volumeValue.setWrappingWidth(48);

        volumeSlider = new Slider(0, 100, OptionsStore.getMusicVolume() * 100.0);
        volumeSlider.getStyleClass().add("options-slider");
        volumeSlider.setPrefWidth(220);
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double volume = newVal.doubleValue() / 100.0;
            OptionsStore.setMusicVolume(volume);
            MusicHelper.setVolume(volume);
            volumeValue.setText(Math.round(newVal.doubleValue()) + "%");
        });
        volumeValue.setText(Math.round(volumeSlider.getValue()) + "%");

        HBox volumeControls = new HBox(10, volumeSlider, volumeValue);
        volumeControls.setAlignment(Pos.CENTER_LEFT);
        VBox volumeBlock = new VBox(6, sectionLabel("Lautstärke"), volumeControls);

        trackName = new Text();
        trackName.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        trackName.setFill(Color.WHITE);

        prevTrack = cycleButton("<", -1);
        nextTrack = cycleButton(">", 1);

        HBox trackRow = new HBox(12, prevTrack, trackName, nextTrack);
        trackRow.setAlignment(Pos.CENTER);
        VBox trackBlock = new VBox(6, sectionLabel("Titel"), trackRow);
        trackBlock.setAlignment(Pos.CENTER);

        Button back = styledButton("ZURÜCK");
        back.setPrefWidth(180);
        back.setOnAction(e -> onClose.run());

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28, 36, 24, 36));
        content.setPrefWidth(panelW);
        content.setTranslateX(panelX);
        content.setTranslateY(panelY);
        content.getChildren().addAll(title, musicRow, volumeBlock, trackBlock, back);

        root.getChildren().addAll(dimmer, panel, accent, content);
        refreshControls();
        ensureStylesheet();
    }

    public Group getRoot() {
        return root;
    }

    private void toggleMusic() {
        OptionsStore.setMusicEnabled(!OptionsStore.isMusicEnabled());
        MusicHelper.applyFromStore();
        refreshControls();
    }

    private void cycleTrack(int delta) {
        List<MusicHelper.Track> tracks = MusicHelper.tracks();
        if (tracks.size() <= 1) {
            return;
        }
        int next = Math.floorMod(MusicHelper.indexOf(OptionsStore.getSelectedTrackId()) + delta, tracks.size());
        OptionsStore.setSelectedTrackId(tracks.get(next).id());
        MusicHelper.applyFromStore();
        refreshControls();
    }

    private void refreshControls() {
        boolean enabled = OptionsStore.isMusicEnabled();
        musicToggle.setText(enabled ? "AN" : "AUS");
        musicToggle.setStyle(enabled ? TOGGLE_ON : TOGGLE_OFF);

        volumeSlider.setDisable(!enabled);
        volumeValue.setOpacity(enabled ? 1.0 : 0.45);

        MusicHelper.Track track = MusicHelper.find(OptionsStore.getSelectedTrackId());
        trackName.setText(track != null ? track.displayName() : "—");

        boolean manyTracks = MusicHelper.tracks().size() > 1;
        prevTrack.setDisable(!manyTracks);
        nextTrack.setDisable(!manyTracks);
        prevTrack.setOpacity(manyTracks ? 1.0 : 0.35);
        nextTrack.setOpacity(manyTracks ? 1.0 : 0.35);
    }

    private static HBox labeledRow(String label, Button control) {
        Text text = sectionLabel(label);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, text, spacer, control);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(380);
        return row;
    }

    private static Text sectionLabel(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        text.setFill(Color.rgb(160, 210, 240));
        return text;
    }

    private Button cycleButton(String label, int delta) {
        Button button = styledButton(label);
        button.setPrefWidth(44);
        button.setOnAction(e -> cycleTrack(delta));
        return button;
    }

    private static Button styledButton(String label) {
        Button button = new Button(label);
        button.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        button.setStyle(BUTTON_BASE);
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                button.setStyle(BUTTON_HOVER);
            }
        });
        button.setOnMouseExited(e -> button.setStyle(BUTTON_BASE));
        return button;
    }

    private static void ensureStylesheet() {
        if (stylesheetAdded) {
            return;
        }
        Platform.runLater(() -> {
            var scene = javafx.stage.Stage.getWindows().stream()
                    .filter(window -> window.isShowing() && window.getScene() != null)
                    .map(javafx.stage.Window::getScene)
                    .findFirst()
                    .orElse(null);
            if (scene == null) {
                return;
            }
            var url = OptionsOverlay.class.getResource("/deltablade/options.css");
            if (url != null && !scene.getStylesheets().contains(url.toExternalForm())) {
                scene.getStylesheets().add(url.toExternalForm());
                stylesheetAdded = true;
            }
        });
    }
}
