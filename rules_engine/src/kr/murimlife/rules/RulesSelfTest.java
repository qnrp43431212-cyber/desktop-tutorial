package kr.murimlife.rules;
import java.util.Set;
public final class RulesSelfTest {
    private static void yes(boolean b,String message) {if (!b) throw new AssertionError(message);}
    public static void main(String[]args) {
        for(int i=-300;i<=300;i++){
            var s=CultRules.fromRoll(i);
            yes(CultRules.exists(s,CultRules.Cult.DEMON) || CultRules.exists(s,CultRules.Cult.BLOOD),"empty world");
        }
        yes(!CultRules.canFound(true,true,true),"found already existing cult");
        yes(!CultRules.canFound(false,true,false),"missing trial");
        yes(CultRules.canFound(false,true,true),"valid founding");
        yes(CultRules.canClaimLeadership(false,true),"leadership duel");
        var active=new MartialRules.Art("검법",false,"sword",Set.of("saber"));
        var passive=new MartialRules.Art("심법",true,null,Set.of());
        yes(MartialRules.compatibility(active,"sword")==MartialRules.Match.OPTIMAL,"optimal");
        yes(MartialRules.compatibility(active,"saber")==MartialRules.Match.SIMILAR,"similar");
        yes(MartialRules.compatibility(active,"spear")==MartialRules.Match.INVALID,"invalid");
        yes(!MartialRules.canBind(passive),"passive bound");
        yes(MartialRules.normalizeWheelIndex(-1)==11,"wrap wheel");
        System.out.println("Rules self test PASS (cult presence, founding, leadership, weapon, passive, wheel)");
    }
}
