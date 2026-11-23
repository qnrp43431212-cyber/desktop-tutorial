package kr.murimlife.worldgen;

import java.util.Collection;
import java.util.Objects;

/** Pure geometry and world-seed rules; independent of Minecraft to enable fast regression tests. */
public final class PlanningCore {
    private PlanningCore() { }

    public record Area(int x0, int z0, int x1, int z1) {
        public Area {
            if (x0 > x1 || z0 > z1) throw new IllegalArgumentException("invalid area");
        }
        public boolean hits(Area other) {
            return x0 <= other.x1 && x1 >= other.x0 && z0 <= other.z1 && z1 >= other.z0;
        }
        public Area expand(int padding) {
            if (padding < 0) throw new IllegalArgumentException("padding");
            return new Area(Math.subtractExact(x0, padding), Math.subtractExact(z0, padding),
                    Math.addExact(x1, padding), Math.addExact(z1, padding));
        }
        public int width() { return x1 - x0 + 1; }
        public int depth() { return z1 - z0 + 1; }
    }
    public record Claim(String id, String site, Area area, int gap, boolean unique) {
        public Claim {
            Objects.requireNonNull(id);
            Objects.requireNonNull(site);
            Objects.requireNonNull(area);
            if (gap < 0) throw new IllegalArgumentException("gap");
        }
    }
    public enum Rejection { ACCEPT, DUPLICATE_KEY, UNIQUE_EXISTS, RESERVED_COLLISION }
    public static Rejection check(Claim next, Collection<Claim> existing) {
        for (Claim old : existing) {
            if (next.id().equals(old.id())) return Rejection.DUPLICATE_KEY;
            if (next.unique() && next.site().equals(old.site()) && old.unique()) return Rejection.UNIQUE_EXISTS;
            int gap = Math.max(next.gap(), old.gap());
            if (next.area().expand(gap).hits(old.area())) return Rejection.RESERVED_COLLISION;
        }
        return Rejection.ACCEPT;
    }
    public static long mix(long v) {
        v = (v ^ (v >>> 30)) * 0xbf58476d1ce4e5b9L;
        v = (v ^ (v >>> 27)) * 0x94d049bb133111ebL;
        return v ^ (v >>> 31);
    }
    public static long cellSeed(long worldSeed, int cx, int cz) {
        return mix(worldSeed ^ mix((long) cx * 0x9e3779b97f4a7c15L)
                ^ mix((long) cz * 0xc2b2ae3d27d4eb4fL));
    }
    public static int median(int[] values, int used) {
        if (used < 1 || used > values.length) throw new IllegalArgumentException("used");
        int[] copy = java.util.Arrays.copyOf(values, used);
        java.util.Arrays.sort(copy);
        return copy[used / 2];
    }
}
