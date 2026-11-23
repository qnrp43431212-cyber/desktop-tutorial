package kr.murimlife.economy;

import kr.murimlife.MurimProgressData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Real, server-observed corrective assignment from the Murim Alliance; no client-side success flags. */
public final class SectDiscipline extends SavedData {
    public static final class Audit {
        public String sect;public UUID founder;public long start,deadline;
        public int trainingStart,guardians,iron,bread,raiders,raidsStopped;
        Audit(String sect,UUID founder,long start,int trainingStart){this.sect=sect;this.founder=founder;
            this.start=start;this.deadline=start+GovernanceRules.CORRECTION_DAYS;this.trainingStart=trainingStart;}
        public boolean complete(int training){return raiders>=3&&raidsStopped>=1;}
    }
    private final Map<String,Audit> audits=new HashMap<>();
    private static long ticks;
    private SectDiscipline(){}
    private static SectDiscipline load(CompoundTag tag,HolderLookup.Provider provider){
        SectDiscipline out=new SectDiscipline();var list=tag.getList("Audits",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)try{
            var t=list.getCompound(i);String sect=t.getString("Sect");
            UUID owner=UUID.fromString(t.getString("Founder"));
            Audit a=new Audit(sect,owner,t.getLong("Start"),t.getInt("TrainingStart"));
            a.deadline=t.getLong("Deadline");a.guardians=t.getInt("Guardians");a.iron=t.getInt("Iron");a.bread=t.getInt("Bread");
            a.raiders=t.getInt("Raiders");a.raidsStopped=t.getInt("RaidsStopped");
            if(!sect.isBlank())out.audits.put(sect,a);
        }catch(IllegalArgumentException ignored){}return out;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        var list=new ListTag();for(Audit a:audits.values()){
            var t=new CompoundTag();t.putString("Sect",a.sect);t.putString("Founder",a.founder.toString());
            t.putLong("Start",a.start);t.putLong("Deadline",a.deadline);t.putInt("TrainingStart",a.trainingStart);
            t.putInt("Guardians",a.guardians);t.putInt("Iron",a.iron);t.putInt("Bread",a.bread);
            t.putInt("Raiders",a.raiders);t.putInt("RaidsStopped",a.raidsStopped);list.add(t);
        }tag.put("Audits",list);return tag;
    }
    public static SectDiscipline get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(SectDiscipline::new,SectDiscipline::load),"murimlife_sect_discipline_v124");}
    public Audit audit(String sect){return audits.get(sect);}
    public void openCase(MinecraftServer server,String sect,MurimProgressData.Profile founder,UUID claimant,long day){
        if(audits.containsKey(sect)||founder.righteousLicenseRevoked)return;
        UUID owner=null;for(var e:MurimProgressData.get(server).playerProfiles().entrySet())
            if(e.getValue()==founder){owner=e.getKey();break;}
        if(owner==null)return;
        var a=new Audit(sect,owner,day,founder.training);audits.put(sect,a);setDirty();
        ServerPlayer player=server.getPlayerList().getPlayer(owner);
        if(player!=null)message(player,"무림맹 긴급 시정 명령: 15일 이내 마교·혈교 침공대를 한 차례 완전히 격퇴하고 적 전투원 3명 이상 직접 처치하십시오. 납품은 요구하지 않습니다. /murim sect audit");
    }
    private static void message(ServerPlayer p,String m){p.sendSystemMessage(Component.literal("[무림맹 감찰] "+m));}
    public void progress(ServerPlayer player){
        var data=MurimProgressData.get(player.server);var pr=data.player(player.getUUID(),player.getName().getString());
        Audit a=audits.get(pr.foundedSect);if(a==null||!a.founder.equals(player.getUUID())){message(player,"현재 진행 중인 정파 시정 명령이 없습니다.");return;}
        long day=player.server.overworld().getGameTime()/24000L;
        message(player,"남은 기간 "+Math.max(0,a.deadline-day)+"일 / 마교·혈교 침공대 전투원 직접 처치 "+a.raiders+"/3 / 습격 완전 격퇴 "+a.raidsStopped+"/1. 감찰 기간 중 습격 사건이 발생합니다.");
    }
    /** Legacy donation commands cannot fulfill the new event-driven corrective assignment. */
    public void deliver(ServerPlayer player,String kind){
        message(player,"v1.26부터 감찰은 사건형 임무입니다. 마교·혈교 습격대 방어만 완료로 인정합니다. /murim sect audit");
    }
    /** Only authenticated cult incursion entity deaths are counted, never arbitrary mob kills. */
    public void raiderDefeated(ServerPlayer player){
        var pr=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        Audit a=audits.get(pr.foundedSect);
        if(a==null||!a.founder.equals(player.getUUID())||player.server.overworld().getGameTime()/24000L>a.deadline)return;
        a.raiders=Math.min(100,a.raiders+1);setDirty();
        if(a.complete(pr.training))finish(player.server,a,pr);
    }
    public void raidRepelled(ServerPlayer player){
        var pr=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        Audit a=audits.get(pr.foundedSect);
        if(a==null||!a.founder.equals(player.getUUID())||player.server.overworld().getGameTime()/24000L>a.deadline)return;
        a.raidsStopped=Math.min(100,a.raidsStopped+1);setDirty();
        if(a.complete(pr.training))finish(player.server,a,pr);
    }
    public void guardianKill(ServerPlayer player){
        // Legacy hook deliberately does not grant corrective credit. Event-driven raid evidence only.
    }
    private void finish(MinecraftServer server,Audit a,MurimProgressData.Profile founder){
        var data=MurimProgressData.get(server);
        long total=0;int repaid=0;
        for(var e:data.playerProfiles().entrySet()){
            var p=e.getValue();long debt=p.wageClaims.getOrDefault(a.sect,0L);
            if(debt<=0)continue;
            if(!MoneyService.change(data,p,debt))continue;
            total+=debt;repaid++;p.wageClaims.remove(a.sect);
            if(a.sect.equals(p.wageCreditorSect)){p.wageDebt=0;p.wageCreditorSect="";p.wageFirstMissedDay=-1;}
            p.loyalty=Math.min(100,p.loyalty+20);data.setDirty();
            ServerPlayer online=server.getPlayerList().getPlayer(e.getKey());
            if(online!=null)MoneyService.message(online,"무림맹이 체불된 급여 "+CurrencyRules.describe(debt)+"를 대납했습니다.");
        }
        long npcRepaid=NpcSectRoster.get(server).reimburse(server,a.sect);
        total+=npcRepaid;
        data.goodDeed(a.founder,founder,35);founder.reputation=GovernanceRules.reputation(founder.reputation,35);
        data.setDirty();audits.remove(a.sect);setDirty();
        ServerPlayer online=server.getPlayerList().getPlayer(a.founder);
        if(online!=null)message(online,"시정 임무 성공! 선업 +35·평판 +35, 체불금 "+CurrencyRules.describe(total)+" 대납 ("+repaid+"명). 탈퇴한 인원의 자동 복귀는 없습니다.");
    }
    private void fail(MinecraftServer server,Audit a){
        var data=MurimProgressData.get(server);
        var founder=data.playerProfiles().get(a.founder);
        if(founder!=null){founder.righteousLicenseRevoked=true;founder.reputation=GovernanceRules.reputation(founder.reputation,-100);data.setDirty();}
        var members=new ArrayList<Map.Entry<UUID,MurimProgressData.Profile>>();
        for(var e:data.playerProfiles().entrySet())if(e.getValue().sect.equals(a.sect)&&e.getValue().foundedSect.isBlank())members.add(e);
        members.sort(Comparator.comparing(e->e.getKey().toString()));
        int n=GovernanceRules.desertionQuota(members.size());
        for(int i=0;i<n;i++){
            var p=members.get(i).getValue();p.sect="무소속";p.sectRank="외문제자";p.loyalty=Math.max(0,p.loyalty-35);p.wageFirstMissedDay=-1;
            data.setDirty();
        }
        int npcGone=NpcSectRoster.get(server).desert(server,a.sect);
        audits.remove(a.sect);setDirty();
        ServerPlayer owner=server.getPlayerList().getPlayer(a.founder);
        if(owner!=null)message(owner,"시정 명령 실패: 정파 자격 박탈·평판 -100·남은 문파원의 70% 탈퇴(NPC "+npcGone+"명 포함). 채무는 소멸하지 않습니다.");
    }
    public static void tick(ServerTickEvent.Post event){
        if(++ticks%200!=0)return;
        var server=event.getServer();var svc=get(server);long day=server.overworld().getGameTime()/24000L;
        for(Audit a:new ArrayList<>(svc.audits.values())){
            var founder=MurimProgressData.get(server).playerProfiles().get(a.founder);
            if(founder!=null&&a.complete(founder.training)&&day<=a.deadline)svc.finish(server,a,founder);
            else if(day>a.deadline)svc.fail(server,a);
        }
    }
}
