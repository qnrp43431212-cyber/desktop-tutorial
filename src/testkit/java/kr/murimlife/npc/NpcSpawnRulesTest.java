package kr.murimlife.npc;

public final class NpcSpawnRulesTest {
    private static int checked;
    private static void ok(boolean b) { checked++; if (!b) throw new AssertionError("NPC spawn preparation check #" + checked); }
    public static void main(String[] unused) {
        var a=NpcSpawnRules.resolve(100,65,-120,new NpcSpawnRules.Offset(12,2,24));
        ok(a.x()==112 && a.y()==67 && a.z()==-96);
        var id=NpcSpawnRules.stableId("minecraft:overworld","shaolin@12,15",4,0);
        ok(id.equals(NpcSpawnRules.stableId("minecraft:overworld","shaolin@12,15",4,0)));
        ok(!id.equals(NpcSpawnRules.stableId("minecraft:overworld","shaolin@12,15",4,1)));
        ok(!id.equals(NpcSpawnRules.stableId("minecraft:overworld","wudang@12,15",4,0)));
        ok(!NpcSpawnRules.ready("PIECES",true,true,true,true,true,0,12,0,100));
        ok(!NpcSpawnRules.ready("COMPLETE",false,true,true,true,true,0,12,0,100));
        ok(!NpcSpawnRules.ready("COMPLETE",true,false,true,true,true,0,12,0,100));
        ok(!NpcSpawnRules.ready("COMPLETE",true,true,false,true,true,0,12,0,100));
        ok(!NpcSpawnRules.ready("COMPLETE",true,true,true,false,true,0,12,0,100));
        ok(!NpcSpawnRules.ready("COMPLETE",true,true,true,true,false,0,12,0,100));
        ok(!NpcSpawnRules.ready("COMPLETE",true,true,true,true,true,12,12,0,100));
        ok(!NpcSpawnRules.ready("COMPLETE",true,true,true,true,true,0,12,100,100));
        ok(NpcSpawnRules.ready("COMPLETE",true,true,true,true,true,0,12,0,100));
        ok(NpcSpawnRules.remaining(7,10)==3);
        ok(NpcSpawnRules.remaining(11,10)==0);
        try {NpcSpawnRules.stableId("","a",1,1);throw new AssertionError("Blank dimension accepted");}
        catch (IllegalArgumentException expected) {checked++;}
        System.out.println("NPC spawn readiness: " + checked + " tests PASS");
    }
}
