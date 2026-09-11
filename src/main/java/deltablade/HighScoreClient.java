package deltablade;

import javafx.application.Platform;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Outbound HTTPS client for the global top-10 on spoteroxe.de.
 * Players never open a port; the existing web host handles 443.
 */
public final class HighScoreClient {

    public static final String ENDPOINT = "https://spoteroxe.de/deltablade/scoreboard.php";
    static final String HMAC_SECRET = "DeltaBlade-spoteroxe-hs-7c4e91b2";

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile List<HighScoreStore.Entry> cache;
    private static final AtomicBoolean submitting = new AtomicBoolean(false);

    public record SubmitResult(boolean ok, int rank, List<HighScoreStore.Entry> entries) {
        public static SubmitResult fail() {
            return new SubmitResult(false, -1, cachedEntries());
        }
    }

    private HighScoreClient() {}

    public static boolean hasCache() {
        return cache != null;
    }

    public static List<HighScoreStore.Entry> cachedEntries() {
        List<HighScoreStore.Entry> snapshot = cache;
        return snapshot == null ? List.of() : snapshot;
    }

    public static void fetch(Consumer<List<HighScoreStore.Entry>> onDone) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(TIMEOUT)
                .GET()
                .build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .orTimeout(TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)
                .whenComplete((response, error) -> {
                    List<HighScoreStore.Entry> parsed = null;
                    if (error == null && response != null && response.statusCode() == 200) {
                        parsed = parseEntries(response.body());
                    }
                    if (parsed != null) {
                        cache = parsed;
                    }
                    List<HighScoreStore.Entry> result = parsed;
                    Platform.runLater(() -> {
                        if (onDone != null) {
                            onDone.accept(result);
                        }
                    });
                });
    }

    public static void submit(String name, int score, int wave, Consumer<SubmitResult> onDone) {
        if (!submitting.compareAndSet(false, true)) {
            complete(onDone, SubmitResult.fail());
            return;
        }
        String clean = HighScoreStore.sanitizeNameOrDefault(name);
        String body = "{\"name\":\"" + jsonEscape(clean) + "\",\"score\":" + score
                + ",\"wave\":" + wave + ",\"token\":\"" + token(clean, score, wave) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .orTimeout(TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)
                .whenComplete((response, error) -> {
                    SubmitResult result = SubmitResult.fail();
                    try {
                        if (error == null && response != null && response.statusCode() == 200) {
                            List<HighScoreStore.Entry> entries = parseEntries(response.body());
                            if (entries != null) {
                                cache = entries;
                                result = new SubmitResult(true, parseRank(response.body()), entries);
                            }
                        }
                    } finally {
                        submitting.set(false);
                    }
                    complete(onDone, result);
                });
    }

    static String token(String name, int score, int wave) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((name + "|" + score + "|" + wave).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static List<HighScoreStore.Entry> parseEntries(String json) {
        if (json == null) {
            return null;
        }
        int entriesAt = json.indexOf("\"entries\"");
        if (entriesAt < 0) {
            return null;
        }
        int arr = json.indexOf('[', entriesAt);
        int end = json.lastIndexOf(']');
        if (arr < 0 || end <= arr) {
            return List.of();
        }
        String block = json.substring(arr + 1, end);
        List<HighScoreStore.Entry> out = new ArrayList<>();
        int pos = 0;
        while (pos < block.length() && out.size() < HighScoreStore.MAX_ENTRIES) {
            int open = block.indexOf('{', pos);
            if (open < 0) {
                break;
            }
            int close = block.indexOf('}', open);
            if (close < 0) {
                break;
            }
            String obj = block.substring(open, close + 1);
            String name = jsonString(obj, "name");
            Integer score = jsonInt(obj, "score");
            Integer wave = jsonInt(obj, "wave");
            if (name != null && score != null && wave != null) {
                out.add(new HighScoreStore.Entry(
                        HighScoreStore.sanitizeNameOrDefault(name),
                        Math.max(0, score),
                        Math.max(1, wave)));
            }
            pos = close + 1;
        }
        return List.copyOf(out);
    }

    static int parseRank(String json) {
        Integer rank = jsonInt(json, "rank");
        return rank == null ? -1 : rank;
    }

    private static void complete(Consumer<SubmitResult> onDone, SubmitResult result) {
        Platform.runLater(() -> {
            if (onDone != null) {
                onDone.accept(result);
            }
        });
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String jsonString(String json, String key) {
        String needle = "\"" + key + "\"";
        int at = json.indexOf(needle);
        if (at < 0) {
            return null;
        }
        int colon = json.indexOf(':', at + needle.length());
        if (colon < 0) {
            return null;
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        int end = json.indexOf('"', start + 1);
        if (end < 0) {
            return null;
        }
        return json.substring(start + 1, end);
    }

    private static Integer jsonInt(String json, String key) {
        String needle = "\"" + key + "\"";
        int at = json.indexOf(needle);
        if (at < 0) {
            return null;
        }
        int colon = json.indexOf(':', at + needle.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        int start = i;
        if (i < json.length() && json.charAt(i) == '-') {
            i++;
        }
        while (i < json.length() && Character.isDigit(json.charAt(i))) {
            i++;
        }
        if (start == i || (json.charAt(start) == '-' && start + 1 == i)) {
            return null;
        }
        try {
            return Integer.parseInt(json.substring(start, i));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
