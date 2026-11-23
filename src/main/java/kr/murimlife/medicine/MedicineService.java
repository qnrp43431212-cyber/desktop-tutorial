package kr.murimlife.medicine;

import kr.murimlife.MurimProgressData;
import kr.murimlife.qi.QiDeviationService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.*;

/** More powerful pellets carry more unstable internal energy; unknown pills hide the formula until swallowed. */
public final class MedicineService {
    private MedicineService(){}
    public static boolean take(ServerPlayer player,String id){
        var formula=ElixirCatalog.get(id);
        if(formula==null)return false;
        var d=MurimProgressData.get(player.server);var pr=d.player(player.getUUID(),player.getName().getString());
        long now=player.serverLevel().getGameTime();
        boolean calming=Set.of("clear_mind_pill","soul_stilling_pill","purifying_pill").contains(id);
        if(pr.qiSeverity>0){
            if(!calming){player.sendSystemMessage(Component.literal("[영약] 주화입마 중에는 강한 영단을 복용할 수 없습니다. 청심단·안혼단·정기단으로 기운을 진정시킬 수 있습니다."));return false;}
            if(pr.qiLastDoseTick>=0 && now-pr.qiLastDoseTick<2400){
                player.sendSystemMessage(Component.literal("[영약] 정신안정 영단을 너무 빨리 재복용하면 위험합니다."));return false;
            }
            pr.qiStrain=Math.max(0,pr.qiStrain-20);
            pr.fatigue=Math.max(0,pr.fatigue-8);
            pr.qiAftereffectUntil=Math.max(now,pr.qiAftereffectUntil-4000);
            pr.qiLastRecoveryTick=-1;
        }
        ElixirCatalog.Formula effective=formula;
        if("unknown_elixir".equals(id)){
            var pool=ElixirCatalog.recipes();effective=pool.get(player.getRandom().nextInt(pool.size()));
        }
        int dose=effective.strain();
        if(pr.qiLastDoseTick>=0&&now-pr.qiLastDoseTick<2400)dose+=9; // stacking within two in-game minutes
        pr.qiStrain=Math.min(100,pr.qiStrain+dose);
        pr.qiLastDoseTick=now;
        pr.inner=Math.min(100,Math.max(0,pr.inner+effective.inner()));
        pr.fatigue=Math.clamp(pr.fatigue+effective.fatigue(),0,100);
        pr.insight=Math.min(10000,pr.insight+effective.insight());
        if("clear_mind_pill".equals(id)||"soul_stilling_pill".equals(id)||"purifying_pill".equals(id))
            pr.qiStrain=Math.max(0,pr.qiStrain-13);
        if(pr.qiStrain>=75){
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,220,0));
            pr.fatigue=Math.min(100,pr.fatigue+8);
        }
        d.setDirty();
        player.sendSystemMessage(Component.literal("[영약] "+formula.title()+" 복용"+
            ("unknown_elixir".equals(id)?" — 실제 성분은 "+effective.title()+"(으)로 드러났습니다.":"")+
            " · 내력 "+(int)pr.inner+" · 부담 "+pr.qiStrain+"/100. 과복용 상태에서 운기조식은 위험합니다."));
        return true;
    }
}
