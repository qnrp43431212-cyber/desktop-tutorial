import kr.murimlife.cult.CultRankRules;
public class MurimV125CultTests {
  public static void main(String[] args){
    int count=0;
    for(String rank:new String[]{"외문제자","내문제자","사부","장로","가주","호법","교주"}){
        int requirement=CultRankRules.minimum(rank);
        for(int realm=0;realm<=7;realm++){
            if(CultRankRules.eligible(rank,realm)!=(realm>=requirement))throw new AssertionError(rank+realm);
            count++;
        }
    }
    for(int vacancy=0;vacancy<12;vacancy++){
      int[] realms=new int[12];java.util.Arrays.fill(realms,-1);
      for(int i=vacancy+1;i<12;i++)realms[i]=6;
      int result=CultRankRules.qualifiedSuccessor(vacancy,realms);
      if(result!=(vacancy<11?vacancy+1:-1))throw new AssertionError("succession seat:"+vacancy);
      count++;
    }
    if(CultRankRules.eligible("교주",6)||!CultRankRules.eligible("교주",7))throw new AssertionError();
    if(CultRankRules.minimum("not-a-rank")!=8)throw new AssertionError();
    System.out.println("PASS: "+count+" rank / succession rule checks");
  }
}
