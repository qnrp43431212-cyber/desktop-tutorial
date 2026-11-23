package kr.murimlife.cult;

import kr.murimlife.MurimCombat;
import kr.murimlife.MurimProgressData;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.dialogue.MurimDialogueService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** No unconditional secret art grant. Server-side low-lethality recruiting and a difficult apprenticeship. */
public final class CultMentorshipService {
    private CultMentorshipService(){}
    private static void say(ServerPlayer player,String words){player.sendSystemMessage(Component.literal("[사문] "+words));}
    private static CultDesign.Person teacher(MurimHumanoid npc){
        return CultDesign.get(npc.getPersistentData().getString("MurimCultPersonId"));
    }
    private static MurimProgressData.Profile profile(ServerPlayer p){
        return MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
    }
    private static boolean near(ServerPlayer p,MurimHumanoid npc){
        return p.serverLevel()==npc.level() && npc.isAlive() && p.distanceToSqr(npc)<=36;
    }
    private static boolean refuse(ServerPlayer p,MurimHumanoid npc,int chance,int hostility){
        if(p.getRandom().nextInt(100)<chance)return false;
        if(p.getRandom().nextInt(100)<hostility){
            say(p,"상대가 거절하고 기습하려 합니다! 주변 교도가 이를 말리려 하지만 이미 무기를 뽑았습니다.");
            MurimDialogueService.forceHostility(p,npc);
        }else say(p,"교단은 사람이 필요하나, 이 무인은 아직 당신을 받아들일 뜻이 없습니다.");
        return true;
    }
    /** Becoming a cult disciple does not bestow any leader-only technique. */
    public static void join(ServerPlayer p,MurimHumanoid npc){
        CultDesign.Person person=teacher(npc);if(person==null||!near(p,npc)){say(p,"교단 소속의 대마두에게 직접 청해야 합니다.");return;}
        var d=MurimProgressData.get(p.server);var pr=profile(p);
        if(!pr.foundedSect.isBlank()||(!pr.sect.isBlank()&&!"무소속".equals(pr.sect))){
            say(p,pr.sect.equals(person.cult())?"이미 이 교단 소속입니다.":"다른 문파에 소속되어 있습니다. 먼저 정식 탈퇴 절차가 필요합니다.");return;
        }
        if(refuse(p,npc,CultMentorRules.admissionChance(pr.realm),1))return;
        npc.getPersistentData().remove("MurimCultProvokedPlayer");
        npc.getPersistentData().putBoolean("MurimCultHostile",false);
        pr.sect=person.cult();pr.sectAlignment=person.cult();pr.sectRank="평교도";pr.loyalty=65;
        d.setDirty();say(p,person.cult()+"의 평교도가 되었습니다. 교주의 비전과 대마두의 사공은 자동으로 습득하지 않습니다.");
    }
    public static void ask(ServerPlayer p,MurimHumanoid npc){
        CultDesign.Person person=teacher(npc);if(person==null||!near(p,npc)){say(p,"상대는 교단의 사공을 전수할 자격이 없습니다.");return;}
        var d=MurimProgressData.get(p.server);var pr=profile(p);
        if(!person.cult().equals(pr.sect)){say(p,"먼저 그 교단에 정식 입교해야 합니다.");return;}
        if(!pr.cultMentorId.isBlank()){
            say(p,"현재 사부: "+pr.cultMentorId+" / 수련 "+pr.mentorLessons+"회. 먼저 기존 사문을 정리해야 합니다.");return;
        }
        int favor=pr.counters.getOrDefault("cultFavor:"+person.id(),0);
        if(refuse(p,npc,CultMentorRules.apprenticeChance(person.realm(),pr.realm,favor),
                  CultMentorRules.hostileRefusalChance(favor)))return;
        pr.cultMentorId=person.id();pr.cultMentorArt=person.art();pr.mentorLessons=0;
        pr.mentorStartTraining=pr.training;pr.mentorLastLessonDay=-1;pr.mentorFavor=0;
        d.setDirty();say(p,person.name()+"이 제자로 받아들였습니다. 하지만 스승은 비전을 아끼므로 반복 수련·경지 증명 후에야 제대로 전수합니다.");
    }
    public static void lesson(ServerPlayer p,MurimHumanoid npc){
        CultDesign.Person person=teacher(npc);if(person==null||!near(p,npc)){say(p,"사부를 직접 만나야 합니다.");return;}
        var d=MurimProgressData.get(p.server);var pr=profile(p);
        if(!pr.cultMentorId.equals(person.id())||!pr.sect.equals(person.cult())){say(p,"이 무인은 당신의 사부가 아닙니다.");return;}
        long day=p.server.overworld().getGameTime()/24000;
        if(pr.mentorLastLessonDay==day){say(p,"하루에 한 차례만 사부의 지도를 받을 수 있습니다.");return;}
        if(pr.training-pr.mentorStartTraining<3*(pr.mentorLessons+1)){
            say(p,"다음 수업 전에 독자적인 수련 3회가 필요합니다. 현재 진도 "+(pr.training-pr.mentorStartTraining)+"회.");return;
        }
        pr.mentorLastLessonDay=day;
        if(p.getRandom().nextInt(100)<8){
            pr.qiStrain=Math.min(100,pr.qiStrain+8);pr.mentorFavor=Math.max(-40,pr.mentorFavor-2);
            d.setDirty();say(p,"사부가 일부 구결을 감췄습니다. 진기 순환이 어그러져 영단 부담이 증가했습니다. 사제 사이에 전적인 신뢰는 없습니다.");return;
        }
        pr.mentorLessons++;pr.mentorFavor=Math.min(40,pr.mentorFavor+1);
        int required=CultMentorRules.lessonsRequired(person.realm(),pr.realm);
        pr.artMastery.merge(person.art(),Math.max(1,30/required), (old,added)->Math.min(95,old+added));
        boolean complete=CultMentorRules.mastered(pr.mentorLessons,required,pr.realm,person.realm());
        if(complete){
            pr.counters.put("cultManual:"+person.art(),1);
            pr.artMastery.put(person.art(),Math.max(60,pr.artMastery.getOrDefault(person.art(),0)));
            say(p,"어렵게 "+person.art()+" 비결을 배웠습니다. /murim learn "+person.art()+" 을 사용해 습득을 시도하세요. 완전 숙련까지는 추가 훈련이 필요합니다.");
        }else say(p,"구결 진도 "+pr.mentorLessons+"/"+required+". "+(pr.realm<person.realm()?"경지가 사부의 무공에 부족하니 돌파도 필요합니다.":"기운의 운용은 스스로 익혀야 합니다."));
        d.setDirty();
    }
    public static void status(ServerPlayer p){
        var pr=profile(p);say(p,pr.cultMentorId.isBlank()?"현재 사부가 없습니다.":
            "사부 "+pr.cultMentorId+" · 사공 "+pr.cultMentorArt+" · 수업 "+pr.mentorLessons+"회 · 숙련 "+pr.artMastery.getOrDefault(pr.cultMentorArt,0)+"%");
    }
    public static void leave(ServerPlayer p){
        var d=MurimProgressData.get(p.server);var pr=profile(p);
        if(pr.cultMentorId.isBlank()){say(p,"맺은 사제 관계가 없습니다.");return;}
        pr.counters.merge("cultFavor:"+pr.cultMentorId,-10,Integer::sum);
        pr.cultMentorId="";pr.cultMentorArt="";pr.mentorLessons=0;pr.mentorLastLessonDay=-1;
        d.setDirty();say(p,"사문을 떠났습니다. 이전 스승의 불신이 남아 재입문이 어려워질 수 있습니다.");
    }
}
