package kr.murimlife.v16;

import java.util.*;

/** Deterministic, unit-testable growth curves. These are provisional balance presets. */
public final class RealmRules {
    private RealmRules() {}
    public static final String[] REALMS = {"삼류", "이류", "일류", "절정", "초절정", "화경", "현경", "생사경"};
    public static final String[] PHASES = {"초입", "소성", "대성", "초월"};
    // Rising steeply from Absolute; early three realm transitions are similar.
    public static final long[] POWER_FLOORS = {100, 280, 510, 1100, 3500, 12500, 50000, 210000};
    public static final double[] REALM_BREAKTHROUGH_DIFFICULTY = {1.0, 1.15, 1.25, 3.6, 7.5, 15, 32, 75};
    // Progress is clamped by current realm until one actually breaks through.
    public static int phase(int realm, long battlePower) {
        check(realm);
        long start = POWER_FLOORS[realm];
        long next = realm == REALMS.length - 1 ? start * 4 : POWER_FLOORS[realm+1];
        long step = Math.max(1, (next - start) / 3);
        if (battlePower >= next + step) return 3;
        if (battlePower >= start + 2*step) return 2;
        if (battlePower >= start + step) return 1;
        return 0;
    }
    public static boolean canAttemptBreakthrough(int currentRealm, long battlePower) {
        return currentRealm < 7 && phase(currentRealm, battlePower) >= 2;
    }
    public static String describe(int realm, long battlePower) {
        check(realm); return REALMS[realm] + " " + PHASES[phase(realm,battlePower)];
    }
    /** Relative ratio bonus for overcoming a materially stronger opponent; no auto-breakthrough. */
    public static double challengeBonus(double ownEffectivePower,double defeatedEffectivePower) {
        if (ownEffectivePower <= 0 || defeatedEffectivePower < ownEffectivePower*1.15) return 0;
        return Math.min(2.0, Math.log(defeatedEffectivePower/ownEffectivePower) / Math.log(2));
    }
    public static long power(Map<String, Double> primary, double skillMastery, double innerPower,
                             double staminaFraction,double fatigueFraction,double injuryFraction) {
        double raw = 0;
        for (double stat : primary.values()) raw += Math.max(0,stat); // all nine base attributes
        raw = raw*1.25 + Math.max(0,skillMastery)*2.3 + Math.max(0,innerPower)*3.5;
        double state = Math.max(.15, Math.min(1.0, staminaFraction)) *
            (1 - .55*Math.max(0, Math.min(1,fatigueFraction))) *
            (1 - .65*Math.max(0, Math.min(1,injuryFraction)));
        return Math.max(1,Math.round(raw*state));
    }
    private static void check(int idx) { if(idx<0 ||idx>=REALMS.length) throw new IllegalArgumentException("realm"); }
}
