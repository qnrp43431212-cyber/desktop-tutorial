package kr.murimlife;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import kr.murimlife.v16.RealmRules;
import kr.murimlife.v16.RecruitmentRules;

/** Single authoritative world save. Persistent player stats, NPC virtual growth, epithets. */
public final class MurimProgressData extends SavedData {
    private final Map<UUID,Profile> players=new LinkedHashMap<>();
    private final Map<UUID,Profile> simulatedNpcs=new LinkedHashMap<>();
    private final Map<String,UUID> exclusiveEpithets=new LinkedHashMap<>();
    private long lastNpcDay=-1;
    private int npcRosterVersion=0;
    public static final class Profile {
        public String name=""; public String sect="무소속";
        public int realm=0, good=0, evil=0, silver=0, copper=0, wins=0, losses=0, training=0;
        public String sectRank="외문제자", foundedSect="", sectAlignment="정파", wageCreditorSect="";
        public int reputation=0, loyalty=100;
        public long wageDebt=0, wageFirstMissedDay=-1;
        public boolean righteousLicenseRevoked=false;
        public long lastStipendDay=-1, lastBreakthroughDay=-1;
        // v1.26: cultivation aftereffects and confidential teacher contract (save-compatible).
        public String cultMentorId="", cultMentorArt="";
        public int mentorLessons=0, mentorStartTraining=0, mentorFavor=0;
        public long mentorLastLessonDay=-1;
        public int qiStrain=0, qiSeverity=0, qiStage=0, qiMistakes=0, qiPendingRealm=-1, insight=0, realmProgress=0;
        public long qiLastDoseTick=-1, qiAftereffectUntil=0, qiDeadline=0, qiSeed=0, qiLastRecoveryTick=-1;
        public final Map<String,Integer> artMastery=new LinkedHashMap<>();
        public long martialStartDay=-1, lastTrainingTick=Long.MIN_VALUE / 2;
        public long lastGrandTournamentDay=-1,lastNewcomerTournamentDay=-1;
        public double fatigue=0,inner=100;
        public long lastCastTick=Long.MIN_VALUE/2,parryUntil=0,lastInnerUpdate=0,lastParryKeyTick=Long.MIN_VALUE/2;
        public final Map<String,Double> attributes=new LinkedHashMap<>();
        public final Map<String,Integer> counters=new LinkedHashMap<>();
        public final Map<String,Long> wageClaims=new LinkedHashMap<>();
        public final List<String> epithets=new ArrayList<>();
        public String displayedEpithet="";
        public final Set<String> arts=new LinkedHashSet<>();
        public final String[] actionSlots=new String[12];
        public Profile() {
            for(String stat:List.of("근력","민첩","체질","끈기","기교","감각","정신력","오성","기감")) attributes.put(stat,10.0);
            arts.add("기본검격");arts.add("운기조식");actionSlots[0]="기본검격";
        }
        public long power() {return RealmRules.power(attributes,training*1.5,training*.9,1.0,Math.min(1,fatigue/100),0);}
        public String realmName() {return RealmRules.describe(realm,power());}
        public void gain(String stat,double amount) {
            if(!attributes.containsKey(stat))throw new IllegalArgumentException("unknown stat");
            attributes.put(stat, Math.min(10000,attributes.get(stat)+Math.max(0,amount)));
        }
        public void restoreInner(long tick) {
            long elapsed=Math.max(0,Math.min(24000,tick-lastInnerUpdate));
            inner=Math.min(100,inner+elapsed*.012*(tick<qiAftereffectUntil?.55:1.0));lastInnerUpdate=tick;
        }
        public CompoundTag save() {
            var t=new CompoundTag();t.putString("Name",name);t.putString("Sect",sect);
            t.putInt("Realm",realm);t.putInt("Good",good);t.putInt("Evil",evil);t.putInt("Silver",silver);t.putInt("Copper",copper);t.putString("SectRank",sectRank);t.putString("FoundedSect",foundedSect);t.putLong("LastStipendDay",lastStipendDay);t.putLong("LastBreakthroughDay",lastBreakthroughDay);
            t.putString("SectAlignment",sectAlignment);t.putString("WageCreditorSect",wageCreditorSect);
            t.putInt("Reputation",reputation);t.putInt("Loyalty",loyalty);t.putLong("WageDebt",wageDebt);
            t.putLong("WageFirstMissedDay",wageFirstMissedDay);t.putBoolean("RighteousLicenseRevoked",righteousLicenseRevoked);
            t.putString("CultMentorId",cultMentorId);t.putString("CultMentorArt",cultMentorArt);
            t.putInt("MentorLessons",mentorLessons);t.putInt("MentorStartTraining",mentorStartTraining);
            t.putInt("MentorFavor",mentorFavor);t.putLong("MentorLastLessonDay",mentorLastLessonDay);
            t.putInt("RealmProgress",realmProgress);t.putInt("QiStrain",qiStrain);t.putInt("QiSeverity",qiSeverity);t.putInt("QiStage",qiStage);
            t.putInt("QiMistakes",qiMistakes);t.putInt("QiPendingRealm",qiPendingRealm);
            t.putInt("QiInsight",insight);t.putLong("QiLastDoseTick",qiLastDoseTick);
            t.putLong("QiAftereffectUntil",qiAftereffectUntil);t.putLong("QiDeadline",qiDeadline);
            t.putLong("QiSeed",qiSeed);t.putLong("QiLastRecoveryTick",qiLastRecoveryTick);
            var mastery=new CompoundTag();artMastery.forEach(mastery::putInt);t.put("ArtMastery",mastery);
            t.putInt("Wins",wins);t.putInt("Losses",losses);t.putInt("Training",training);
            t.putLong("MartialStartDay",martialStartDay);t.putLong("LastTrainingTick",lastTrainingTick);t.putLong("LastGrandTournamentDay",lastGrandTournamentDay);t.putLong("LastNewcomerTournamentDay",lastNewcomerTournamentDay);
            t.putDouble("Fatigue",fatigue);t.putDouble("Inner",inner);t.putLong("CastTick",lastCastTick);t.putLong("ParryUntil",parryUntil);t.putLong("InnerUpdate",lastInnerUpdate);t.putLong("ParryKeyTick",lastParryKeyTick);t.putString("DisplayedEpithet",displayedEpithet);
            var attrs=new CompoundTag();attributes.forEach(attrs::putDouble);t.put("Attributes",attrs);
            var count=new CompoundTag();counters.forEach(count::putInt);t.put("Counters",count);
            var debts=new CompoundTag();wageClaims.forEach((key,value)->debts.putLong(key,value));t.put("WageClaims",debts);
            var names=new ListTag();epithets.forEach(s->names.add(StringTag.valueOf(s)));t.put("Epithets",names);
            var known=new ListTag();arts.forEach(s->known.add(StringTag.valueOf(s)));t.put("Arts",known);
            var slots=new ListTag();for(var s:actionSlots)slots.add(StringTag.valueOf(s==null?"":s));t.put("Slots",slots);return t;
        }
        public static Profile load(CompoundTag t) {
            var p=new Profile();p.name=t.getString("Name");p.sect=t.getString("Sect");
            p.realm=Math.max(0,Math.min(7,t.getInt("Realm")));p.good=t.getInt("Good");p.evil=t.getInt("Evil");p.silver=Math.max(0,t.getInt("Silver"));p.copper=Math.clamp(t.getInt("Copper"),0,99);p.sectRank=t.contains("SectRank")?t.getString("SectRank"):"외문제자";p.foundedSect=t.getString("FoundedSect");p.lastStipendDay=t.contains("LastStipendDay")?t.getLong("LastStipendDay"):-1;
            p.lastBreakthroughDay=t.contains("LastBreakthroughDay")?t.getLong("LastBreakthroughDay"):-1;
            p.sectAlignment=t.contains("SectAlignment")?t.getString("SectAlignment"):"정파";
            p.wageCreditorSect=t.getString("WageCreditorSect");p.reputation=Math.clamp(t.getInt("Reputation"),0,1000);
            p.loyalty=t.contains("Loyalty")?Math.clamp(t.getInt("Loyalty"),0,100):100;
            p.wageDebt=Math.max(0,t.getLong("WageDebt"));p.wageFirstMissedDay=t.contains("WageFirstMissedDay")?t.getLong("WageFirstMissedDay"):-1;
            p.righteousLicenseRevoked=t.getBoolean("RighteousLicenseRevoked");
            p.cultMentorId=t.getString("CultMentorId");p.cultMentorArt=t.getString("CultMentorArt");
            p.mentorLessons=t.getInt("MentorLessons");p.mentorStartTraining=t.getInt("MentorStartTraining");
            p.mentorFavor=t.getInt("MentorFavor");p.mentorLastLessonDay=t.contains("MentorLastLessonDay")?t.getLong("MentorLastLessonDay"):-1;
            p.realmProgress=Math.clamp(t.getInt("RealmProgress"),0,100);p.qiStrain=Math.clamp(t.getInt("QiStrain"),0,100);p.qiSeverity=Math.clamp(t.getInt("QiSeverity"),0,4);
            p.qiStage=Math.max(0,t.getInt("QiStage"));p.qiMistakes=Math.max(0,t.getInt("QiMistakes"));
            p.qiPendingRealm=t.contains("QiPendingRealm")?t.getInt("QiPendingRealm"):-1;
            p.insight=Math.max(0,t.getInt("QiInsight"));p.qiLastDoseTick=t.contains("QiLastDoseTick")?t.getLong("QiLastDoseTick"):-1;
            p.qiAftereffectUntil=t.getLong("QiAftereffectUntil");p.qiDeadline=t.getLong("QiDeadline");
            p.qiSeed=t.getLong("QiSeed");p.qiLastRecoveryTick=t.contains("QiLastRecoveryTick")?t.getLong("QiLastRecoveryTick"):-1;
            var masteries=t.getCompound("ArtMastery");for(String art:masteries.getAllKeys())
                if(!art.isBlank())p.artMastery.put(art,Math.clamp(masteries.getInt(art),0,100));
            p.wins=t.getInt("Wins");p.losses=t.getInt("Losses");p.training=t.getInt("Training");
            p.martialStartDay=t.contains("MartialStartDay")?t.getLong("MartialStartDay"):-1;
            p.lastTrainingTick=t.getLong("LastTrainingTick");p.lastGrandTournamentDay=t.contains("LastGrandTournamentDay")?t.getLong("LastGrandTournamentDay"):-1;p.lastNewcomerTournamentDay=t.contains("LastNewcomerTournamentDay")?t.getLong("LastNewcomerTournamentDay"):-1;p.fatigue=t.getDouble("Fatigue");p.inner=t.contains("Inner")?t.getDouble("Inner"):100;p.lastCastTick=t.contains("CastTick")?t.getLong("CastTick"):Long.MIN_VALUE/2;p.parryUntil=t.getLong("ParryUntil");p.lastInnerUpdate=t.getLong("InnerUpdate");p.lastParryKeyTick=t.contains("ParryKeyTick")?t.getLong("ParryKeyTick"):Long.MIN_VALUE/2;p.displayedEpithet=t.getString("DisplayedEpithet");
            var attrs=t.getCompound("Attributes");for(String key:attrs.getAllKeys()) if(p.attributes.containsKey(key))p.attributes.put(key,attrs.getDouble(key));
            var count=t.getCompound("Counters");for(String key:count.getAllKeys())p.counters.put(key,count.getInt(key));
            var debts=t.getCompound("WageClaims");for(String key:debts.getAllKeys())if(!key.isBlank())p.wageClaims.put(key,Math.max(0,debts.getLong(key)));
            if(p.wageClaims.isEmpty()&&!p.wageCreditorSect.isBlank()&&p.wageDebt>0)p.wageClaims.put(p.wageCreditorSect,p.wageDebt);
            p.epithets.clear();var names=t.getList("Epithets",8);for(int i=0;i<names.size();i++)p.epithets.add(names.getString(i));
            p.arts.clear();var arts=t.getList("Arts",8);for(int i=0;i<arts.size();i++)p.arts.add(arts.getString(i));
            var slots=t.getList("Slots",8);for(int i=0;i<Math.min(12,slots.size());i++)p.actionSlots[i]=slots.getString(i).isBlank()?null:slots.getString(i);
            return p;
        }
    }
    public static MurimProgressData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new Factory<>(MurimProgressData::new,MurimProgressData::load),"murimlife_v16_progress");
    }
    private static MurimProgressData load(CompoundTag tag,HolderLookup.Provider ignored) {
        var data=new MurimProgressData();data.lastNpcDay=tag.getLong("LastNpcDay");data.npcRosterVersion=tag.getInt("NpcRosterVersion");
        for(var key:tag.getCompound("Players").getAllKeys())try{data.players.put(UUID.fromString(key),Profile.load(tag.getCompound("Players").getCompound(key)));}catch(IllegalArgumentException ignored1){}
        for(var key:tag.getCompound("SimNpcs").getAllKeys())try{data.simulatedNpcs.put(UUID.fromString(key),Profile.load(tag.getCompound("SimNpcs").getCompound(key)));}catch(IllegalArgumentException ignored1){}
        var aliases=tag.getCompound("ExclusiveEpithets");for(String s:aliases.getAllKeys())try{data.exclusiveEpithets.put(s,UUID.fromString(aliases.getString(s)));}catch(IllegalArgumentException ignored1){}
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider ignored) {
        tag.putLong("LastNpcDay",lastNpcDay);tag.putInt("NpcRosterVersion",npcRosterVersion);
        var ps=new CompoundTag();players.forEach((id,p)->ps.put(id.toString(),p.save()));tag.put("Players",ps);
        var ns=new CompoundTag();simulatedNpcs.forEach((id,p)->ns.put(id.toString(),p.save()));tag.put("SimNpcs",ns);
        var as=new CompoundTag();exclusiveEpithets.forEach((n,id)->as.putString(n,id.toString()));tag.put("ExclusiveEpithets",as);
        return tag;
    }
    public Profile player(UUID id,String name) {
        var p=players.computeIfAbsent(id,k->{setDirty();return new Profile();});
        if(!p.name.equals(name)){p.name=name;setDirty();}return p;
    }
    /** Includes offline sect members for server-authoritative payroll. */
    public java.util.Map<UUID,Profile> playerProfiles(){return java.util.Collections.unmodifiableMap(players);}
    public boolean claimEpithet(UUID owner,Profile profile,String name) {
        if(exclusiveEpithets.containsKey(name)||name.isBlank())return false;
        exclusiveEpithets.put(name,owner);profile.epithets.add(name);
        if(profile.displayedEpithet.isBlank())profile.displayedEpithet=name;
        setDirty();return true;
    }
    public boolean displayEpithet(UUID owner,Profile p,String name) {
        if(!owner.equals(exclusiveEpithets.get(name)))return false;
        p.displayedEpithet=name;setDirty();return true;
    }
    public Optional<UUID> aliasOwner(String name){return Optional.ofNullable(exclusiveEpithets.get(name));}
    public boolean practice(Profile p,String stat,long nowTick,long today) {
        if(nowTick - p.lastTrainingTick < 200)return false;
        if(!p.attributes.containsKey(stat))return false;
        if(p.martialStartDay<0)p.martialStartDay=today;
        p.gain(stat,.18/(1+p.attributes.get(stat)/35));p.training++;
        if(p.training%2==0)p.realmProgress=Math.min(100,p.realmProgress+1);
        p.fatigue=Math.min(100,p.fatigue+.45);p.lastTrainingTick=nowTick;
        setDirty();return true;
    }
    public void recordVictory(UUID owner,Profile p, boolean innocentVictim) {
        p.wins++;p.counters.merge("victories",1,Integer::sum);
        if(innocentVictim)p.evil+=5;
        else if(p.wins%4==0)p.gain("기교",.1);
        EpithetAwards.evaluate(owner,p,this);setDirty();
    }
    public void goodDeed(UUID owner,Profile p,int amount) {p.good+=Math.max(0,amount);EpithetAwards.evaluate(owner,p,this);setDirty();}
    public void evilDeed(UUID owner,Profile p,int amount) {p.evil+=Math.max(0,amount);EpithetAwards.evaluate(owner,p,this);setDirty();}
    /** Seed and migrate virtual NPC identities. This DOES NOT create any Minecraft entities. */
    private void reconcileNpcSeedNames() {
        if(npcRosterVersion>=1)return;
        java.util.List<kr.murimlife.npc.NpcSeedProfiles.Seed> seeds=
                kr.murimlife.npc.NpcSeedProfiles.readOrEmpty();
        if(seeds.size()!=96)return; // Optional source corruption cannot break existing worlds.
        for(int i=0;i<96;i++) {
            UUID id=UUID.nameUUIDFromBytes(("murimlife:npc:"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Profile existing=simulatedNpcs.get(id);
            if(existing==null)continue;
            // Only replace stock placeholder identities. Do not overwrite ongoing NPC progression.
            if(existing.name.equals("무림인-"+(i+1))) {
                existing.name=seeds.get(i).name();
                if(existing.sect.equals("무소속")||existing.sect.equals("무당파")||existing.sect.equals("화산파"))
                    existing.sect=seeds.get(i).sect();
            }
        }
        npcRosterVersion=1;setDirty();
    }
    /** These 96 distant simulated agents are NOT spawned/visible Minecraft entities. */
    public void simulateDistantNpcs(long worldDay) {
        if(lastNpcDay>=0)reconcileNpcSeedNames();
        if(worldDay<=lastNpcDay)return;
        if(lastNpcDay<0){
            java.util.List<kr.murimlife.npc.NpcSeedProfiles.Seed> seeds=
                    kr.murimlife.npc.NpcSeedProfiles.readOrEmpty();
            for(int i=0;i<96;i++) {
                UUID id=UUID.nameUUIDFromBytes(("murimlife:npc:"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var npc=simulatedNpcs.computeIfAbsent(id,k->new Profile());
                if(seeds.size()==96){
                    var seed=seeds.get(i);
                    npc.name=seed.name();npc.sect=seed.sect();npc.realm=seed.realm();
                } else {
                    npc.name="무림인-"+(i+1);npc.sect=i%3==0?"화산파":i%3==1?"무당파":"무소속";
                    npc.realm=Math.min(7,((i*7)%95)/25);
                }
                npc.training=(i*7)%95;
            }
            if(seeds.size()==96)npcRosterVersion=1;
            for(var e:simulatedNpcs.entrySet())EpithetAwards.evaluate(e.getKey(),e.getValue(),this);
            lastNpcDay=worldDay;setDirty();return;
        }
        long delta=Math.min(worldDay-lastNpcDay,28); // catchup cap to prevent runaway simulation on big jumps
        for(var npc:simulatedNpcs.values()) {
            npc.training+=delta;
            npc.gain(npc.training%2==0?"근력":"기교",delta*.05);
            if(npc.realm<7 && npc.power()>=RealmRules.POWER_FLOORS[npc.realm+1] && RealmRules.canAttemptBreakthrough(npc.realm,npc.power()) && npc.training%(10+npc.realm*3)<delta)npc.realm++;
            npc.counters.merge(npc.training%2==0?"sword_mastery":"inner_cultivation",(int)delta,Integer::sum);
        }
        for(var e:simulatedNpcs.entrySet())EpithetAwards.evaluate(e.getKey(),e.getValue(),this);
        lastNpcDay=worldDay;setDirty();
    }
    public int simulatedNpcCount(){return simulatedNpcs.size();}
}
