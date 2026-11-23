import java.util.UUID;
import kr.murimlife.npc.dialogue.DialogueRules;
import kr.murimlife.npc.combat.WeaponTactics;
/** Java-only v1.22 regressions; does not claim Minecraft/Epic Fight runtime testing. */
public final class MurimV122PureTests {
    private static int passed;
    private static void ok(boolean condition,String name){
        if(!condition)throw new AssertionError(name);
        passed++;
    }
    public static void main(String... args) {
        ok(DialogueRules.COMBAT_CHOICE_TICKS==300,"15-second choice window");
        ok(DialogueRules.OFFER_TICKS==200,"10-second offer window");
        ok(DialogueRules.choiceValid(0)&&DialogueRules.choiceValid(5),"choice boundaries");
        ok(!DialogueRules.choiceValid(-1)&&!DialogueRules.choiceValid(6),"invalid choice rejected");
        ok(!DialogueRules.expired(true,299,300),"combat choice still active");
        ok(DialogueRules.expired(true,300,300),"combat choice expires on deadline");
        ok(!DialogueRules.expired(false,999999,300),"peaceful choice never expires");
        ok(DialogueRules.mayTalk(36,true,false),"talk range inclusive six blocks");
        ok(!DialogueRules.mayTalk(36.01,true,false),"talk range cannot exceed six blocks");
        ok(!DialogueRules.mayTalk(4,true,true),"spectator cannot talk");
        ok(!DialogueRules.mayTalk(4,false,false),"dead NPC cannot talk");
        ok(Math.abs(DialogueRules.recoverFatigue(8,1.2)-6.8)<1e-8,"recover fatigue");
        ok(DialogueRules.recoverFatigue(0,1.2)==0,"fatigue never negative");
        ok(DialogueRules.recoverFatigue(130,0)==100,"fatigue clamped 100");
        ok(DialogueRules.recoverFatigue(10,-99)==10,"negative recovery does not injure");
        UUID player=UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID npc=UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID outsider=UUID.fromString("00000000-0000-0000-0000-000000000003");
        ok(DialogueRules.pairedDamage(player,npc,player,npc),"NPC attacks player during dialogue");
        ok(DialogueRules.pairedDamage(player,npc,npc,player),"player attacks NPC during dialogue");
        ok(!DialogueRules.pairedDamage(player,npc,player,player),"player self-damage must continue");
        ok(!DialogueRules.pairedDamage(player,npc,npc,npc),"NPC self-damage must continue");
        ok(!DialogueRules.pairedDamage(player,npc,player,outsider),"third-party attack must continue");
        ok(!DialogueRules.pairedDamage(player,npc,player,null),"environmental damage must continue");
        var styles=WeaponTactics.Style.values();
        ok(styles.length==8,"eight weapon behavior families");
        ok(WeaponTactics.fromItemId("murimlife:weapon_jian")==WeaponTactics.Style.SWORD,"jian");
        ok(WeaponTactics.fromItemId("murimlife:weapon_dao")==WeaponTactics.Style.SABER,"dao");
        ok(WeaponTactics.fromItemId("murimlife:weapon_spear")==WeaponTactics.Style.SPEAR,"spear");
        ok(WeaponTactics.fromItemId("murimlife:weapon_staff")==WeaponTactics.Style.STAFF,"staff");
        ok(WeaponTactics.fromItemId("murimlife:weapon_dagger")==WeaponTactics.Style.DAGGER,"dagger");
        ok(WeaponTactics.fromItemId("murimlife:weapon_dual")==WeaponTactics.Style.DUAL,"dual");
        ok(WeaponTactics.fromItemId("murimlife:weapon_hammer")==WeaponTactics.Style.HEAVY,"heavy");
        ok(WeaponTactics.fromItemId("minecraft:air")==WeaponTactics.Style.FIST,"fist");
        for(var style:styles){
            var b=WeaponTactics.behavior(style);
            ok(b.reach()>0&&b.minCooldown()>0&&b.maxCooldown()>=b.minCooldown(),"valid behavior "+style);
        }
        System.out.println("PASS "+passed+" v1.22 pure Java assertions");
    }
}
