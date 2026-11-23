package kr.murimlife.npc.dialogue;

import kr.murimlife.economy.MoneyService;
import kr.murimlife.economy.CurrencyRules;
import kr.murimlife.economy.RumorLedger;
import kr.murimlife.opportunity.OpportunitySites;
import kr.murimlife.v16.RecruitmentRules;
import kr.murimlife.v16.TournamentRules;
import kr.murimlife.worldgen.SiteLedger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Real-time server-sourced local knowledge; unknown map coordinates are never fabricated as fact. */
public final class InformationService {
    private static final String[] FACTIONS={"소림사","무당파","화산파","청성파","곤륜파","아미파","종남파","공동파","점창파","개방",
        "남궁세가","사천당문","하북팽가","제갈세가","모용세가","무림맹","사도련"};
    private static final String[] IDS={"shaolin","wudang","huashan","qingcheng","kunlun","emei","zhongnan","kongtong","diancang","gaibang",
        "namgung","tangmun","paengga","jegal","mourong","murim_alliance","sadaoryeon"};
    private static final Map<UUID,Visit> visits=new HashMap<>();
    private record Visit(UUID entity,int count,long tick){}
    private InformationService(){}
    private static void say(ServerPlayer player,String text){player.sendSystemMessage(Component.literal("[강호 정보] "+text));}
    private static boolean chargeIfNeeded(ServerPlayer player,LivingEntity npc,boolean premium){
        var role=NpcRoles.role(npc);long now=player.serverLevel().getGameTime();
        Visit prior=visits.get(player.getUUID());int count=prior!=null&&prior.entity().equals(npc.getUUID())&&now-prior.tick()<6000?prior.count():0;
        if(role==NpcRoles.Role.RESIDENT && (count>=3||premium)){
            say(player,"주민: 그 이상은 나도 모르오. 다른 사람에게 물어보시오.");return false;
        }
        int fee=0;
        if(role==NpcRoles.Role.WAITER && (premium||count>=2))fee=premium?45:9;
        if(role==NpcRoles.Role.MERCHANT && (premium||count>=1))fee=premium?140:22;
        if(role==NpcRoles.Role.WARRIOR && (premium||count>=3))fee=premium?40:8;
        if(fee>0&&!MoneyService.pay(player,kr.murimlife.quest.ReputationService.informationFee(player,fee),"정보 사례금"))return false;
        visits.put(player.getUUID(),new Visit(npc.getUUID(),count+1,now));
        return true;
    }
    private static long upcomingTournament(long day){
        for(long d=day;d<=day+TournamentRules.GRAND_PERIOD;d++)if(TournamentRules.grandOpen(d))return d;
        return -1;
    }
    public static void ask(ServerPlayer player,LivingEntity npc,int topic){
        if(topic==5 && OpportunitySites.get(player.serverLevel()).all().isEmpty()){
            say(player,"확인된 기연 위치가 없어 돈을 받지 않겠습니다. 탐험으로 새로운 기연이 발견되면 다시 오십시오.");return;
        }
        if(!chargeIfNeeded(player,npc,topic==5))return;
        long day=player.server.overworld().getDayTime()/24000L;
        switch(topic){
            case 1 -> {
                StringBuilder text=new StringBuilder("모집 현황 (현재 "+day+"일째): ");
                for(int i=0;i<15;i++){
                    String name=FACTIONS[i];text.append(name).append(RecruitmentRules.isOpen(player.server.overworld().getSeed(),name,day)?" 모집 중":" 다음 "+RecruitmentRules.nextOpenDay(player.server.overworld().getSeed(),name,day)+"일째");
                    if(i<14)text.append(" / ");
                }
                say(player,text.toString());
            }
            case 2 -> say(player,"정기 무림대회 개최일: "+upcomingTournament(day)+"일째 시작 · 3일간 진행 · 84일 주기. 신진무인 대회는 개인 입문 후 30일 조건입니다.");
            case 3 -> {
                var locations=SiteLedger.get(player.serverLevel()).jobs();
                int knownMax=switch(NpcRoles.role(npc)){case RESIDENT->3;case WAITER->8;case MERCHANT->17;case WARRIOR->5;};
                int offset=Math.floorMod(npc.getUUID().hashCode(),FACTIONS.length);
                for(int k=0;k<FACTIONS.length;k++){
                    int i=(k+offset)%FACTIONS.length;
                    if(k>=knownMax){say(player,FACTIONS[i]+": 이 사람은 해당 세력의 위치를 모릅니다.");continue;}
                    final String id=IDS[i];
                    var placed=locations.stream().filter(j->j.site.equals(id)&&j.stage==SiteLedger.Stage.COMPLETE).findFirst();
                    if(placed.isPresent())say(player,FACTIONS[i]+": 좌표 X="+placed.get().x+" Z="+placed.get().z+" (생성 확인됨)");
                    else say(player,FACTIONS[i]+": 현재 확인된 본부 좌표 없음. 미생성·미발견 가능성이 있어 정확한 위치를 알려줄 수 없습니다.");
                }
            }
            case 4 -> {
                var site=OpportunitySites.get(player.serverLevel()).closest(player.getBlockX(),player.getBlockZ());
                if(site.isEmpty())say(player,"근처에 확인된 기연이 없습니다. 폐허나 깊은 동굴에 옛 무인의 흔적이 남아 있다는 소문뿐입니다.");
                else {
                    var s=site.get();String direction=(s.x()>player.getBlockX()?"동":"서")+"쪽·"+(s.z()>player.getBlockZ()?"남":"북")+"쪽";
                    say(player,"기연의 소문: "+OpportunitySites.KINDS.get(s.type()).korean()+"이(가) "+direction+"에 있다는 말이 있습니다. 좌표를 알려면 상세 정보를 구하십시오.");
                }
            }
            case 5 -> premium(player,npc);
            default -> say(player,"그런 정보는 없습니다.");
        }
    }
    private static void premium(ServerPlayer player,LivingEntity npc){
        var nearest=OpportunitySites.get(player.serverLevel()).closest(player.getBlockX(),player.getBlockZ());
        if(nearest.isEmpty()){say(player,"아직 지도에 기록된 기연이 없어 정밀한 좌표를 알아낼 수 없습니다.");return;}
        var site=nearest.get();
        int fee=kr.murimlife.quest.ReputationService.informationFee(player,
            NpcRoles.role(npc)==NpcRoles.Role.MERCHANT?140:NpcRoles.role(npc)==NpcRoles.Role.WAITER?45:40);
        long seed=site.x()*31L+site.z()*17L+npc.getUUID().getLeastSignificantBits();
        var random=new Random(seed);
        int percent=NpcRoles.role(npc)==NpcRoles.Role.MERCHANT?
            Math.min(45,12+npc.getPersistentData().getInt("MurimTrustPenalty")*3):
            NpcRoles.role(npc)==NpcRoles.Role.WAITER?26:35;
        boolean fake=random.nextInt(100)<percent;
        int x=site.x()+(fake?80+random.nextInt(36):0),z=site.z()+(fake?-90-random.nextInt(25):0);
        say(player,"제보자: "+OpportunitySites.KINDS.get(site.type()).korean()+"의 위치는 X="+x+", Z="+z+"라고 하오. 정보의 진위는 직접 확인해야 하오.");
        if(NpcRoles.role(npc)==NpcRoles.Role.MERCHANT)RumorLedger.get(player.server).record(player.getUUID(),npc.getUUID(),x,z,fee,fake);
    }
    public static void dispute(ServerPlayer player,LivingEntity merchant,int option){
        var ledger=RumorLedger.get(player.server);var rumor=ledger.disputed(player.getUUID(),merchant.getUUID());
        if(rumor==null){say(player,"이 상인에게 입증할 허위정보 거래 기록이 없습니다.");return;}
        switch(option){
            case 1 -> {
                // Merchant refund and apology; poor merchant reputation reduces repeat trust.
                MoneyService.add(player,rumor.fee+Math.max(5,rumor.fee/4),"허위정보 배상 합의");
                merchant.getPersistentData().putInt("MurimTrustPenalty",Math.min(10,merchant.getPersistentData().getInt("MurimTrustPenalty")+2));
                say(player,"상인: 내 실수였소. 원금과 배상금을 드리겠소. 제발 소문을 퍼뜨리지는 말아주시오.");
            }
            case 2 -> {
                var data=kr.murimlife.MurimProgressData.get(player.server);
                data.evilDeed(player.getUUID(),data.player(player.getUUID(),player.getName().getString()),4);
                MoneyService.add(player,rumor.fee*2L,"위협으로 받아낸 보상");
                merchant.getPersistentData().putInt("MurimTrustPenalty",10);
                say(player,"협박에 굴복한 상인이 배상했지만, 주변 사람들은 당신의 행실을 나쁘게 평가합니다. 악업 +4");
            }
            case 3 -> {
                merchant.getPersistentData().putInt("MurimTrustPenalty",10);
                say(player,"사람들이 거짓 정보를 퍼뜨린 상인을 경계하기 시작했습니다. 상인의 신용이 크게 떨어집니다.");
            }
            case 4 -> say(player,"상인을 용서했습니다. 다른 정보는 더 신중히 확인해야 합니다.");
            default -> {return;}
        }
        ledger.resolve(rumor);
    }
}
