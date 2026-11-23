package kr.murimlife.v16;
import java.util.*;

/** World-unique titles, multi-title owners, stable IDs, deterministic inheritance. */
public final class EpithetRegistry {
    private final Map<String,UUID> owners=new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<String>> byOwner=new HashMap<>();
    private final Map<UUID,String> display=new HashMap<>();
    public synchronized boolean claim(UUID holder,String alias) {
        Objects.requireNonNull(holder);Objects.requireNonNull(alias);
        if(alias.isBlank()|| owners.containsKey(alias)) return false;
        owners.put(alias,holder);byOwner.computeIfAbsent(holder,x->new LinkedHashSet<>()).add(alias);
        display.putIfAbsent(holder,alias);return true;
    }
    public synchronized void importExisting(UUID holder,String alias) {
        if(!claim(holder,alias)) throw new IllegalArgumentException("already owned epithet: "+alias);
    }
    public synchronized Optional<UUID> ownerOf(String alias) {return Optional.ofNullable(owners.get(alias));}
    public synchronized List<String> titlesOf(UUID holder) {
        return List.copyOf(byOwner.getOrDefault(holder,new LinkedHashSet<>()));
    }
    public synchronized String displayOf(UUID holder) {return display.getOrDefault(holder,"");}
    public synchronized boolean displayAs(UUID holder,String alias) {
        if(!byOwner.getOrDefault(holder,new LinkedHashSet<>()).contains(alias)) return false;
        display.put(holder,alias);return true;
    }
    /** Death does not silently free a world-unique name; a later world-event can explicitly retire it. */
    public synchronized boolean retire(UUID holder,String alias) {
        if(!holder.equals(owners.get(alias))) return false;
        owners.remove(alias);byOwner.get(holder).remove(alias);
        if(alias.equals(display.get(holder))) {
            var list=byOwner.get(holder);if(list.isEmpty()) display.remove(holder); else display.put(holder,list.iterator().next());
        }
        return true;
    }
    public synchronized Map<String,UUID> snapshot(){return Map.copyOf(owners);}
}
