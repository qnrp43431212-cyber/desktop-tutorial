package kr.murimlife.npc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Deterministic, side-effect free preparation checks. Does NOT spawn entities. */
public final class NpcSpawnRules {
    private NpcSpawnRules() { }
    public record Offset(int x, int y, int z) { }
    public record Position(int x, int y, int z) { }

    /** World positions are relative to a finished site, NOT the chunk loading trigger. */
    public static Position resolve(int siteX, int siteBaseY, int siteZ, Offset offset) {
        return new Position(Math.addExact(siteX, offset.x()),
                Math.addExact(siteBaseY, offset.y()), Math.addExact(siteZ, offset.z()));
    }
    public static UUID stableId(String dimension, String siteKey, int pieceIndex, int npcIndex) {
        if (dimension == null || dimension.isBlank() || siteKey == null || siteKey.isBlank()
                || pieceIndex < 0 || npcIndex < 0) throw new IllegalArgumentException("Invalid persistent NPC key");
        return UUID.nameUUIDFromBytes(("murimlife:npc-spawn/v1/" + dimension + "/" + siteKey + "/"
                + pieceIndex + "/" + npcIndex).getBytes(StandardCharsets.UTF_8));
    }
    /** Logical gates only. Callers MUST separately check real block collision, pathfinding, and SavedData. */
    public static boolean ready(String siteStage, boolean serverEnabled, boolean chunksLoaded,
                                boolean playerNearby, boolean floorClear, boolean noExistingEntity,
                                int siteCount, int siteCap, int serverCount, int serverCap) {
        return serverEnabled && "COMPLETE".equals(siteStage) && chunksLoaded && playerNearby &&
                floorClear && noExistingEntity && siteCount >= 0 && serverCount >= 0 &&
                siteCap >= 0 && serverCap >= 0 && siteCount < siteCap && serverCount < serverCap;
    }
    public static int remaining(int live, int cap) {
        if (live < 0 || cap < 0) throw new IllegalArgumentException("Negative NPC cap");
        return Math.max(0, cap - live);
    }
}
