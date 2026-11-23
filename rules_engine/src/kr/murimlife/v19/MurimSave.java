package kr.murimlife.v19;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Versioned binary persistence for the headless engine. In the game, SavedData is authoritative. */
public final class MurimSave {
    private MurimSave(){}
    private static final int MAGIC=0x4D555249,VERSION=1;
    public static void write(MurimEngine.World world,Path path) throws IOException {
        Path parent=path.toAbsolutePath().getParent();if(parent!=null)Files.createDirectories(parent);
        // Write to a temporary file and atomically replace where supported.
        Path tmp=path.resolveSibling(path.getFileName()+".tmp");
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))){
            out.writeInt(MAGIC);out.writeInt(VERSION);out.writeLong(world.seed);out.writeLong(world.day);
            out.writeBoolean(world.demonFounded);out.writeBoolean(world.bloodFounded);
            out.writeInt(world.actors.size());
            for(var a:world.actors.values()){
                out.writeUTF(a.id.toString());out.writeUTF(a.name);out.writeBoolean(a.npc);out.writeBoolean(a.alive);out.writeBoolean(a.adult);
                out.writeUTF(a.sect);out.writeUTF(a.weapon);out.writeUTF(a.trait);out.writeUTF(a.team);out.writeUTF(a.displayedEpithet);
                out.writeInt(a.realm);out.writeInt(a.good);out.writeInt(a.evil);out.writeInt(a.silver);out.writeInt(a.wins);out.writeInt(a.trainingDays);
                out.writeLong(a.becameMartialAt);out.writeDouble(a.fatigue);out.writeDouble(a.inner);out.writeDouble(a.stamina);
                out.writeDouble(a.injury);out.writeDouble(a.breath);out.writeDouble(a.balance);out.writeDouble(a.potentialBonus);out.writeDouble(a.peakPower);
                out.writeUTF(a.spouse==null?"":a.spouse.toString());
                for(String s:MurimEngine.STATS)out.writeDouble(a.stat(s));
                writeStrings(out,a.knownMartials);writeStrings(out,a.relics);writeStrings(out,a.epithets);writeStrings(out,a.knownRumors);
                out.writeInt(a.skillMastery.size());for(var e:a.skillMastery.entrySet()){out.writeUTF(e.getKey());out.writeInt(e.getValue());}
                writeUUIDIntMap(out,a.affection);writeUUIDIntMap(out,a.trust);
            }
            out.writeInt(world.titleOwners.size());for(var e:world.titleOwners.entrySet()){out.writeUTF(e.getKey());out.writeUTF(e.getValue().toString());}
            out.writeInt(world.sects.size());for(var s:world.sects.values()){
                out.writeUTF(s.name);out.writeInt(s.type.ordinal());out.writeInt(s.recruitmentOffset);out.writeInt(s.fame);
                out.writeUTF(s.leader==null?"":s.leader.toString());
                out.writeInt(s.members.size());for(UUID member:s.members)out.writeUTF(member.toString());
            }
            out.writeInt(world.fates.size());for(var f:world.fates.values()){
                out.writeUTF(f.id);out.writeUTF(f.name);out.writeUTF(f.hint);out.writeUTF(f.reward);
                out.writeInt(f.status.ordinal());out.writeLong(f.discoveredDay);out.writeLong(f.resolvedDay);
                out.writeUTF(f.firstDiscoverer==null?"":f.firstDiscoverer.toString());out.writeUTF(f.winner==null?"":f.winner.toString());
            }
            out.writeInt(world.alreadyWonNewcomer.size());for(UUID id:world.alreadyWonNewcomer)out.writeUTF(id.toString());
        }
        try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException ex){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
    public static MurimEngine.World read(Path path) throws IOException {
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)throw new IOException("Invalid MURIM save format");
            MurimEngine.World world=new MurimEngine.World(in.readLong());world.day=in.readLong();
            world.demonFounded=in.readBoolean();world.bloodFounded=in.readBoolean();
            if(!world.demonFounded&&!world.bloodFounded)throw new IOException("World requires a cult");
            world.actors.clear();int count=readCount(in,100_000);
            for(int i=0;i<count;i++){
                UUID id=UUID.fromString(in.readUTF());String name=in.readUTF();boolean npc=in.readBoolean();
                var a=world.addActor(id,name,npc);a.alive=in.readBoolean();a.adult=in.readBoolean();
                a.sect=in.readUTF();a.weapon=in.readUTF();a.trait=in.readUTF();a.team=in.readUTF();a.displayedEpithet=in.readUTF();
                a.realm=in.readInt();a.good=in.readInt();a.evil=in.readInt();a.silver=in.readInt();a.wins=in.readInt();a.trainingDays=in.readInt();
                a.becameMartialAt=in.readLong();a.fatigue=in.readDouble();a.inner=in.readDouble();a.stamina=in.readDouble();
                a.injury=in.readDouble();a.breath=in.readDouble();a.balance=in.readDouble();a.potentialBonus=in.readDouble();a.peakPower=in.readDouble();
                String spouse=in.readUTF();a.spouse=spouse.isEmpty()?null:UUID.fromString(spouse);
                for(String stat:MurimEngine.STATS)a.stats.put(stat,in.readDouble());
                readStrings(in,a.knownMartials);readStrings(in,a.relics);readStrings(in,a.epithets);readStrings(in,a.knownRumors);
                int mastery=readCount(in,10000);for(int k=0;k<mastery;k++)a.skillMastery.put(in.readUTF(),in.readInt());
                readUUIDIntMap(in,a.affection);readUUIDIntMap(in,a.trust);
            }
            world.titleOwners.clear();count=readCount(in,100_000);
            for(int i=0;i<count;i++)world.titleOwners.put(in.readUTF(),UUID.fromString(in.readUTF()));
            world.sects.clear();count=readCount(in,10_000);
            for(int i=0;i<count;i++){
                String name=in.readUTF();int type=in.readInt();int offset=in.readInt();
                var s=new MurimEngine.Sect(name,MurimEngine.SectType.values()[type],offset);s.fame=in.readInt();
                String leader=in.readUTF();s.leader=leader.isEmpty()?null:UUID.fromString(leader);
                int members=readCount(in,100_000);for(int j=0;j<members;j++)s.members.add(UUID.fromString(in.readUTF()));
                world.sects.put(name,s);
            }
            world.fates.clear();count=readCount(in,10_000);
            for(int i=0;i<count;i++){
                String id=in.readUTF(),name=in.readUTF(),hint=in.readUTF(),reward=in.readUTF();
                var f=new MurimEngine.Fate(id,name,hint,reward);
                f.status=MurimEngine.FateStatus.values()[in.readInt()];f.discoveredDay=in.readLong();f.resolvedDay=in.readLong();
                String first=in.readUTF(),winner=in.readUTF();f.firstDiscoverer=first.isBlank()?null:UUID.fromString(first);
                f.winner=winner.isBlank()?null:UUID.fromString(winner);
                world.fates.put(id,f);
            }
            count=readCount(in,100_000);for(int i=0;i<count;i++)world.alreadyWonNewcomer.add(UUID.fromString(in.readUTF()));
            return world;
        }catch(IllegalArgumentException|ArrayIndexOutOfBoundsException ex){throw new IOException("Corrupted MURIM save",ex);}
    }
    private static void writeStrings(DataOutputStream out,Collection<String> strings)throws IOException{out.writeInt(strings.size());for(String s:strings)out.writeUTF(s);}
    private static void readStrings(DataInputStream in,Collection<String> result)throws IOException{int n=readCount(in,100_000);for(int i=0;i<n;i++)result.add(in.readUTF());}
    private static void writeUUIDIntMap(DataOutputStream out,Map<UUID,Integer> map)throws IOException{out.writeInt(map.size());for(var e:map.entrySet()){out.writeUTF(e.getKey().toString());out.writeInt(e.getValue());}}
    private static void readUUIDIntMap(DataInputStream in,Map<UUID,Integer> map)throws IOException{int n=readCount(in,100_000);for(int i=0;i<n;i++)map.put(UUID.fromString(in.readUTF()),in.readInt());}
    private static int readCount(DataInputStream in,int limit)throws IOException{int count=in.readInt();if(count<0||count>limit)throw new IOException("invalid count "+count);return count;}
}
