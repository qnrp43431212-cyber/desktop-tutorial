package kr.murimlife.npc.combat;
import kr.murimlife.npc.MurimHumanoid;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import java.util.Map;
import java.util.WeakHashMap;
/** Server-authoritative clash; narrow PvP/PvE scope to Murim NPCs, not vanilla combat globally. */
public final class MurimClashEvents {
    private static final Map<ServerLevel,ClashRules.Journal> JOURNALS=new WeakHashMap<>();
    private MurimClashEvents(){}
    private static ClashRules.Journal journal(ServerLevel s){return JOURNALS.computeIfAbsent(s,k->new ClashRules.Journal());}
    public static void recordAttack(LivingEntity e){
        if(!(e.level() instanceof ServerLevel world))return;
        if(e instanceof MurimHumanoid npc){
            var tag=npc.getPersistentData();
            tag.putFloat("MurimFatigue",Math.min(100f,tag.getFloat("MurimFatigue")+1.6f));
        }
        var look=e.getLookAngle();
        journal(world).record(e.getUUID(),new ClashRules.Strike(world.getGameTime(),e.getX(),e.getZ(),look.x,look.z));
    }
    public static void onPlayerSwing(AttackEntityEvent e){
        if(e.getEntity().level() instanceof ServerLevel && e.getTarget() instanceof LivingEntity
            && (e.getTarget() instanceof MurimHumanoid))recordAttack(e.getEntity());
    }
    private static boolean weapon(LivingEntity e){
        var held=e.getMainHandItem();
        return held.getItem() instanceof SwordItem || held.getItem() instanceof AxeItem;
    }
    public static void onIncomingDamage(LivingIncomingDamageEvent e){
        LivingEntity defender=e.getEntity();
        if(!(defender.level() instanceof ServerLevel world))return;
        if(!(e.getSource().getEntity() instanceof LivingEntity attacker))return;
        if(!(defender instanceof MurimHumanoid || attacker instanceof MurimHumanoid))return;
        if(!weapon(attacker)||!weapon(defender))return;
        if(journal(world).evaluate(attacker.getUUID(),defender.getUUID(),world.getGameTime(),
            attacker.distanceTo(defender),true)){
            e.setCanceled(true);
            double x=(attacker.getX()+defender.getX())/2,y=(attacker.getY()+defender.getY())/2+1.1,
                z=(attacker.getZ()+defender.getZ())/2;
            world.sendParticles(ParticleTypes.CRIT,x,y,z,10,.2,.15,.2,.08);
            world.sendParticles(ParticleTypes.ENCHANTED_HIT,x,y,z,6,.22,.2,.22,.06);
        }
    }
}
