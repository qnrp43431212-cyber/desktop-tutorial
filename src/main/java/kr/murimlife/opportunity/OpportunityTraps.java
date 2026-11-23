package kr.murimlife.opportunity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Distinct one-shot proximity traps at the treasure's heart (in addition to permanent world hazards). */
public final class OpportunityTraps {
    private static long ticks;
    private OpportunityTraps(){}
    public static void tick(ServerTickEvent.Post event){
        if(++ticks%10!=0)return;
        var level=event.getServer().overworld();var data=OpportunitySites.get(level);
        for(ServerPlayer p:level.players()){
            for(var site:data.all()){
                long dx=(long)p.getBlockX()-site.x(),dz=(long)p.getBlockZ()-site.z();
                if(dx*dx+dz*dz>16||Math.abs(p.getBlockY()-site.y())>3)continue;
                String key="MurimTrap_"+site.key();
                if(p.getPersistentData().getBoolean(key))continue;
                p.getPersistentData().putBoolean(key,true);
                int trap=OpportunitySites.KINDS.get(site.type()).trap();
                var at=new BlockPos(site.x(),site.y(),site.z());
                switch(trap){
                    case 0 -> {
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,110,1));
                        if(level.getBlockState(at.offset(1,0,0)).isAir())level.setBlock(at.offset(1,0,0),Blocks.COBWEB.defaultBlockState(),2);
                        say(p,"거미줄 봉인진이 작동하여 발이 묶였습니다!");
                    }
                    case 1 -> {
                        p.hurt(level.damageSources().generic(),4f);
                        say(p,"발밑의 함정 기관이 튀어나와 상처를 입혔습니다!");
                    }
                    case 2 -> {
                        p.addEffect(new MobEffectInstance(MobEffects.POISON,150,0));
                        say(p,"숨겨진 독분이 터져 중독되었습니다!");
                    }
                    case 3 -> {
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,180,2));
                        say(p,"한기가 폭발하며 발이 얼어붙었습니다!");
                    }
                    case 4 -> {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,0));
                        p.hurt(level.damageSources().generic(),2f);
                        say(p,"봉인실의 환영진이 작동했습니다!");
                    }
                    case 5 -> {
                        p.hurt(level.damageSources().generic(),6f);
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0));
                        say(p,"매복된 기관노가 작동해 화살비가 쏟아집니다!");
                    }
                }
                level.sendParticles(ParticleTypes.CRIT,p.getX(),p.getY()+1,p.getZ(),28,.75,1,.75,.1);
            }
        }
    }
    private static void say(ServerPlayer p,String line){p.sendSystemMessage(Component.literal("[기연 함정] "+line));}
}
