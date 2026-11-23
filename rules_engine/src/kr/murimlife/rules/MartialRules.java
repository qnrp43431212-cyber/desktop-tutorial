package kr.murimlife.rules;

import java.util.*;
public final class MartialRules {
    public enum Match { PASSIVE, OPTIMAL, SIMILAR, INVALID }
    public record Art(String name, boolean passive, String optimal, Set<String> compatible) {}
    private MartialRules() {}
    public static Match compatibility(Art art, String heldFamily) {
        if (art.passive()) return Match.PASSIVE;
        if (art.optimal()==null) return Match.OPTIMAL; // e.g. active inner energy / footwork
        if (art.optimal().equals(heldFamily)) return Match.OPTIMAL;
        if (art.compatible().contains(heldFamily)) return Match.SIMILAR;
        return Match.INVALID;
    }
    public static boolean canBind(Art art) { return !art.passive(); }
    public static int normalizeWheelIndex(int n) { return Math.floorMod(n, 12); }
    public static boolean validWheelIndex(int n) { return n >= 0 && n < 12; }
}
