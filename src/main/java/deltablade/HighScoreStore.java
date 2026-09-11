package deltablade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * Top-10 hiscores plus last used name (1–10 letters). Survives restarts via Preferences.
 */
public final class HighScoreStore {

    public static final int MAX_ENTRIES = 10;
    public static final int NAME_LENGTH = 10;
    public static final String DEFAULT_NAME = "AAA";

    public record Entry(String name, int score, int wave) {}

    private static final Preferences PREFS = Preferences.userNodeForPackage(HighScoreStore.class);
    private static final String KEY_TABLE = "highScores";
    private static final String KEY_LAST_NAME = "highScoreLastName";

    private HighScoreStore() {}

    public static List<Entry> entries() {
        return parse(PREFS.get(KEY_TABLE, ""));
    }

    public static String lastName() {
        return sanitizeNameOrDefault(PREFS.get(KEY_LAST_NAME, DEFAULT_NAME));
    }

    public static void setLastName(String name) {
        PREFS.put(KEY_LAST_NAME, sanitizeNameOrDefault(name));
    }

    public static boolean qualifies(int score) {
        return qualifies(score, entries());
    }

    public static boolean qualifies(int score, List<Entry> table) {
        if (table == null || table.size() < MAX_ENTRIES) {
            return true;
        }
        return score > table.getLast().score();
    }

    /**
     * Inserts a new row, keeps the list sorted and capped.
     * @return 0-based rank of the new entry, or -1 if it did not qualify
     */
    public static int insert(String name, int score, int wave) {
        String clean = sanitizeNameOrDefault(name);
        List<Entry> table = new ArrayList<>(entries());
        table.add(new Entry(clean, Math.max(0, score), Math.max(1, wave)));
        table.sort(Comparator
                .comparingInt(Entry::score).reversed()
                .thenComparing(Comparator.comparingInt(Entry::wave).reversed()));
        if (table.size() > MAX_ENTRIES) {
            table = new ArrayList<>(table.subList(0, MAX_ENTRIES));
        }
        int rank = -1;
        for (int i = 0; i < table.size(); i++) {
            Entry entry = table.get(i);
            if (entry.name().equals(clean) && entry.score() == score && entry.wave() == Math.max(1, wave)) {
                rank = i;
                break;
            }
        }
        PREFS.put(KEY_TABLE, serialize(table));
        setLastName(clean);
        return rank;
    }

    public static String sanitizeName(String raw) {
        StringBuilder letters = new StringBuilder();
        if (raw != null) {
            for (int i = 0; i < raw.length(); i++) {
                char c = Character.toUpperCase(raw.charAt(i));
                if ((c >= 'A' && c <= 'Z') || c == ' ') {
                    letters.append(c);
                }
                if (letters.length() == NAME_LENGTH) {
                    break;
                }
            }
        }
        return letters.toString();
    }

    public static String sanitizeNameOrDefault(String raw) {
        String clean = sanitizeName(raw);
        return clean.isEmpty() ? DEFAULT_NAME : clean;
    }

    private static List<Entry> parse(String packed) {
        List<Entry> table = new ArrayList<>();
        if (packed == null || packed.isBlank()) {
            return table;
        }
        for (String row : packed.split("\\|")) {
            String[] parts = row.split(":");
            if (parts.length != 3) {
                continue;
            }
            try {
                table.add(new Entry(
                        sanitizeNameOrDefault(parts[0]),
                        Integer.parseInt(parts[1]),
                        Math.max(1, Integer.parseInt(parts[2]))));
            } catch (NumberFormatException ignored) {
                // skip corrupt row
            }
        }
        table.sort(Comparator
                .comparingInt(Entry::score).reversed()
                .thenComparing(Comparator.comparingInt(Entry::wave).reversed()));
        if (table.size() > MAX_ENTRIES) {
            return List.copyOf(table.subList(0, MAX_ENTRIES));
        }
        return List.copyOf(table);
    }

    private static String serialize(List<Entry> table) {
        StringBuilder out = new StringBuilder();
        for (Entry entry : table) {
            if (!out.isEmpty()) {
                out.append('|');
            }
            out.append(entry.name()).append(':').append(entry.score()).append(':').append(entry.wave());
        }
        return out.toString();
    }

    public static String formatScore(int score) {
        return String.format(Locale.ROOT, "%06d", Math.max(0, score));
    }
}
