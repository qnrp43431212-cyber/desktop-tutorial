package kr.murimlife.v16;
import java.util.*;
/** Vanilla day = 24000 ticks; each sect gets deterministic independent 21-day phase. */
public final class RecruitmentRules {
    public static final int PERIOD_DAYS=21, OPEN_DAYS=3;
    private RecruitmentRules() {}
    public static int phase(long worldSeed,String sect) {
        // One anchored sect starts open on day zero, not every sect does.
        if("소림사".equals(sect)) return 0;
        return new SplittableRandom(worldSeed ^ (long)sect.hashCode()*0x9e3779b97f4a7c15L).nextInt(PERIOD_DAYS);
    }
    public static boolean isOpen(long seed,String sect,long worldDay) {
        return Math.floorMod(worldDay + phase(seed,sect), PERIOD_DAYS) < OPEN_DAYS;
    }
    public static long nextOpenDay(long seed,String sect,long fromDay) {
        for(long d=fromDay;d<=fromDay+PERIOD_DAYS;d++) if(isOpen(seed,sect,d)) return d;
        throw new AssertionError("no next recruitment");
    }
}
