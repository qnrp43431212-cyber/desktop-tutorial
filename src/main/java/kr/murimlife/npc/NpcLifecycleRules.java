package kr.murimlife.npc;

/** Testable rules for identity persistence and spectator budgets, independent of Minecraft APIs. */
public final class NpcLifecycleRules {
    private NpcLifecycleRules(){}
    public enum Residence { LOADED_ALIVE, UNLOADED_SAVED, DEAD }
    /** Chunk unload is NOT equivalent to death or despawning. */
    public static Residence onChunkUnload(Residence before){
        if(before==Residence.LOADED_ALIVE)return Residence.UNLOADED_SAVED;
        return before;
    }
    public static Residence onChunkLoad(Residence before){
        if(before==Residence.UNLOADED_SAVED)return Residence.LOADED_ALIVE;
        return before;
    }
    /** A previously assigned UUID/slot must never be auto-spawned again. */
    public static boolean allowNewResident(boolean siteComplete,boolean playerNear,boolean chunkLoaded,
          boolean vacantSlot,int loadedResidents,int loadedCap,int siteAssigned,int siteCap){
        if(loadedResidents<0||loadedCap<0||siteAssigned<0||siteCap<0)throw new IllegalArgumentException("Bad NPC limit");
        return siteComplete&&playerNear&&chunkLoaded&&vacantSlot&&
                loadedResidents<loadedCap&&siteAssigned<siteCap;
    }
    public static int tournamentCrowdSize(int requested,int safeSeats,int hardCap){
        if(requested<0||safeSeats<0||hardCap<0)throw new IllegalArgumentException("Bad spectator count");
        return Math.min(80,Math.min(requested,Math.min(safeSeats,hardCap)));
    }
}
