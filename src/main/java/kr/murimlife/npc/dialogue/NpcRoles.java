package kr.murimlife.npc.dialogue;

import kr.murimlife.npc.MurimHumanoid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.npc.VillagerProfession;

/** One classifier for information quality, commerce, and theft across supported NPCs. */
public final class NpcRoles {
    public enum Role { RESIDENT, WAITER, MERCHANT, WARRIOR }
    private NpcRoles(){}
    public static boolean supported(LivingEntity entity){return entity instanceof MurimHumanoid || entity instanceof Villager || entity instanceof WanderingTrader;}
    public static Role role(LivingEntity entity){
        if(entity instanceof WanderingTrader)return Role.MERCHANT;
        if(entity instanceof Villager villager)return villager.getVillagerData().getProfession()==VillagerProfession.NONE?Role.RESIDENT:Role.MERCHANT;
        if(!(entity instanceof MurimHumanoid))return Role.RESIDENT;
        String name=entity.getPersistentData().getString("MurimRole").toLowerCase(java.util.Locale.ROOT);
        if(name.contains("inn_waiter")||name.contains("innkeeper")||name.contains("tea_master"))return Role.WAITER;
        if(name.contains("merchant")||name.contains("peddler")||name.contains("blacksmith")||name.contains("armorer")||name.contains("apothecary")||name.contains("banker")||name.contains("caravan")||name.contains("artisan")||name.contains("alchemist"))return Role.MERCHANT;
        if(name.contains("disciple")||name.contains("warrior")||name.contains("guard")||name.contains("elder")||name.contains("leader")||name.contains("head")||name.contains("patrol"))return Role.WARRIOR;
        return Role.RESIDENT;
    }
    public static String tradeCategory(LivingEntity entity){
        if(entity instanceof WanderingTrader)return "travel";
        if(entity instanceof Villager v){
            String name=v.getVillagerData().getProfession().toString();
            if(name.contains("farmer")||name.contains("fisherman")||name.contains("butcher"))return "food";
            if(name.contains("weapon")||name.contains("tool")||name.contains("armorer"))return "weapon";
            if(name.contains("cleric")||name.contains("cartographer")||name.contains("librarian"))return "scroll";
            return "sundry";
        }
        String role=entity.getPersistentData().getString("MurimRole");
        if(role.contains("blacksmith")||role.contains("armorer")||role.contains("artisan"))return "weapon";
        if(role.contains("apothecary")||role.contains("alchemist"))return "herb";
        if(role.contains("inn")||role.contains("tea"))return "food";
        return "sundry";
    }
}
