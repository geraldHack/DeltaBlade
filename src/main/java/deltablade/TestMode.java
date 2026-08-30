package deltablade;

import javafx.scene.input.KeyCode;

import java.util.Optional;

/**
 * Debug codes: tap the same number key three times (111 = minigame 1, 222 = 2).
 */
public final class TestMode {

    public record Drop(String spawnName, String banner) {}

    private static final long TIMEOUT_MS = 900;
    private static final int TAPS = 3;

    private static int lastDigit;
    private static int count;
    private static long lastAt;

    private TestMode() {}

    public static Optional<Drop> feed(KeyCode code) {
        Integer digit = toDigit(code);
        if (digit == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        if (digit == lastDigit && now - lastAt < 40) {
            return Optional.empty();
        }
        if (digit != lastDigit || now - lastAt > TIMEOUT_MS) {
            lastDigit = digit;
            count = 1;
        } else {
            count++;
        }
        lastAt = now;
        if (count < TAPS) {
            return Optional.empty();
        }
        count = 0;
        Drop drop = dropFor(digit);
        if (drop == null) {
            return Optional.of(new Drop(null, "TEST  " + digit + digit + digit + "  ?"));
        }
        return Optional.of(drop);
    }

    public static Drop dropFor(int index) {
        return switch (index) {
            case 1 -> new Drop("meteorPickup", "TEST DROP  METEOR");
            case 2 -> new Drop("cognitivePickup", "TEST DROP  COGNITIVE");
            default -> null;
        };
    }

    private static Integer toDigit(KeyCode code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case DIGIT1, NUMPAD1 -> 1;
            case DIGIT2, NUMPAD2 -> 2;
            case DIGIT3, NUMPAD3 -> 3;
            case DIGIT4, NUMPAD4 -> 4;
            case DIGIT5, NUMPAD5 -> 5;
            case DIGIT6, NUMPAD6 -> 6;
            case DIGIT7, NUMPAD7 -> 7;
            case DIGIT8, NUMPAD8 -> 8;
            case DIGIT9, NUMPAD9 -> 9;
            default -> null;
        };
    }
}
