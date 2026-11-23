package kr.murimlife.v16;
import java.util.*;
public class MurimRuleTests {
    static void ok(boolean p,String msg){if(!p) throw new AssertionError(msg);}
    public static void main(String[] args) {
        var alias=new EpithetRegistry();UUID p=UUID.randomUUID(),npc=UUID.randomUUID();
        ok(alias.claim(npc,"검성"),"npc claims"); ok(!alias.claim(p,"검성"),"unique global");
        ok(alias.claim(p,"무영검"),"player title");ok(alias.claim(p,"협객"),"multi title");
        ok(alias.titlesOf(p).size()==2,"multi titles count");ok(alias.displayAs(p,"협객"),"display change");
        ok(!alias.retire(p,"검성"),"cannot steal");ok(alias.retire(npc,"검성"),"retirement owner only");
        ok(alias.claim(p,"검성"),"reclaim freed");
        for(int i=0;i<8;i++) {long min=RealmRules.POWER_FLOORS[i];
            ok(RealmRules.phase(i,min)==0,"realm init");
            ok(RealmRules.phase(i,min*6)==3,"realm transcendence");
            ok(RealmRules.describe(i,min).contains(RealmRules.REALMS[i]),"name");}
        ok(RealmRules.REALM_BREAKTHROUGH_DIFFICULTY[3]>RealmRules.REALM_BREAKTHROUGH_DIFFICULTY[2]*2,"difficulty wall");
        ok(RealmRules.challengeBonus(100,110)==0,"weak challenge");ok(RealmRules.challengeBonus(100,250)>0,"strong challenge");
        int count=0; for(long day=0;day<21;day++) if(RecruitmentRules.isOpen(12345L,"소림사",day))count++;
        ok(count==3,"exact recruitment length");ok(RecruitmentRules.isOpen(123,"소림사",0),"world spawn recruitment");
        for(String sect:List.of("화산파","무당파","청성파","남궁세가")) {
            count=0;for(long d=0;d<21;d++) if(RecruitmentRules.isOpen(12345,sect,d))count++;
            ok(count==3,"other sect 3-day");ok(RecruitmentRules.nextOpenDay(12345,sect,0)<=21,"will open");}
        ok(WeaponRules.Grade.values().length==7,"grade count");
        ok(WeaponRules.Family.values().length==16,"family count");
        ok(WeaponRules.effectiveness(WeaponRules.Family.JIAN,WeaponRules.Family.DAO,false)>.5,"similar");
        ok(WeaponRules.effectiveness(WeaponRules.Family.JIAN,WeaponRules.Family.HAMMER,false)==0,"invalid");
        ok(WeaponRules.effectiveDamage(10,WeaponRules.Grade.DIVINE,WeaponRules.Family.JIAN,WeaponRules.Family.JIAN,false)>20,"grade scaling");
        ok(!TournamentRules.grandOpen(21),"not every recruitment");ok(TournamentRules.grandOpen(42),"grand opens");
        ok(!TournamentRules.grandOpen(46),"grand closes");ok(TournamentRules.grandOpen(126),"next grand");
        ok(TournamentRules.newcomerEligible(30,0,2),"eligible");ok(!TournamentRules.newcomerEligible(29,0,2),"too early");
        ok(!TournamentRules.newcomerEligible(30,0,3),"too strong");
        ok(!TournamentRules.newcomerEligible(30,-1,0),"never began martial life");
        System.out.println("PASS: 45+ title / realm / recruitment / weapon / tournament assertions");
    }
}
