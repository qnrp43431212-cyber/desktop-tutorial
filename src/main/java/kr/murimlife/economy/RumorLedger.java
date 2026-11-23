package kr.murimlife.economy;

import kr.murimlife.opportunity.OpportunitySites;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Merchant rumors are server-authored and persist across restarts. Proof appears when inspected. */
public final class RumorLedger extends SavedData {
    public static final class Rumor {
        public UUID player,merchant;public int x,z,fee;public boolean falseRumor,discovered,resolved;
        Rumor(UUID player,UUID merchant,int x,int z,int fee,boolean lie){this.player=player;this.merchant=merchant;this.x=x;this.z=z;this.fee=fee;falseRumor=lie;}
    }
    private final Map<UUID,Rumor> rumors=new HashMap<>();
    private static long ticks;
    private RumorLedger(){}
    private static RumorLedger load(CompoundTag tag,HolderLookup.Provider provider){
        RumorLedger ledger=new RumorLedger();var list=tag.getList("Rumors",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)try{
            var t=list.getCompound(i);UUID player=UUID.fromString(t.getString("Player")),merchant=UUID.fromString(t.getString("Merchant"));
            Rumor r=new Rumor(player,merchant,t.getInt("X"),t.getInt("Z"),t.getInt("Fee"),t.getBoolean("False"));
            r.discovered=t.getBoolean("Discovered");r.resolved=t.getBoolean("Resolved");ledger.rumors.put(player,r);
        }catch(IllegalArgumentException ignored){}return ledger;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        var list=new ListTag();for(Rumor r:rumors.values()){
            var t=new CompoundTag();t.putString("Player",r.player.toString());t.putString("Merchant",r.merchant.toString());
            t.putInt("X",r.x);t.putInt("Z",r.z);t.putInt("Fee",r.fee);t.putBoolean("False",r.falseRumor);
            t.putBoolean("Discovered",r.discovered);t.putBoolean("Resolved",r.resolved);list.add(t);
        }tag.put("Rumors",list);return tag;
    }
    public static RumorLedger get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(RumorLedger::new,RumorLedger::load),"murimlife_rumors_v123");}
    public void record(UUID player,UUID merchant,int x,int z,int fee,boolean lie){rumors.put(player,new Rumor(player,merchant,x,z,fee,lie));setDirty();}
    public Rumor disputed(UUID player,UUID merchant){
        Rumor r=rumors.get(player);return r!=null&&r.merchant.equals(merchant)&&r.discovered&&r.falseRumor&&!r.resolved?r:null;
    }
    public void resolve(Rumor r){r.resolved=true;setDirty();}
    public static void tick(ServerTickEvent.Post event){
        if(++ticks%40!=0)return;
        var ledger=get(event.getServer());
        for(ServerPlayer p:event.getServer().getPlayerList().getPlayers()){
            Rumor r=ledger.rumors.get(p.getUUID());if(r==null||r.discovered||p.level()!=event.getServer().overworld())continue;
            long dx=(long)p.getBlockX()-r.x,dz=(long)p.getBlockZ()-r.z;
            if(dx*dx+dz*dz<=24*24){
                boolean actual=OpportunitySites.get(p.serverLevel()).containsSiteNear(r.x,r.z,32);
                r.discovered=true;r.falseRumor=!actual;ledger.setDirty();
                if(r.falseRumor)MoneyService.message(p,"소문에 적힌 좌표에 기연이 없습니다. 거짓 정보를 판 상인을 찾아가 추궁할 수 있습니다.");
                else MoneyService.message(p,"소문에 적힌 곳에서 기연의 흔적을 확인했습니다.");
            }
        }
    }
}
