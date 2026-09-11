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

import java.util.Arrays;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

/**
 * Arcade hiscore table and name entry (1–10 letters).
 */
public final class HighScoreOverlay {

    private static final char EMPTY = '\0';
    private static final char SPACE = ' ';
    private static final double ROTATE_SECONDS = 3.5;

    @FunctionalInterface
    public interface SavedHandler {
        void accept(String name, int score, int wave, int localRank);
    }

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
    private final SavedHandler onSaved;
    private final Runnable onClose;

    private char[] letters;
    private int cursor;
    private Text[] letterTexts;
    private Timeline blink;
    private Timeline rotate;
    private boolean blinkOn = true;
    private boolean finished;
    private boolean confirmArmed;
    private boolean showingGlobal;
    private List<HighScoreStore.Entry> localEntries = List.of();
    private List<HighScoreStore.Entry> globalEntries;
    private Integer localHighlight;
    private Integer globalHighlight;
    private Text headingText;
    private Text[] rowTexts;

    public static HighScoreOverlay table(Integer localHighlight, Integer globalHighlight,
                                         String closeLabel, Runnable onClose) {
        return new HighScoreOverlay(false, 0, 0, localHighlight, globalHighlight, closeLabel, null, onClose);
    }

    public static HighScoreOverlay nameEntry(int score, int wave, SavedHandler onSaved) {
        return new HighScoreOverlay(true, score, wave, null, null, null, onSaved, null);
    }

    private HighScoreOverlay(boolean nameEntry, int score, int wave, Integer localHighlight,
                             Integer globalHighlight, String closeLabel, SavedHandler onSaved, Runnable onClose) {
        this.nameEntry = nameEntry;
        this.pendingScore = score;
        this.pendingWave = wave;
        this.onSaved = onSaved;
        this.onClose = onClose;
        this.localHighlight = localHighlight;
        this.globalHighlight = globalHighlight;
        this.localEntries = HighScoreStore.entries();
        this.globalEntries = HighScoreClient.hasCache() ? HighScoreClient.cachedEntries() : null;
        this.letters = new char[HighScoreStore.NAME_LENGTH];
        Arrays.fill(this.letters, EMPTY);
        String previous = HighScoreStore.lastName();
        for (int i = 0; i < previous.length() && i < this.letters.length; i++) {
            this.letters[i] = previous.charAt(i);
        }
        this.cursor = 0;

        double width = getAppWidth();
        double height = getAppHeight();

        Rectangle dimmer = new Rectangle(width, height);
        dimmer.setFill(Color.rgb(0, 0, 0, 0.78));

        double panelW = 520;
        double panelH = nameEntry ? 360 : 470;
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
                    muted("Tippen  Leertaste = Leerzeichen    ENTER fertig    X weiter")
            );
            startBlink();
            Timeline arm = new Timeline(new KeyFrame(Duration.millis(350), e -> confirmArmed = true));
            arm.play();
        } else {
            headingText = heading(currentTableTitle());
            content.getChildren().add(headingText);
            content.getChildren().add(buildTable());
            startRotate();
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
            if (code == KeyCode.LEFT) {
                cursor = Math.max(0, cursor - 1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.RIGHT) {
                cursor = Math.min(HighScoreStore.NAME_LENGTH - 1, cursor + 1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.UP) {
                letters[cursor] = nextLetter(letters[cursor], 1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.DOWN) {
                letters[cursor] = nextLetter(letters[cursor], -1);
                refreshLetters();
                return true;
            }
            if (code == KeyCode.BACK_SPACE) {
                if (letters[cursor] != EMPTY) {
                    letters[cursor] = EMPTY;
                } else {
                    cursor = Math.max(0, cursor - 1);
                    letters[cursor] = EMPTY;
                }
                refreshLetters();
                return true;
            }
            if (code == KeyCode.ESCAPE || code == KeyCode.ENTER) {
                if (!confirmArmed) {
                    return true;
                }
                confirm();
                return true;
            }
            if (code == KeyCode.SPACE) {
                letters[cursor] = SPACE;
                if (cursor < HighScoreStore.NAME_LENGTH - 1) {
                    cursor++;
                }
                refreshLetters();
                return true;
            }
            if (code == KeyCode.X) {
                if (!confirmArmed) {
                    return true;
                }
                if (letters[cursor] == EMPTY || cursor >= HighScoreStore.NAME_LENGTH - 1) {
                    confirm();
                } else {
                    cursor++;
                    refreshLetters();
                }
                return true;
            }
            char typed = typedLetter(event);
            if (typed != 0) {
                letters[cursor] = typed;
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
        if (rotate != null) {
            rotate.stop();
            rotate = null;
        }
    }

    public void setGlobalEntries(List<HighScoreStore.Entry> entries) {
        setGlobalEntries(entries, null);
    }

    public void setGlobalEntries(List<HighScoreStore.Entry> entries, Integer highlight) {
        this.globalEntries = entries;
        if (highlight != null) {
            this.globalHighlight = highlight;
        }
        if (entries != null && !nameEntry) {
            refreshTable();
            startRotate();
        }
    }

    private String currentName() {
        StringBuilder name = new StringBuilder();
        for (char letter : letters) {
            if (letter != EMPTY) {
                name.append(letter);
            }
        }
        return name.toString();
    }

    private void confirm() {
        if (finished) {
            return;
        }
        finished = true;
        dispose();
        String name = currentName();
        int rank = HighScoreStore.insert(name, pendingScore, pendingWave);
        if (onSaved != null) {
            onSaved.accept(name, pendingScore, pendingWave, rank);
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

    private VBox buildTable() {
        VBox table = new VBox(5);
        table.setAlignment(Pos.CENTER);
        rowTexts = new Text[HighScoreStore.MAX_ENTRIES];
        for (int i = 0; i < HighScoreStore.MAX_ENTRIES; i++) {
            Text row = new Text();
            row.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
            rowTexts[i] = row;
            table.getChildren().add(row);
        }
        refreshTable();
        return table;
    }

    private void startRotate() {
        if (nameEntry || globalEntries == null || rotate != null) {
            return;
        }
        rotate = new Timeline(new KeyFrame(Duration.seconds(ROTATE_SECONDS), e -> {
            showingGlobal = !showingGlobal;
            refreshTable();
        }));
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();
    }

    private void refreshTable() {
        if (rowTexts == null) {
            return;
        }
        if (showingGlobal && globalEntries == null) {
            showingGlobal = false;
        }
        if (headingText != null) {
            headingText.setText(currentTableTitle());
        }
        List<HighScoreStore.Entry> entries = showingGlobal ? globalEntries : localEntries;
        if (entries == null) {
            entries = List.of();
        }
        Integer highlight = showingGlobal ? globalHighlight : localHighlight;
        for (int i = 0; i < rowTexts.length; i++) {
            boolean hasRow = i < entries.size();
            boolean lit = highlight != null && highlight == i;
            String line;
            if (hasRow) {
                HighScoreStore.Entry entry = entries.get(i);
                line = String.format("%2d  %-10s  %s  W%02d",
                        i + 1, entry.name(), HighScoreStore.formatScore(entry.score()), entry.wave());
            } else {
                line = String.format("%2d  %-10s  000000  W--", i + 1, "----------");
            }
            rowTexts[i].setText(line);
            if (lit) {
                rowTexts[i].setFill(Color.CYAN);
                rowTexts[i].setEffect(new Glow(0.45));
            } else if (hasRow) {
                rowTexts[i].setFill(Color.WHITE);
                rowTexts[i].setEffect(null);
            } else {
                rowTexts[i].setFill(Color.rgb(90, 110, 130));
                rowTexts[i].setEffect(null);
            }
        }
    }

    private String currentTableTitle() {
        if (showingGlobal && globalEntries != null) {
            return "HISCORE GLOBAL";
        }
        return "HISCORE LOKAL";
    }

    private HBox buildNameRow() {
        letterTexts = new Text[HighScoreStore.NAME_LENGTH];
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);
        for (int i = 0; i < HighScoreStore.NAME_LENGTH; i++) {
            Text letter = new Text("_");
            letter.setFont(Font.font("Monospace", FontWeight.BOLD, 26));
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
            char letter = letters[i];
            boolean empty = letter == EMPTY;
            boolean space = letter == SPACE;
            letterTexts[i].setText(empty ? "_" : space ? "·" : String.valueOf(letter));
            boolean selected = i == cursor;
            if (selected) {
                letterTexts[i].setFill(Color.CYAN);
            } else if (empty) {
                letterTexts[i].setFill(Color.rgb(70, 95, 120));
            } else if (space) {
                letterTexts[i].setFill(Color.rgb(180, 220, 255));
            } else {
                letterTexts[i].setFill(Color.WHITE);
            }
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

    private static char typedLetter(KeyEvent event) {
        String text = event.getText();
        if (text != null && !text.isEmpty()) {
            char c = Character.toUpperCase(text.charAt(0));
            if (c >= 'A' && c <= 'Z') {
                return c;
            }
        }
        KeyCode code = event.getCode();
        if (code != null && code.isLetterKey()) {
            String name = code.getName();
            if (name != null && name.length() == 1) {
                char c = Character.toUpperCase(name.charAt(0));
                if (c >= 'A' && c <= 'Z') {
                    return c;
                }
            }
        }
        return 0;
    }

    private static char nextLetter(char current, int delta) {
        if (current < 'A' || current > 'Z') {
            return 'A';
        }
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
