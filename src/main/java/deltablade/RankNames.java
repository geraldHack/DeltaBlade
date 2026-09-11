package deltablade;

/**
 * Navy-style ranks, shown after each full set of rank markers.
 * Index 0 is the starting rank; 1 is the first promotion.
 */
public final class RankNames {

    private static final String[] TITLES = {
            "RECRUIT",
            "SEAMAN 3RD CLASS",
            "SEAMAN 2ND CLASS",
            "SEAMAN 1ST CLASS",
            "PETTY OFFICER 3RD CLASS",
            "PETTY OFFICER 2ND CLASS",
            "PETTY OFFICER 1ST CLASS",
            "CHIEF PETTY OFFICER",
            "SENIOR CHIEF",
            "MASTER CHIEF",
            "WARRANT OFFICER",
            "ENSIGN",
            "LIEUTENANT J.G.",
            "LIEUTENANT",
            "LT. COMMANDER",
            "COMMANDER",
            "CAPTAIN",
            "COMMODORE",
            "REAR ADMIRAL",
            "VICE ADMIRAL",
            "ADMIRAL",
            "FLEET ADMIRAL",
            "GRAND ADMIRAL",
            "STAR MARSHAL"
    };

    private static final String[] SHORT_TITLES = {
            "RECRUIT",
            "SN 3RD CLASS",
            "SN 2ND CLASS",
            "SN 1ST CLASS",
            "PO 3RD CLASS",
            "PO 2ND CLASS",
            "PO 1ST CLASS",
            "CHIEF PO",
            "SR CHIEF",
            "MASTER CHIEF",
            "WARRANT",
            "ENSIGN",
            "LT J.G.",
            "LIEUTENANT",
            "LT CMDR",
            "COMMANDER",
            "CAPTAIN",
            "COMMODORE",
            "REAR ADM",
            "VICE ADM",
            "ADMIRAL",
            "FLEET ADM",
            "GRAND ADM",
            "STAR MSHL"
    };

    private RankNames() {}

    public static String title(int rank) {
        int index = Math.max(0, rank);
        if (index < TITLES.length) {
            return TITLES[index];
        }
        int extra = index - TITLES.length + 1;
        return TITLES[TITLES.length - 1] + " " + toRoman(extra + 1);
    }

    public static String shortTitle(int rank) {
        int index = Math.max(0, rank);
        if (index < SHORT_TITLES.length) {
            return SHORT_TITLES[index];
        }
        return title(rank);
    }

    public static String banner(int rank) {
        return "RANK  " + title(rank);
    }

    private static String toRoman(int value) {
        return switch (value) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }
}
