package kr.murimlife.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Legacy 96 authored NPC seed records (not live Minecraft entities). */
public final class NpcSeedProfiles {
    private NpcSeedProfiles() { }
    public record Seed(String id, String name, String role, String sect, String skin,
                       int realm, String weapon, String temperament, double growthRate) { }
    private static volatile List<Seed> cached;
    public static List<Seed> read() {
        List<Seed> current = cached;
        if (current != null) return current;
        synchronized (NpcSeedProfiles.class) {
            if (cached != null) return cached;
            try (var raw = NpcSeedProfiles.class.getResourceAsStream("/murimnpc/legacy_profiles_96.json")) {
                if (raw == null) throw new IllegalStateException("Missing NPC seed catalog");
                JsonObject root = JsonParser.parseReader(new InputStreamReader(raw, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray json = root.getAsJsonArray("profiles");
                if (json == null || json.size() != 96) throw new IllegalStateException("NPC seed count must be 96");
                List<Seed> built = new ArrayList<>(96);
                HashSet<String> ids = new HashSet<>();
                for (int index = 0; index < 96; index++) {
                    JsonObject o = json.get(index).getAsJsonObject();
                    String id = o.get("npc_id").getAsString();
                    String expected = "npc_%03d".formatted(index + 1);
                    if (!expected.equals(id) || !ids.add(id)) throw new IllegalStateException("Bad NPC seed ID: " + id);
                    var row = new Seed(id, o.get("name").getAsString(), o.get("role").getAsString(),
                            o.get("sect").getAsString(), o.get("skin").getAsString(),
                            o.get("starting_realm").getAsInt(), o.get("preferred_weapon").getAsString(),
                            o.get("temperament").getAsString(), o.get("growth_rate").getAsDouble());
                    if (row.name().isBlank() || row.realm() < 0 || row.realm() > 7 || row.growthRate() <= 0)
                        throw new IllegalStateException("Invalid NPC profile: " + id);
                    built.add(row);
                }
                cached = List.copyOf(built);
                return cached;
            } catch (Exception e) {
                throw new IllegalStateException("Invalid MURIM LIFE NPC seed catalog", e);
            }
        }
    }
    /** Preserve game operation even if an optional NPC profile file is damaged. */
    public static List<Seed> readOrEmpty() {
        try { return read(); } catch (RuntimeException ignored) { return List.of(); }
    }
}
