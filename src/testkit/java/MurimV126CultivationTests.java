import kr.murimlife.cult.CultMentorRules;
import kr.murimlife.qi.QiDeviationRules;
import kr.murimlife.medicine.ElixirCatalog;
import java.util.*;
public final class MurimV126CultivationTests {
    private static int n;
    private static void check(boolean ok,String msg){n++;if(!ok)throw new AssertionError(msg);}
    public static void main(String[] ignored){
        check(ElixirCatalog.all().size()==24,"24 unique plants and elixirs");
        check(ElixirCatalog.recipes().size()==23,"23 craftable, 1 unknown merchant-only");
        Set<String> ids=new HashSet<>(),names=new HashSet<>();
        for(var f:ElixirCatalog.all()){
            check(ids.add(f.id()),"distinct medicine item id");check(names.add(f.title()),"distinct Korean display name");
            check(f.price()>0&&f.countA()>0&&f.countB()>0,"valid alchemy amounts");
            check(!f.ingredientA().isBlank()&&!f.ingredientB().isBlank(),"real ingredient IDs");
        }
        for(int realm=0;realm<8;realm++){
            check(CultMentorRules.admissionChance(realm)>=90,"cults prioritize disciples");
            for(int masterRealm=4;masterRealm<=7;masterRealm++){
                check(CultMentorRules.apprenticeChance(masterRealm,realm,0)>=58,"mentorship is attainable");
                check(CultMentorRules.lessonsRequired(masterRealm,realm)>=5,"never instantly learn arts");
                check(CultMentorRules.hostileRefusalChance(0)<=3,"hostile refusal is rare");
                check(!CultMentorRules.mastered(1,10,7,4),"no early cult manual");
                check(CultMentorRules.mastered(10,10,7,4),"mastery check allows completion");
            }
        }
        for(int strain=0;strain<=100;strain+=5)for(int fatigue=0;fatigue<=100;fatigue+=5){
            int risk=QiDeviationRules.risk(strain,fatigue,15,3,false);
            check(risk>=0&&risk<=100,"bounded risk");
            check(QiDeviationRules.chance(risk)>=0&&QiDeviationRules.chance(risk)<=46,"never guaranteed deviation");
            check(QiDeviationRules.severity(risk)>=1&&QiDeviationRules.severity(risk)<=4,"severity bounds");
        }
        check(QiDeviationRules.chance(QiDeviationRules.risk(0,0,15,0,true))==0,"safe breakthrough never triggers");
        check(QiDeviationRules.chance(QiDeviationRules.risk(100,65,6,6,true))>0,"overdose is dangerous");
        for(int s=1;s<=4;s++){
            check(QiDeviationRules.rounds(s)==s+2,"more severe means more stages");
            check(QiDeviationRules.aftereffectTicks(s,0)>0,"there are aftereffects");
            check(QiDeviationRules.insight(s,0)>QiDeviationRules.insight(s,1),"mistakes reduce insight");
        }
        for(long seed=0;seed<200;seed++)for(int phase=0;phase<6;phase++)
            check(QiDeviationRules.answer(seed,phase)>=0&&QiDeviationRules.answer(seed,phase)<3,"three legal QTE responses");
        System.out.println("PASS v1.26 independent rules: "+n+" checks");
    }
}
