package deltablade;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

import java.util.List;
import java.util.function.IntConsumer;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

/**
 * Arcade hiscore table and 3-letter name entry.
 */
public final class HighScoreOverlay {

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

    private final Group root = new Group();
    private final boolean nameEntry;
    private final int pendingScore;
    private final int pendingWave;
    private final IntConsumer onSaved;
    private final Runnable onClose;

    private char[] letters;
    private int cursor;
    private Text[] letterTexts;
    private Timeline blink;
    private boolean blinkOn = true;
    private boolean finished;

    public static HighScoreOverlay table(Integer highlightRank, String closeLabel, Runnable onClose) {
        return new HighScoreOverlay(false, 0, 0, highlightRank, closeLabel, null, onClose);
    }

    public static HighScoreOverlay nameEntry(int score, int wave, IntConsumer onSaved) {
        return new HighScoreOverlay(true, score, wave, null, null, onSaved, null);
    }

    private HighScoreOverlay(boolean nameEntry, int score, int wave, Integer highlightRank,
                             String closeLabel, IntConsumer onSaved, Runnable onClose) {
        this.nameEntry = nameEntry;
        this.pendingScore = score;
        this.pendingWave = wave;
        this.onSaved = onSaved;
        this.onClose = onClose;
        this.letters = HighScoreStore.lastName().toCharArray();
        this.cursor = HighScoreStore.NAME_LENGTH - 1;

        double width = getAppWidth();
        double height = getAppHeight();

        Rectangle dimmer = new Rectangle(width, height);
        dimmer.setFill(Color.rgb(0, 0, 0, 0.78));

        double panelW = 460;
        double panelH = nameEntry ? 340 : 470;
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

        VBox content = new VBox(nameEntry ? 16 : 10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 28, 20, 28));
        content.setPrefWidth(panelW);
        content.setTranslateX(panelX);
        content.setTranslateY(panelY);

        if (nameEntry) {
            content.getChildren().addAll(
                    heading("NEW HISCORE"),
                    muted(HighScoreStore.formatScore(score) + "   WAVE " + String.format("%02d", wave)),
                    buildNameRow(),
                    muted("← → Buchstabe   ↑↓ Zeichen   FEUER OK")
            );
            startBlink();
        } else {
            content.getChildren().add(heading("HISCORE"));
            content.getChildren().add(buildTable(highlightRank));
            if (onClose != null) {
                Button back = styledButton(closeLabel != null ? closeLabel : "ZURÜCK");
                back.setPrefWidth(180);
                back.setOnAction(e -> close());
                content.getChildren().add(back);
            } else {
                content.getChildren().add(muted(closeLabel != null ? closeLabel : "R = Neustart"));
            }
        }

        root.getChildren().addAll(dimmer, panel, accent, content);
    }

    public Group getRoot() {
        return root;
    }

    public boolean isNameEntry() {
        return nameEntry && !finished;
    }

    public boolean handleKey(KeyEvent event) {
        if (finished) {
            return false;
        }
        KeyCode code = event.getCode();
        if (nameEntry) {
            if (code == KeyCode.LEFT || code == KeyCode.A) {
                cursor = Math.max(0, cursor - 1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.RIGHT || code == KeyCode.D) {
                cursor = Math.min(HighScoreStore.NAME_LENGTH - 1, cursor + 1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.UP || code == KeyCode.W) {
                letters[cursor] = nextLetter(letters[cursor], 1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.DOWN || code == KeyCode.S) {
                letters[cursor] = nextLetter(letters[cursor], -1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.BACK_SPACE) {
                letters[cursor] = 'A';
                cursor = Math.max(0, cursor - 1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.ESCAPE) {
                confirm();
                return true;
            }
            if (code == KeyCode.ENTER || code == KeyCode.SPACE || code == KeyCode.X) {
                if (cursor < HighScoreStore.NAME_LENGTH - 1) {
                    cursor++;
                    refreshLetters();
                } else {
                    confirm();
                }
                return true;
            }
            if (code.isLetterKey()) {
                letters[cursor] = code.getName().charAt(0);
                if (cursor < HighScoreStore.NAME_LENGTH - 1) {
                    cursor++;
                }
                refreshLetters();
                return true;
            }
            return true;
        }
        if (onClose != null && (code == KeyCode.ESCAPE || code == KeyCode.ENTER)) {
            close();
            return true;
        }
        return false;
    }

    public void dispose() {
        if (blink != null) {
            blink.stop();
            blink = null;
        }
    }

    private void confirm() {
        if (finished) {
            return;
        }
        finished = true;
        dispose();
        int rank = HighScoreStore.insert(new String(letters), pendingScore, pendingWave);
        if (onSaved != null) {
            onSaved.accept(rank);
        }
    }

    private void close() {
        if (finished) {
            return;
        }
        finished = true;
        dispose();
        if (onClose != null) {
            onClose.run();
        }
    }

    private VBox buildTable(Integer highlightRank) {
        VBox table = new VBox(5);
        table.setAlignment(Pos.CENTER);
        List<HighScoreStore.Entry> entries = HighScoreStore.entries();
        for (int i = 0; i < HighScoreStore.MAX_ENTRIES; i++) {
            boolean highlight = highlightRank != null && highlightRank == i;
            String line;
            if (i < entries.size()) {
                HighScoreStore.Entry entry = entries.get(i);
                line = String.format("%2d  %s  %s  W%02d",
                        i + 1, entry.name(), HighScoreStore.formatScore(entry.score()), entry.wave());
            } else {
                line = String.format("%2d  ---  000000  W--", i + 1);
            }
            Text row = new Text(line);
            row.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
            if (highlight) {
                row.setFill(Color.CYAN);
                row.setEffect(new Glow(0.45));
            } else if (i < entries.size()) {
                row.setFill(Color.WHITE);
            } else {
                row.setFill(Color.rgb(90, 110, 130));
            }
            table.getChildren().add(row);
        }
        return table;
    }

    private HBox buildNameRow() {
        letterTexts = new Text[HighScoreStore.NAME_LENGTH];
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER);
        for (int i = 0; i < HighScoreStore.NAME_LENGTH; i++) {
            Text letter = new Text(String.valueOf(letters[i]));
            letter.setFont(Font.font("Monospace", FontWeight.BOLD, 42));
            letter.setFill(Color.CYAN);
            letterTexts[i] = letter;
            row.getChildren().add(letter);
        }
        refreshLetters();
        return row;
    }

    private void refreshLetters() {
        if (letterTexts == null) {
            return;
        }
        for (int i = 0; i < letterTexts.length; i++) {
            letterTexts[i].setText(String.valueOf(letters[i]));
            boolean selected = i == cursor;
            letterTexts[i].setFill(selected ? Color.CYAN : Color.WHITE);
            letterTexts[i].setOpacity(selected && !blinkOn ? 0.25 : 1.0);
            letterTexts[i].setEffect(selected ? new Glow(0.5) : null);
        }
    }

    private void startBlink() {
        blink = new Timeline(new KeyFrame(Duration.millis(380), e -> {
            blinkOn = !blinkOn;
            refreshLetters();
        }));
        blink.setCycleCount(Animation.INDEFINITE);
        blink.play();
    }

    private static char nextLetter(char current, int delta) {
        int index = current - 'A';
        index = Math.floorMod(index + delta, 26);
        return (char) ('A' + index);
    }

    private static Text heading(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        text.setFill(Color.CYAN);
        text.setEffect(new Glow(0.4));
        return text;
    }

    private static Text muted(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("Monospace", 13));
        text.setFill(Color.rgb(160, 210, 240));
        return text;
    }

    private static Button styledButton(String label) {
        Button button = new Button(label);
        button.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        button.setStyle(BUTTON_BASE);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_HOVER));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_BASE));
        return button;
    }
}
