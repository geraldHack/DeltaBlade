package deltablade.minigames;

import com.almasb.fxgl.entity.Entity;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public interface MinigameHost {

    void finishMinigame(boolean won, int scoreBonus, int moneyBonus, String banner, Color color);

    void displayBanner(String message, Color color, double seconds);

    Entity player();

    boolean holdingFire();

    boolean movingLeft();

    boolean movingRight();

    boolean movingUp();

    boolean movingDown();

    double playLeft();

    double playRight();

    double playTop();

    double playBottom();

    void addUi(Node node);

    void removeUi(Node node);

    void hidePlayer(boolean hide);

    void playMusicOverride(String fileName);

    void restoreMusic();

    void spawnExplosion(double x, double y, String size);

    void spawnCoinAt(double x, double y, String coinType);
}
