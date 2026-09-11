package deltablade;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;
import java.util.function.DoubleConsumer;

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

    private final Group root = new Group();
    private Button musicToggle;
    private VolumeBar volumeBar;
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
        double panelH = 420;
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
        volumeValue.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        volumeValue.setFill(Color.rgb(180, 240, 255));
        volumeValue.setWrappingWidth(52);

        volumeBar = new VolumeBar(280, 36);
        volumeBar.setValue(OptionsStore.getMusicVolume());
        volumeBar.setOnLive(this::applyVolumeLive);
        volumeBar.setOnCommit(volume -> {
            OptionsStore.setMusicVolume(volume);
            applyVolumeLive(volume);
        });
        volumeValue.setText(Math.round(volumeBar.getValue() * 100) + "%");

        HBox volumeControls = new HBox(14, volumeBar, volumeValue);
        volumeControls.setAlignment(Pos.CENTER_LEFT);
        VBox volumeBlock = new VBox(8, sectionLabel("Lautstärke"), volumeControls);

        trackName = new Text();
        trackName.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        trackName.setFill(Color.WHITE);

        prevTrack = cycleButton("<", -1);
        nextTrack = cycleButton(">", 1);

        HBox trackRow = new HBox(12, prevTrack, trackName, nextTrack);
        trackRow.setAlignment(Pos.CENTER);
        VBox trackBlock = new VBox(6, sectionLabel("Titel"), trackRow);
        trackBlock.setAlignment(Pos.CENTER);

        Text folderHint = new Text("Ordner: " + MusicLocations.displayPath());
        folderHint.setFont(Font.font("Monospace", 12));
        folderHint.setFill(Color.rgb(140, 180, 210));

        Button openFolder = styledButton("ORDNER ÖFFNEN");
        openFolder.setPrefWidth(220);
        openFolder.setOnAction(e -> {
            MusicLocations.revealUserLibrary();
            MusicHelper.rescan();
            refreshControls();
        });

        VBox folderBlock = new VBox(8, folderHint, openFolder);
        folderBlock.setAlignment(Pos.CENTER);

        Button back = styledButton("ZURÜCK");
        back.setPrefWidth(180);
        back.setOnAction(e -> onClose.run());

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 36, 20, 36));
        content.setPrefWidth(panelW);
        content.setTranslateX(panelX);
        content.setTranslateY(panelY);
        content.getChildren().addAll(title, musicRow, volumeBlock, trackBlock, folderBlock, back);

        root.getChildren().addAll(dimmer, panel, accent, content);
        MusicHelper.rescan();
        refreshControls();
    }

    public Group getRoot() {
        return root;
    }

    private void applyVolumeLive(double volume) {
        MusicHelper.setVolume(volume);
        volumeValue.setText(Math.round(volume * 100) + "%");
    }

    private void toggleMusic() {
        OptionsStore.setMusicEnabled(!OptionsStore.isMusicEnabled());
        MusicHelper.applyFromStore();
        refreshControls();
    }

    private void cycleTrack(int delta) {
        MusicHelper.rescan();
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

        volumeBar.setBarDisabled(!enabled);
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

    /**
     * Fat click-and-drag bar: large hit target, live volume, persist on release.
     */
    private static final class VolumeBar extends Group {
        private static final double TRACK_HEIGHT = 12;
        private static final double THUMB_RADIUS = 11;

        private final double width;
        private final double height;
        private final Rectangle fill;
        private final Circle thumb;
        private double value;
        private boolean disabled;
        private DoubleConsumer onLive = v -> {};
        private DoubleConsumer onCommit = v -> {};

        VolumeBar(double width, double height) {
            this.width = width;
            this.height = height;

            Rectangle hit = new Rectangle(width, height);
            hit.setFill(Color.rgb(8, 16, 28, 0.35));
            hit.setArcWidth(10);
            hit.setArcHeight(10);
            hit.setStroke(Color.rgb(74, 144, 217, 0.55));
            hit.setStrokeWidth(1);

            double trackY = (height - TRACK_HEIGHT) / 2.0;
            Rectangle track = new Rectangle(width - 16, TRACK_HEIGHT);
            track.setArcWidth(TRACK_HEIGHT);
            track.setArcHeight(TRACK_HEIGHT);
            track.setFill(Color.rgb(26, 48, 80));
            track.setTranslateX(8);
            track.setTranslateY(trackY);

            fill = new Rectangle(0, TRACK_HEIGHT);
            fill.setArcWidth(TRACK_HEIGHT);
            fill.setArcHeight(TRACK_HEIGHT);
            fill.setFill(Color.rgb(78, 205, 196));
            fill.setTranslateX(8);
            fill.setTranslateY(trackY);

            thumb = new Circle(THUMB_RADIUS);
            thumb.setFill(Color.rgb(78, 205, 196));
            thumb.setStroke(Color.rgb(159, 249, 242));
            thumb.setStrokeWidth(2);
            thumb.setTranslateY(height / 2.0);
            thumb.setMouseTransparent(true);
            fill.setMouseTransparent(true);
            track.setMouseTransparent(true);

            getChildren().addAll(hit, track, fill, thumb);
            setCursor(javafx.scene.Cursor.HAND);

            hit.setOnMousePressed(this::onPointer);
            hit.setOnMouseDragged(this::onPointer);
            hit.setOnMouseReleased(e -> {
                if (disabled) {
                    return;
                }
                applyFromEvent(e, false);
                onCommit.accept(value);
            });

            layoutValue();
        }

        void setOnLive(DoubleConsumer consumer) {
            this.onLive = consumer != null ? consumer : v -> {};
        }

        void setOnCommit(DoubleConsumer consumer) {
            this.onCommit = consumer != null ? consumer : v -> {};
        }

        void setValue(double volume) {
            this.value = clamp01(volume);
            layoutValue();
        }

        double getValue() {
            return value;
        }

        void setBarDisabled(boolean disabled) {
            this.disabled = disabled;
            setOpacity(disabled ? 0.4 : 1.0);
            setCursor(disabled ? javafx.scene.Cursor.DEFAULT : javafx.scene.Cursor.HAND);
        }

        private void onPointer(MouseEvent event) {
            if (disabled) {
                return;
            }
            applyFromEvent(event, true);
        }

        private void applyFromEvent(MouseEvent event, boolean live) {
            double inner = width - 16;
            double x = Math.max(0, Math.min(inner, event.getX() - 8));
            value = inner <= 0 ? 0 : x / inner;
            layoutValue();
            if (live) {
                onLive.accept(value);
            }
        }

        private void layoutValue() {
            double inner = width - 16;
            fill.setWidth(Math.max(TRACK_HEIGHT, inner * value));
            thumb.setTranslateX(8 + inner * value);
        }

        private static double clamp01(double volume) {
            return Math.max(0.0, Math.min(1.0, volume));
        }
    }
}
