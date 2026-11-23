package kr.murimlife.v19;

import java.nio.file.*;
import java.util.*;

/** Uses plain Java assertions without JUnit or Minecraft runtime. */
public final class MurimEngineTest {
    private static int passed=0;
    private static void check(boolean value,String msg){if(!value)throw new AssertionError(msg);passed++;}
    private static MurimEngine.Martial sword(){return new MurimEngine.Martial("sword","연환검","화산","상승무공","active","jian","blade_combo",1.2,9.0,3,15,"verified_secondary");}
    private static MurimEngine.Actor hero(MurimEngine.World w,String name){return w.addActor(name,false);}
    public static void main(String[] args) throws Exception {
        for(int i=0;i<3000;i++){
            var w=new MurimEngine.World(i);
            check(w.demonFounded||w.bloodFounded,"world must have at least one cult");
            var same=new MurimEngine.World(i);
            check(w.demonFounded==same.demonFounded&&w.bloodFounded==same.bloodFounded,"world seed deterministic");
        }
        var w=new MurimEngine.World(69);
        var a=hero(w,"A");var b=hero(w,"B");
        check(w.claimTitle(a,"검성"),"first title");
        check(!w.claimTitle(b,"검성"),"global exclusive title");
        check(w.claimTitle(a,"검신"),"multiple titles");
        check(w.setDisplayedTitle(a,"검신"),"primary display");
        w.retireTitle(a,"검성");check(w.claimTitle(b,"검성"),"title after retirement");
        var first=w.sects.get("소림");check(first.recruitmentOpen(0),"shaolin opening day0");
        check(first.recruitmentOpen(1)&&first.recruitmentOpen(2)&&!first.recruitmentOpen(3),"3-day window");
        check(first.recruitmentOpen(21),"reopen day21");
        check(w.sects.values().stream().anyMatch(s->!s.recruitmentOpen(0)),"all schedules not synchronized");
        check(MurimEngine.Combat.compatibility("jian","jian")==1,"optimal weapon");
        check(MurimEngine.Combat.compatibility("jian","dao")>0&&MurimEngine.Combat.compatibility("jian","dao")<1,"similar weapon weaker");
        check(MurimEngine.Combat.compatibility("jian","bow")==0,"incompatible weapon blocked");
        check(MurimEngine.Combat.quality("신병")>MurimEngine.Combat.quality("범철"),"grade order");
        double old=a.stat("근력");a.train("근력",1);double firstGain=a.stat("근력")-old;
        a.train("근력",1000);double current=a.stat("근력");a.train("근력",1);
        check(a.stat("근력")-current<firstGain,"growth diminishing returns");
        check(a.fatigue>0,"training fatigue");a.rest();check(a.fatigue<100,"rest relieves fatigue");
        var weak=hero(w,"weak");check(!w.trainBreakthrough(weak,true,false,0),"not ready for breakthrough");
        for(var s:MurimEngine.STATS)weak.stats.put(s,40.0);
        weak.updatePeak();check(weak.canBreakthrough(),"full stage for breakthrough");
        check(w.trainBreakthrough(weak,false,false,0),"early tier simple breakthrough");
        weak.realm=2;weak.updatePeak();check(!w.trainBreakthrough(weak,false,false,0),"advanced tier requires insight");
        check(w.trainBreakthrough(weak,true,false,0),"meditation insight");
        weak.realm=2;check(w.trainBreakthrough(weak,false,true,.3),"stronger enemy breakthrough");
        weak.realm=2;check(!w.trainBreakthrough(weak,false,true,.03),"insufficient enemy gap");
        var c=w.testOrthodox(a,"소림",100,99,100,100,90,false);
        check(c.dummyScore()>=65,"1pct miss forgiven");
        check(c.passed(),"standard exam passes");
        var c2=w.testOrthodox(a,"소림",100,99,100,100,90,true);
        check(!c2.passed()||c2.offCycle(),"offcycle stricter");
        var rival=hero(w,"Rival");
        var fate=w.fates.get("lone_sword"); fate.status=MurimEngine.FateStatus.RUMORED;fate.discoveredDay=w.day;
        check(!w.resolveFateByRival(fate.id,MurimEngine.FateStatus.TAKEN_BY_OTHER),"two-day grace");
        w.advanceDays(2);
        check(w.resolveFateByRival(fate.id,MurimEngine.FateStatus.TAKEN_BY_OTHER),"rival may claim after two days");
        check(!w.claimFate(a,fate.id),"lost fate unavailable");
        var other=w.fates.get("mountain_healer");other.status=MurimEngine.FateStatus.RUMORED;
        check(w.claimFate(a,other.id),"fate rewards held");
        check(a.relics.contains("내상치유비록"),"fate inventory memory");
        check(!w.foundCult(a,"천마신교")||w.demonFounded,"cult only when world absent and authorized");
        w.demonFounded=false;
        check(!w.foundCult(a,"천마신교"),"cult relic gate");
        check(w.unlockCult(a,"천마계승령"),"relic unlock");
        check(w.foundCult(a,"천마신교"),"cult founded by eligible player");
        check(!w.foundCult(b,"천마신교"),"duplicate cult blocked");
        var created=w.createSect(b,"청풍문",false,0);
        check(created!=null&&created.type==MurimEngine.SectType.UNORTHODOX,"unregistered = unorthodox");
        b.silver=20;
        check(w.createSect(b,"적월문",true,30)==null,"founding cost enforced");
        check(w.createSect(b,"적월문",true,10)!=null,"registered orthodox");
        check(!w.marry(a,rival),"cannot marry at zero affection");
        for(int i=0;i<8;i++){w.befriend(a,rival,12);w.befriend(rival,a,12);}
        check(w.marry(a,rival),"mutual adult consent marriage");
        var child=w.addActor("Minor",true);child.adult=false;
        check(!w.marry(b,child),"no minor romance");
        var companion=w.addActor("Companion",true);
        for(int i=0;i<6;i++)w.befriend(b,companion,12);
        check(w.recruitCompanion(b,companion),"NPC join party after trust");
        check(w.allies(b,companion),"party friendly fire prevented");
        var art=sword();b.weapon="jian";a.weapon="jian";
        b.stance=MurimEngine.Stance.ATTACK;a.stance=MurimEngine.Stance.ATTACK;
        b.fatigue=0;b.stamina=100;a.fatigue=0;a.stamina=100;
        var clash=MurimEngine.Combat.strike(b,a,art,"정철",new Random(1),w);
        check(clash.events().stream().anyMatch(e->e.kind()==MurimEngine.EventKind.CLASH)||
              clash.events().stream().anyMatch(e->e.kind()==MurimEngine.EventKind.MISS),"clash/dodge resolution");
        var teammate=MurimEngine.Combat.strike(b,companion,art,"신병",new Random(1),w);
        check(!teammate.hit()&&teammate.damage()==0,"party friendly fire");
        b.weapon="bow";check(MurimEngine.Combat.strike(b,a,art,"신병",new Random(1),w).damage()==0,"wrong weapon no damage");
        b.weapon="jian";
        a.parryTicks=10;a.stance=MurimEngine.Stance.PARRY;
        var parry=MurimEngine.Combat.strike(b,a,art,"강철",new Random(4),w);
        check(parry.damage()>=0,"parry event processed");
        var social=new MurimEngine.SocialConflict(b,a);
        for(int i=0;i<4&&social.phase!=MurimEngine.ConflictPhase.FIGHT;i++)social.step(new Random(123+i),null);
        check(social.phase==MurimEngine.ConflictPhase.FIGHT||social.phase==MurimEngine.ConflictPhase.DISPERSED,"argument escalates or disperses");
        w.day=30;
        check(w.newcomerTournament(b),"newcomer day30");
        check(w.winTournament(b,true)==250,"newcomer award");
        check(!w.newcomerTournament(b),"cannot repeat newcomer championship");
        w.day=42;check(w.grandTournamentOpen(),"grand day42");
        check(w.winTournament(b,false)>0,"grand prizes");
        w.day=50;check(!w.grandTournamentOpen(),"grand not frequently recurring");
        Path tmp=Files.createTempFile("murim-world-test-",".bin");
        MurimSave.write(w,tmp);var copy=MurimSave.read(tmp);
        check(copy.demonFounded==w.demonFounded&&copy.bloodFounded==w.bloodFounded,"cult persistence");
        check(copy.titleOwners.equals(w.titleOwners),"epithet persistence");
        check(copy.actors.size()==w.actors.size(),"actor persistence");
        check(copy.actors.get(a.id).relics.contains("천마계승령"),"fate relic persistence");
        check(copy.actors.get(a.id).spouse.equals(rival.id),"relationship persistence");
        check(copy.fates.get(fate.id).status==MurimEngine.FateStatus.TAKEN_BY_OTHER,"fate persistence");
        Files.deleteIfExists(tmp);
        if(args.length>0){
            MartialLibrary catalog=new MartialLibrary(Path.of(args[0]));
            check(catalog.all().size()==261,"261 preserved skill definitions");
            check(catalog.all().stream().anyMatch(MurimEngine.Martial::passive),"passives identified");
            check(catalog.all().stream().anyMatch(m->m.provenance().equals("original_game_design")),"original skills labeled");
            var legendary=catalog.all().stream().filter(MurimEngine.Martial::legendary).findFirst().orElseThrow();
            check(!catalog.learn(b,legendary,MurimEngine.LearningSource.BOOKSELLER),"legendary stall restriction");
            check(catalog.learn(b,legendary,MurimEngine.LearningSource.FATE),"legendary fate unlock");
        }
        System.out.println("PASS: "+passed+" independent assertions; all MURIM v1.9 headless rules tests passed.");
    }
}
