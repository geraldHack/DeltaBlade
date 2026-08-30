package deltablade.minigames;

import deltablade.GameVars;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.geti;
import static com.almasb.fxgl.dsl.FXGL.inc;

public class CognitiveTestGame implements Minigame {

    private static final String[] MOTIFS = {
            "SHIP", "E", "X", "T", "R", "A",
            "CW", "CG", "CB", "CV",
            "WPN", "AMMO", "AUTO", "LIFE",
            "STAR", "BOSS", "FAST", "WAVE"
    };

    private static final Color[] MOTIF_COLORS = {
            Color.CYAN, Color.rgb(255, 80, 80), Color.rgb(80, 255, 80),
            Color.rgb(80, 180, 255), Color.rgb(255, 180, 80), Color.rgb(200, 80, 255),
            Color.rgb(240, 240, 240), Color.rgb(100, 220, 100), Color.rgb(100, 150, 255),
            Color.rgb(200, 100, 255), Color.GOLD, Color.CYAN,
            Color.AQUA, Color.LIMEGREEN, Color.YELLOW,
            Color.MEDIUMPURPLE, Color.LIME, Color.GOLD
    };

    private MinigameHost host;
    private boolean active;
    private boolean finished;
    private boolean inputLocked;
    private boolean awaitingCashout;
    private double cashoutTimer;
    private double timeLeft;
    private double lockTimer;
    private double cursorRepeat;
    private int cols;
    private int rows;
    private int selected;
    private int firstPick = -1;
    private int matchedPairs;
    private int totalPairs;
    private final List<Card> cards = new ArrayList<>();
    private final List<javafx.scene.Node> ui = new ArrayList<>();
    private Text timerText;
    private Text hintText;

    private static final class Card {
        final int motif;
        final boolean free;
        boolean faceUp;
        boolean matched;
        StackPane node;
        Rectangle plate;
        Group face;
        Group back;

        Card(int motif) {
            this.motif = motif;
            this.free = motif < 0;
            this.matched = this.free;
            this.faceUp = this.free;
        }
    }

    @Override
    public void start(MinigameHost host) {
        this.host = host;
        this.active = true;
        this.finished = false;
        host.hidePlayer(true);
        host.playMusicOverride("cues/hektischer_countdown.mp3");

        int wins = geti(GameVars.COGNITIVE_WINS);
        if (wins < 2) {
            cols = 4;
            rows = 4;
        } else if (wins < 4) {
            cols = 5;
            rows = 5;
        } else {
            cols = 6;
            rows = 6;
        }

        timeLeft = 30 + (cols - 4) * 7;
        buildGrid();
        host.displayBanner("COGNITIVE TEST", Color.CYAN, 1.6);
    }

    @Override
    public boolean usesPlayerShip() {
        return false;
    }

    @Override
    public void update(double tpf) {
        if (!active || finished) {
            return;
        }

        if (awaitingCashout) {
            cashoutTimer -= tpf;
            if (cashoutTimer <= 0) {
                cashOut();
            }
            return;
        }

        if (inputLocked) {
            lockTimer -= tpf;
            if (lockTimer <= 0) {
                inputLocked = false;
                hideMismatched();
            }
        }

        moveCursor(tpf);

        timeLeft -= tpf;
        if (timerText != null) {
            timerText.setText(String.format("ZEIT %04.1f", Math.max(0, timeLeft)));
            timerText.setFill(timeLeft < 8 ? Color.ORANGERED : Color.CYAN);
        }

        if (timeLeft <= 0) {
            fail();
        }
    }

    @Override
    public void onFirePress() {
        if (!active || finished || inputLocked || awaitingCashout) {
            return;
        }
        flip(selected);
    }

    @Override
    public void onBonusKey() {
        if (awaitingCashout) {
            cashOut();
        }
    }

    @Override
    public void cleanup() {
        active = false;
        host.hidePlayer(false);
        host.restoreMusic();
        for (javafx.scene.Node node : ui) {
            host.removeUi(node);
        }
        ui.clear();
        cards.clear();
    }

    private void moveCursor(double tpf) {
        cursorRepeat -= tpf;
        int dx = 0;
        int dy = 0;
        if (host.movingLeft()) {
            dx = -1;
        } else if (host.movingRight()) {
            dx = 1;
        }
        if (host.movingUp()) {
            dy = -1;
        } else if (host.movingDown()) {
            dy = 1;
        }
        if ((dx != 0 || dy != 0) && cursorRepeat <= 0) {
            int col = selected % cols;
            int row = selected / cols;
            col = Math.floorMod(col + dx, cols);
            row = Math.floorMod(row + dy, rows);
            select(row * cols + col);
            cursorRepeat = 0.16;
        }
        if (dx == 0 && dy == 0) {
            cursorRepeat = 0;
        }
    }

    private void buildGrid() {
        int cells = cols * rows;
        boolean hasFree = cells % 2 != 0;
        totalPairs = cells / 2;

        List<Integer> deck = new ArrayList<>();
        for (int i = 0; i < totalPairs; i++) {
            int motif = i % MOTIFS.length;
            deck.add(motif);
            deck.add(motif);
        }
        Collections.shuffle(deck);
        if (hasFree) {
            int mid = cells / 2;
            deck.add(mid, -1);
        }

        double left = host.playLeft() + 14;
        double top = 78;
        double areaW = host.playRight() - host.playLeft() - 28;
        double areaH = host.playBottom() - top - 36;
        double gap = 6;
        double cellW = (areaW - (cols + 1) * gap) / cols;
        double cellH = (areaH - (rows + 1) * gap) / rows;

        for (int i = 0; i < cells; i++) {
            int motif = deck.get(i);
            Card card = new Card(motif);
            card.node = createCardNode(card, cellW, cellH);
            int col = i % cols;
            int row = i / cols;
            card.node.setTranslateX(left + gap + col * (cellW + gap));
            card.node.setTranslateY(top + gap + row * (cellH + gap));
            final int index = i;
            card.node.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    select(index);
                    flip(index);
                }
            });
            host.addUi(card.node);
            ui.add(card.node);
            cards.add(card);
            if (card.free) {
                showFace(card, false);
            }
        }

        timerText = new Text(String.format("ZEIT %04.1f", timeLeft));
        timerText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        timerText.setFill(Color.CYAN);
        timerText.setEffect(new DropShadow(10, Color.CYAN));
        timerText.setTranslateX(host.playLeft() + 16);
        timerText.setTranslateY(68);
        host.addUi(timerText);
        ui.add(timerText);

        hintText = new Text("KLICK / FEUER = WENDEN");
        hintText.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        hintText.setFill(Color.rgb(140, 190, 210));
        hintText.setTranslateX(host.playRight() - 168);
        hintText.setTranslateY(68);
        host.addUi(hintText);
        ui.add(hintText);

        selected = 0;
        refreshSelection();
    }

    private StackPane createCardNode(Card card, double w, double h) {
        card.plate = new Rectangle(w, h);
        card.plate.setArcWidth(8);
        card.plate.setArcHeight(8);
        card.plate.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(18, 32, 52)),
                new Stop(1, Color.rgb(8, 14, 26))));
        card.plate.setStroke(Color.rgb(60, 140, 180, 0.7));
        card.plate.setStrokeWidth(1.4);

        card.back = new Group();
        Rectangle backFill = new Rectangle(w - 10, h - 10);
        backFill.setArcWidth(6);
        backFill.setArcHeight(6);
        backFill.setTranslateX(5);
        backFill.setTranslateY(5);
        backFill.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(20, 70, 90)),
                new Stop(1, Color.rgb(10, 24, 40))));
        Text mark = new Text("?");
        mark.setFont(Font.font("Monospace", FontWeight.BOLD, Math.min(22, h * 0.4)));
        mark.setFill(Color.CYAN);
        mark.setTranslateX(w / 2 - 7);
        mark.setTranslateY(h / 2 + 7);
        card.back.getChildren().addAll(backFill, mark);

        card.face = motifView(card.motif, w, h);
        card.face.setVisible(false);

        StackPane pane = new StackPane(card.plate, card.back, card.face);
        pane.setPrefSize(w, h);
        pane.setAlignment(Pos.CENTER);
        pane.setCursor(javafx.scene.Cursor.HAND);
        return pane;
    }

    private Group motifView(int motif, double w, double h) {
        Group g = new Group();
        if (motif < 0) {
            Text free = new Text("FREE");
            free.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
            free.setFill(Color.GOLD);
            free.setTranslateX(w / 2 - 16);
            free.setTranslateY(h / 2 + 4);
            g.getChildren().add(free);
            return g;
        }

        Color color = MOTIF_COLORS[motif % MOTIF_COLORS.length];
        String label = MOTIFS[motif % MOTIFS.length];

        if ("SHIP".equals(label)) {
            Polygon ship = new Polygon(
                    w / 2, 8,
                    w - 10, h - 10,
                    w / 2, h - 18,
                    10, h - 10
            );
            ship.setFill(color);
            g.getChildren().add(ship);
        } else if (label.startsWith("C")) {
            Circle coin = new Circle(Math.min(w, h) * 0.22);
            coin.setFill(new RadialGradient(0, 0, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
                    new Stop(0, color.brighter()),
                    new Stop(1, color.darker())));
            coin.setCenterX(w / 2);
            coin.setCenterY(h / 2);
            g.getChildren().add(coin);
        } else if ("STAR".equals(label) || "LIFE".equals(label)) {
            Text symbol = new Text("LIFE".equals(label) ? "\u2665" : "\u2605");
            symbol.setFont(Font.font("Monospace", FontWeight.BOLD, Math.min(26, h * 0.45)));
            symbol.setFill(color);
            symbol.setTranslateX(w / 2 - 10);
            symbol.setTranslateY(h / 2 + 8);
            g.getChildren().add(symbol);
        } else {
            Text text = new Text(switch (label) {
                case "WPN" -> "W";
                case "AMMO" -> "B";
                case "AUTO" -> "A";
                case "BOSS" -> "B";
                case "FAST" -> "F";
                case "WAVE" -> "#";
                default -> label;
            });
            text.setFont(Font.font("Monospace", FontWeight.BOLD, Math.min(24, h * 0.42)));
            text.setFill(color);
            text.setEffect(new Glow(0.4));
            text.setTranslateX(w / 2 - 7);
            text.setTranslateY(h / 2 + 8);
            g.getChildren().add(text);
        }
        return g;
    }

    private void select(int index) {
        if (index < 0 || index >= cards.size()) {
            return;
        }
        selected = index;
        refreshSelection();
    }

    private void refreshSelection() {
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (i == selected) {
                card.plate.setStroke(Color.GOLD);
                card.plate.setStrokeWidth(2.4);
            } else if (card.matched) {
                card.plate.setStroke(Color.rgb(80, 220, 140, 0.55));
                card.plate.setStrokeWidth(1.2);
            } else {
                card.plate.setStroke(Color.rgb(60, 140, 180, 0.7));
                card.plate.setStrokeWidth(1.4);
            }
        }
    }

    private void flip(int index) {
        if (inputLocked || finished || awaitingCashout) {
            return;
        }
        Card card = cards.get(index);
        if (card.matched || card.faceUp) {
            return;
        }

        showFace(card, true);
        if (firstPick < 0) {
            firstPick = index;
            return;
        }

        Card other = cards.get(firstPick);
        if (other.motif == card.motif) {
            other.matched = true;
            card.matched = true;
            matchedPairs++;
            firstPick = -1;
            refreshSelection();
            if (matchedPairs >= totalPairs) {
                readyCashout();
            }
        } else {
            inputLocked = true;
            lockTimer = 0.55;
        }
    }

    private void showFace(Card card, boolean animate) {
        card.faceUp = true;
        card.back.setVisible(false);
        card.face.setVisible(true);
        if (animate) {
            ScaleTransition st = new ScaleTransition(Duration.millis(90), card.node);
            st.setFromX(0.15);
            st.setToX(1);
            st.play();
        }
    }

    private void hideMismatched() {
        if (firstPick >= 0) {
            hideFace(cards.get(firstPick));
        }
        for (Card card : cards) {
            if (!card.matched && card.faceUp) {
                hideFace(card);
            }
        }
        firstPick = -1;
    }

    private void hideFace(Card card) {
        if (card.free || card.matched) {
            return;
        }
        card.faceUp = false;
        card.face.setVisible(false);
        card.back.setVisible(true);
    }

    private void readyCashout() {
        awaitingCashout = true;
        cashoutTimer = 2.6;
        if (hintText != null) {
            hintText.setText("B / RECHTSKLICK = ZEITBONUS");
            hintText.setFill(Color.GOLD);
        }
        host.displayBanner("CLEAR  B = BONUS", Color.CYAN, 2.0);
    }

    private void cashOut() {
        if (finished) {
            return;
        }
        finished = true;
        int leftover = Math.max(0, (int) Math.ceil(timeLeft));
        int score = 450 + leftover * 35 + geti(GameVars.COGNITIVE_WINS) * 60;
        int money = 35 + leftover * 3;
        inc(GameVars.COGNITIVE_WINS, 1);
        host.finishMinigame(true, score, money, "COGNITIVE CLEAR  +" + score, Color.CYAN);
    }

    private void fail() {
        finished = true;
        host.finishMinigame(false, 40, 5, "TEST FAIL  +40", Color.ORANGERED);
    }
}
