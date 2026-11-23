package kr.murimlife.npc;
public final class NpcLifecycleRulesTest {
    static int count;
    static void check(boolean value){count++;if(!value)throw new AssertionError("NPC lifecycle test "+count);}
    public static void main(String[] unused){
        var life=NpcLifecycleRules.Residence.LOADED_ALIVE;
        check(NpcLifecycleRules.onChunkUnload(life)==NpcLifecycleRules.Residence.UNLOADED_SAVED);
        check(NpcLifecycleRules.onChunkLoad(NpcLifecycleRules.Residence.UNLOADED_SAVED)==life);
        check(NpcLifecycleRules.onChunkLoad(NpcLifecycleRules.Residence.DEAD)==NpcLifecycleRules.Residence.DEAD);
        check(NpcLifecycleRules.onChunkUnload(NpcLifecycleRules.Residence.DEAD)==NpcLifecycleRules.Residence.DEAD);
        check(NpcLifecycleRules.allowNewResident(true,true,true,true,30,160,10,24));
        check(!NpcLifecycleRules.allowNewResident(false,true,true,true,30,160,10,24));
        check(!NpcLifecycleRules.allowNewResident(true,false,true,true,30,160,10,24));
        check(!NpcLifecycleRules.allowNewResident(true,true,false,true,30,160,10,24));
        check(!NpcLifecycleRules.allowNewResident(true,true,true,false,30,160,10,24));
        check(!NpcLifecycleRules.allowNewResident(true,true,true,true,160,160,10,24));
        check(!NpcLifecycleRules.allowNewResident(true,true,true,true,30,160,24,24));
        check(NpcLifecycleRules.allowNewResident(true,true,true,true,30,160,23,24));
        check(NpcLifecycleRules.tournamentCrowdSize(48,444,80)==48);
        check(NpcLifecycleRules.tournamentCrowdSize(300,444,120)==80);
        check(NpcLifecycleRules.tournamentCrowdSize(64,35,80)==35);
        check(NpcLifecycleRules.tournamentCrowdSize(0,444,80)==0);
        try{NpcLifecycleRules.tournamentCrowdSize(-1,10,80);throw new AssertionError("invalid accepted");}
        catch(IllegalArgumentException expected){count++;}
        try{NpcLifecycleRules.allowNewResident(true,true,true,true,0,-1,0,2);throw new AssertionError("negative accepted");}
        catch(IllegalArgumentException expected){count++;}
        System.out.println("NPC persistence + arena rules: "+count+" TESTS PASS");
    }
}
