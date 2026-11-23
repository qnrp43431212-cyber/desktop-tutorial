package kr.murimlife;

import java.util.Random;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent world-level state. A world ALWAYS contains at least one of the two cults. */
public final class CultWorldData extends SavedData {
    private boolean demonExists;
    private boolean bloodExists;
    private String demonLeader = "";
    private String bloodLeader = "";

    private CultWorldData(long seed) {
        switch (new Random(seed ^ 0x6c8e9cf5aa111c32L).nextInt(3)) {
            case 0 -> { demonExists = true; bloodExists = true; }
            case 1 -> { demonExists = true; bloodExists = false; }
            default -> { demonExists = false; bloodExists = true; }
        }
        // World founding events and NPC leader entities are implemented in later phases.
    }
    private static CultWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        CultWorldData data = new CultWorldData(0L);
        data.demonExists = tag.getBoolean("DemonExists");
        data.bloodExists = tag.getBoolean("BloodExists");
        data.demonLeader = tag.getString("DemonLeader");
        data.bloodLeader = tag.getString("BloodLeader");
        // Fail closed if a corrupted save tries to create a world with neither cult.
        if (!data.demonExists && !data.bloodExists) data.demonExists = true;
        return data;
    }
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("DemonExists", demonExists);
        tag.putBoolean("BloodExists", bloodExists);
        tag.putString("DemonLeader", demonLeader);
        tag.putString("BloodLeader", bloodLeader);
        return tag;
    }
    public static CultWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(() -> new CultWorldData(server.overworld().getSeed()), CultWorldData::load),
            "murimlife_cult_world");
    }
    public boolean demonExists() { return demonExists; }
    public boolean bloodExists() { return bloodExists; }
    public String demonLeader() { return demonLeader; }
    public String bloodLeader() { return bloodLeader; }
    /** Invoked after a server-verified relic + inner-world trial, not from a client-side button. */
    public boolean foundDemonCult(String leaderId, boolean verifiedUnlock) {
        if (demonExists || !verifiedUnlock || leaderId.isBlank()) return false;
        demonExists = true; demonLeader = leaderId; setDirty(); return true;
    }
    public boolean foundBloodCult(String leaderId, boolean verifiedUnlock) {
        if (bloodExists || !verifiedUnlock || leaderId.isBlank()) return false;
        bloodExists = true; bloodLeader = leaderId; setDirty(); return true;
    }
    /** Call after a verified leader-kill event. Boss/NPC entity logic still needed. */
    public boolean claimDemonAfterKill(String victor, boolean leaderKillVerified) {
        if (!demonExists || !leaderKillVerified || victor.isBlank()) return false;
        demonLeader = victor; setDirty(); return true;
    }
    public boolean claimBloodAfterKill(String victor, boolean leaderKillVerified) {
        if (!bloodExists || !leaderKillVerified || victor.isBlank()) return false;
        bloodLeader = victor; setDirty(); return true;
    }
}
