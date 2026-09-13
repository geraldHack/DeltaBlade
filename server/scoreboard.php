<?php
/**
 * DeltaBlade global scoreboard.
 * Game clients GET without query params and still receive the top 10.
 * The website uses ?page=&limit= for the full list.
 */
const SCOREBOARD_SECRET = 'DeltaBlade-spoteroxe-hs-7c4e91b2';
const GAME_TOP = 10;
const STORE_MAX = 250;
const PAGE_DEFAULT = 20;
const PAGE_MAX = 50;
const MAX_SCORE = 99999999;
const MAX_WAVE = 9999;
define('DATA_FILE', is_dir('/home/gehack/deltablade-data')
    ? '/home/gehack/deltablade-data/scores.json'
    : __DIR__ . '/scores.json');

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');

function fail(int $code, string $msg): void
{
    http_response_code($code);
    echo json_encode(['ok' => false, 'error' => $msg]);
    exit;
}

function sanitize_name(string $raw): string
{
    $out = '';
    $raw = strtoupper($raw);
    $len = strlen($raw);
    for ($i = 0; $i < $len && strlen($out) < 10; $i++) {
        $c = $raw[$i];
        if (($c >= 'A' && $c <= 'Z') || $c === ' ') {
            $out .= $c;
        }
    }
    return $out;
}

function read_entries($fh): array
{
    $raw = stream_get_contents($fh);
    if ($raw === false || $raw === '') {
        return [];
    }
    $data = json_decode($raw, true);
    if (!is_array($data) || !isset($data['entries']) || !is_array($data['entries'])) {
        return [];
    }
    return $data['entries'];
}

function normalize_entries(array $entries, int $limit = STORE_MAX): array
{
    $clean = [];
    foreach ($entries as $row) {
        if (!is_array($row)) {
            continue;
        }
        $name = sanitize_name((string) ($row['name'] ?? ''));
        if ($name === '') {
            continue;
        }
        $clean[] = [
            'name' => $name,
            'score' => max(0, intval($row['score'] ?? 0)),
            'wave' => max(1, intval($row['wave'] ?? 1)),
        ];
    }
    usort($clean, static function ($a, $b) {
        if ($a['score'] !== $b['score']) {
            return $b['score'] <=> $a['score'];
        }
        return $b['wave'] <=> $a['wave'];
    });
    return array_slice($clean, 0, max(1, $limit));
}

function find_rank(array $entries, string $name, int $score, int $wave): int
{
    foreach ($entries as $i => $row) {
        if ($row['name'] === $name && $row['score'] === $score && $row['wave'] === $wave) {
            return $i;
        }
    }
    return -1;
}

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method === 'GET') {
    $fh = fopen(DATA_FILE, 'c+');
    if ($fh === false) {
        fail(500, 'store');
    }
    flock($fh, LOCK_SH);
    $all = normalize_entries(read_entries($fh), STORE_MAX);
    flock($fh, LOCK_UN);
    fclose($fh);

    if (isset($_GET['page']) || isset($_GET['limit'])) {
        $limit = intval($_GET['limit'] ?? PAGE_DEFAULT);
        $limit = max(1, min(PAGE_MAX, $limit));
        $total = count($all);
        $pages = max(1, (int) ceil(max(1, $total) / $limit));
        $page = max(1, intval($_GET['page'] ?? 1));
        if ($page > $pages) {
            $page = $pages;
        }
        $offset = ($page - 1) * $limit;
        echo json_encode([
            'entries' => array_slice($all, $offset, $limit),
            'page' => $page,
            'limit' => $limit,
            'total' => $total,
            'pages' => $pages,
        ]);
        exit;
    }

    echo json_encode(['entries' => array_slice($all, 0, GAME_TOP)]);
    exit;
}

if ($method !== 'POST') {
    fail(405, 'method');
}

$body = json_decode((string) file_get_contents('php://input'), true);
if (!is_array($body)) {
    fail(400, 'json');
}

$name = sanitize_name((string) ($body['name'] ?? ''));
$score = intval($body['score'] ?? -1);
$wave = intval($body['wave'] ?? -1);
$token = strtolower(trim((string) ($body['token'] ?? '')));

if ($name === '' || $score < 0 || $score > MAX_SCORE || $wave < 1 || $wave > MAX_WAVE) {
    fail(400, 'fields');
}

$expected = hash_hmac('sha256', $name . '|' . $score . '|' . $wave, SCOREBOARD_SECRET);
if (!hash_equals($expected, $token)) {
    fail(403, 'token');
}

$fh = fopen(DATA_FILE, 'c+');
if ($fh === false) {
    fail(500, 'store');
}
if (!flock($fh, LOCK_EX)) {
    fclose($fh);
    fail(503, 'lock');
}

$entries = normalize_entries(read_entries($fh), STORE_MAX);
$entries[] = ['name' => $name, 'score' => $score, 'wave' => $wave];
$entries = normalize_entries($entries, STORE_MAX);
$rank = find_rank($entries, $name, $score, $wave);

rewind($fh);
ftruncate($fh, 0);
fwrite($fh, json_encode(['entries' => $entries], JSON_UNESCAPED_UNICODE));
fflush($fh);
flock($fh, LOCK_UN);
fclose($fh);

echo json_encode([
    'ok' => true,
    'rank' => $rank,
    'entries' => array_slice($entries, 0, GAME_TOP),
]);
