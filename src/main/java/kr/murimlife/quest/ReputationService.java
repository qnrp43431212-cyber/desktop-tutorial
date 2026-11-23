package kr.murimlife.quest;

import kr.murimlife.MurimProgressData;
import kr.murimlife.economy.GovernanceRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Public recognition never awards karma. Masks hide faces, not the saved reputation value. */
public final class ReputationService {
    private static final Map<String,Long> SEEN=new HashMap<>();
    private static long ticks;
    private ReputationService(){}
    public static boolean disguised(ServerPlayer player){
        var helmet=player.getItemBySlot(EquipmentSlot.HEAD);
        if(helmet.isEmpty())return false;
        var id=BuiltInRegistries.ITEM.getKey(helmet.getItem());
        if(!"murimlife".equals(id.getNamespace()))return false;
        return id.getPath().equals("mask")||id.getPath().equals("full_hood")||id.getPath().equals("veil");
    }
    public static int buyingPrice(ServerPlayer player,int base){
        var pr=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        return Math.max(1,(base*(100-GovernanceRules.priceDiscountPercent(pr.reputation))+99)/100);
    }
    public static int sellingPrice(ServerPlayer player,int base){
        var pr=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        return Math.max(0,base*(100+Math.min(10,pr.reputation/150))/100);
    }
    public static int informationFee(ServerPlayer player,int base){return buyingPrice(player,base);}
    /** NPCs can recognize familiar faces in passing; only nearby loaded entities are inspected. */
    public static void tick(ServerTickEvent.Post event){
        if(++ticks%200!=0)return;
        for(ServerPlayer player:event.getServer().getPlayerList().getPlayers()){
            var pr=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
            if(pr.reputation<100||disguised(player))continue;
            int checked=0;
            for(LivingEntity npc:player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(6.0),npc->
                    npc!=player&&kr.murimlife.npc.dialogue.NpcRoles.supported(npc))){
                if(checked++>=4)break;
                recognize(player,npc);
            }
        }
    }
    public static void recognize(ServerPlayer player,LivingEntity npc){
        if(disguised(player))return;
        var pr=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        int chance=GovernanceRules.recognitionPercent(pr.reputation);
        if(chance==0)return;
        long day=player.serverLevel().getGameTime()/24000L;
        String key=player.getUUID()+"/"+npc.getUUID();
        if(SEEN.getOrDefault(key,-1L)==day)return;
        if(SEEN.size()>20000)SEEN.clear();SEEN.put(key,day);
        int roll=Math.floorMod(Objects.hash(player.getUUID(),npc.getUUID(),day),100);
        if(roll<chance)player.sendSystemMessage(Component.literal("[강호 평판] "+npc.getDisplayName().getString()+": 듣자 하니 강호에 이름을 알린 분이시군요."));
    }
}
