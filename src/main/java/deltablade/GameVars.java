package deltablade;

public final class GameVars {
    public static final String SCORE = "score";
    public static final String LIVES = "lives";
    public static final String LEVEL = "level";
    public static final String WEAPON_GRADE = "weaponGrade";
    public static final String AMMO_CAP = "ammoCap";
    public static final String ACTIVE_BULLETS = "activeBullets";
    public static final String ENEMIES_REMAINING = "enemiesRemaining";
    public static final String MONEY = "money";
    public static final String EXTRA_E = "extraE";
    public static final String EXTRA_X = "extraX";
    public static final String EXTRA_T = "extraT";
    public static final String EXTRA_R = "extraR";
    public static final String EXTRA_A = "extraA";
    public static final String AUTOFIRE = "autofire";
    public static final String EXTRA_LETTER_SPAWNED_THIS_WAVE = "extraLetterSpawnedThisWave";
    
    public static final int INITIAL_LIVES = 3;
    public static final int INITIAL_AMMO_CAP = 5;
    public static final int MAX_AMMO_CAP = 12;
    public static final int MAX_WEAPON_GRADE = 4;
    
    public static final int RAIL_WIDTH = 64;
    
    public static final char[] EXTRA_LETTERS = {'E', 'X', 'T', 'R', 'A'};
    public static final String[] EXTRA_VARS = {EXTRA_E, EXTRA_X, EXTRA_T, EXTRA_R, EXTRA_A};
    
    public static final int KILL_MONEY_BASE = 5;
    public static final int WAVE_CLEAR_MONEY = 50;
    public static final double EXTRA_LETTER_DROP_CHANCE = 0.025;
    public static final double PICKUP_DROP_CHANCE = 0.08;
    public static final double AUTOFIRE_DROP_CHANCE = 0.03;
    
    public static final int BOSS_BASE_HEALTH = 10;
    public static final int BOSS_HEALTH_PER_CYCLE = 2;
    public static final int BOSS_SCORE = 1000;
    public static final int BOSS_MONEY = 50;
    
    private GameVars() {}
}
