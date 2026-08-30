package deltablade.minigames;

public interface Minigame {

    void start(MinigameHost host);

    void update(double tpf);

    void cleanup();

    boolean usesPlayerShip();

    default void onFirePress() {}

    default void onBonusKey() {}
}
