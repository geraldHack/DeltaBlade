package deltablade.minigames;

import com.almasb.fxgl.entity.Entity;
import deltablade.EntityType;
import deltablade.GameVars;
import deltablade.components.MeteorRockComponent;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.inc;
import static com.almasb.fxgl.dsl.FXGL.spawn;

public class MeteorStormGame implements Minigame {

    private static final double GOAL_DISTANCE = 420;
    private static final Random RANDOM = new Random();

    private MinigameHost host;
    private boolean active;
    private boolean finished;
    private double distance;
    private double spawnTimer;
    private double thrustSum;
    private double thrustTime;
    private final List<NodeHud> hud = new ArrayList<>();
    private Rectangle distanceFill;
    private Text speedText;

    private record NodeHud(javafx.scene.Node node) {}

    @Override
    public void start(MinigameHost host) {
        this.host = host;
        this.active = true;
        this.finished = false;
        this.distance = 0;
        this.spawnTimer = 0.4;
        this.thrustSum = 0;
        this.thrustTime = 0;
        host.hidePlayer(false);
        buildHud();
        host.displayBanner("METEOR STORM", Color.ORANGERED, 1.6);
    }

    @Override
    public boolean usesPlayerShip() {
        return true;
    }

    @Override
    public void update(double tpf) {
        if (!active || finished) {
            return;
        }

        double thrust = host.holdingFire() ? 2.2 : 0.9;
        thrustSum += thrust * tpf;
        thrustTime += tpf;
        distance += 14 * thrust * tpf;

        double progress = Math.min(1.0, distance / GOAL_DISTANCE);
        if (distanceFill != null) {
            distanceFill.setHeight(Math.max(4, 220 * progress));
            distanceFill.setTranslateY(hudBarBottom() - distanceFill.getHeight());
        }
        if (speedText != null) {
            speedText.setText(host.holdingFire() ? "SCHUB" : "GLIDE");
            speedText.setFill(host.holdingFire() ? Color.ORANGE : Color.rgb(160, 180, 200));
        }

        for (Entity rock : getGameWorld().getEntitiesByType(EntityType.MINIGAME_HAZARD)) {
            if (rock.isActive() && rock.hasComponent(MeteorRockComponent.class)) {
                rock.getComponent(MeteorRockComponent.class).setSpeedScale(thrust);
            }
        }

        spawnTimer -= tpf;
        if (spawnTimer <= 0) {
            spawnWave(progress, thrust);
            spawnTimer = Math.max(0.26, 0.68 - progress * 0.36);
        }

        if (playerHitRock()) {
            fail();
            return;
        }

        if (distance >= GOAL_DISTANCE) {
            win();
        }
    }

    @Override
    public void cleanup() {
        active = false;
        for (Entity rock : List.copyOf(getGameWorld().getEntitiesByType(EntityType.MINIGAME_HAZARD))) {
            if (rock.isActive()) {
                rock.removeFromWorld();
            }
        }
        for (NodeHud item : hud) {
            host.removeUi(item.node());
        }
        hud.clear();
    }

    private void spawnWave(double progress, double thrust) {
        int count = 1 + (progress > 0.35 ? 1 : 0) + (RANDOM.nextDouble() < progress * 0.45 ? 1 : 0);
        double left = host.playLeft() + 8;
        double right = host.playRight() - 40;

        for (int i = 0; i < count; i++) {
            double size = 28 + RANDOM.nextDouble() * 28;
            double x = left + RANDOM.nextDouble() * Math.max(20, right - left);
            double y = -size - RANDOM.nextDouble() * 40;
            double baseSpeed = 90 + progress * 110 + RANDOM.nextDouble() * 40;
            spawn("meteorRock", new com.almasb.fxgl.entity.SpawnData(x, y)
                    .put("size", size)
                    .put("baseSpeed", baseSpeed)
                    .put("driftX", (RANDOM.nextDouble() - 0.5) * 40)
                    .put("spin", (RANDOM.nextDouble() - 0.5) * 180)
                    .put("tint", RANDOM.nextInt(3)));
        }

        if (RANDOM.nextDouble() < 0.22 + progress * 0.1) {
            String type = RANDOM.nextDouble() < 0.7 ? "white" : (RANDOM.nextDouble() < 0.7 ? "green" : "blue");
            double cx = left + 16 + RANDOM.nextDouble() * Math.max(20, right - left - 16);
            host.spawnCoinAt(cx, -12, type);
        }
    }

    private boolean playerHitRock() {
        Entity player = host.player();
        if (player == null || !player.isActive()) {
            return false;
        }
        double px = player.getX() + 8;
        double py = player.getY() + 8;
        double pw = Math.max(8, player.getWidth() - 16);
        double ph = Math.max(8, player.getHeight() - 16);

        for (Entity rock : getGameWorld().getEntitiesByType(EntityType.MINIGAME_HAZARD)) {
            if (!rock.isActive()) {
                continue;
            }
            double inset = Math.min(10, rock.getWidth() * 0.18);
            if (rectsOverlap(px, py, pw, ph,
                    rock.getX() + inset, rock.getY() + inset,
                    rock.getWidth() - inset * 2, rock.getHeight() - inset * 2)) {
                host.spawnExplosion(player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, "ship");
                return true;
            }
        }
        return false;
    }

    private static boolean rectsOverlap(double ax, double ay, double aw, double ah,
                                        double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void win() {
        finished = true;
        double avg = thrustTime > 0 ? thrustSum / thrustTime : 0.9;
        int score = 600 + (int) (avg * 220);
        int money = 40 + (int) (avg * 28);
        String banner = "STORM CLEAR  +" + score;
        if (avg >= 1.8) {
            score += 350;
            money += 30;
            banner = "SUPER STORM  +" + score;
        }
        inc(GameVars.METEOR_WINS, 1);
        host.finishMinigame(true, score, money, banner, Color.ORANGE);
    }

    private void fail() {
        finished = true;
        int score = 80;
        host.finishMinigame(false, score, 10, "CRASH  +" + score, Color.ORANGERED);
    }

    private void buildHud() {
        double x = host.playRight() - 22;
        double top = 70;
        double h = 220;

        Rectangle bg = new Rectangle(12, h);
        bg.setFill(Color.rgb(10, 16, 28, 0.85));
        bg.setStroke(Color.rgb(255, 140, 40, 0.7));
        bg.setTranslateX(x);
        bg.setTranslateY(top);

        distanceFill = new Rectangle(8, 4);
        distanceFill.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.ORANGERED),
                new Stop(1, Color.GOLD)));
        distanceFill.setTranslateX(x + 2);
        distanceFill.setTranslateY(top + h - 4);

        Text caption = new Text("DIST");
        caption.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        caption.setFill(Color.rgb(255, 180, 80));
        caption.setTranslateX(x - 6);
        caption.setTranslateY(top - 6);

        speedText = new Text("GLIDE");
        speedText.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        speedText.setFill(Color.rgb(160, 180, 200));
        speedText.setEffect(new DropShadow(6, Color.BLACK));
        speedText.setTranslateX(host.playLeft() + 12);
        speedText.setTranslateY(host.playBottom() - 18);

        Group group = new Group(bg, distanceFill, caption, speedText);
        host.addUi(group);
        hud.add(new NodeHud(group));
    }

    private double hudBarBottom() {
        return 70 + 220;
    }
}
