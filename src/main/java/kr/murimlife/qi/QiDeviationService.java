package kr.murimlife.qi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.murimlife.MurimProgressData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Event-driven heart-devil crisis: triggered only by elevated cultivation risk, not every breakthrough. */
public final class QiDeviationService {
    private QiDeviationService(){}
    private static int tickCounter;
    private static void tell(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("[주화입마] "+s));}
    private static MurimProgressData.Profile profile(ServerPlayer p){return MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());}
    public static boolean checkRisk(ServerPlayer p,boolean forcing,int pendingRealm){
        var data=MurimProgressData.get(p.server);var pr=profile(p);
        if(pr.qiSeverity>0){tell(p,"이미 주화입마 상태입니다. 먼저 기를 안정시키십시오.");return true;}
        int risk=QiDeviationRules.risk(pr.qiStrain,pr.fatigue,pr.attributes.getOrDefault("정신력",10.0),pr.realm,forcing);
        int chance=QiDeviationRules.chance(risk);
        if(p.getRandom().nextInt(100)>=chance)return false;
        pr.qiSeverity=QiDeviationRules.severity(risk);pr.qiStage=0;pr.qiMistakes=0;
        pr.qiPendingRealm=pendingRealm;pr.qiDeadline=0;
        pr.qiSeed=p.getRandom().nextLong();pr.qiLastRecoveryTick=-1;
        pr.fatigue=Math.min(100,pr.fatigue+12);data.setDirty();
        tell(p,"기혈이 역류하여 "+pr.qiSeverity+"단계 주화입마에 빠졌습니다. 평소보다 강한 영단 복용·무리한 운기가 원인일 수 있습니다. /murim qi confront 로 심마를 마주하세요.");
        return true;
    }
    public static void onBreathing(ServerPlayer p){
        var pr=profile(p);
        if(pr.qiSeverity>0){tell(p,"주화입마 중에는 운기조식으로 기맥을 강제로 돌릴 수 없습니다.");return;}
        // Ordinary gentle circulation is safe; overdose above the threshold makes forced circulation risky.
        if(pr.qiStrain>=45)checkRisk(p,true,-1);
    }
    private static final String[] CLUES={"심화가 상단전에서 타오릅니다", "음한한 기운이 혈맥을 막고 있습니다", "탁기가 역류해 임독양맥이 흔들립니다"};
    private static final String[] CHOICES={"호흡을 늦춰 심화를 잠재운다", "온기를 하단전에 모아 한기를 녹인다", "역류한 탁기를 밖으로 순환시킨다"};
    private static int correct(MurimProgressData.Profile p){return QiDeviationRules.answer(p.qiSeed,p.qiStage);}
    private static void show(ServerPlayer p,MurimProgressData.Profile pr){
        if(pr.qiSeverity<=0||pr.qiDeadline<=0)return;
        tell(p,"심마 "+(pr.qiStage+1)+"/"+QiDeviationRules.rounds(pr.qiSeverity)+" 단계 · "+CLUES[correct(pr)]+". 14초 안에 대응하십시오.");
        for(int i=0;i<3;i++){
            final int choice=i+1;
            p.sendSystemMessage(Component.literal("["+choice+". "+CHOICES[i]+"]").withStyle(s->s.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/murim qi choose "+choice))));
        }
    }
    private static void fail(ServerPlayer p,MurimProgressData.Profile pr,String why){
        long now=p.serverLevel().getGameTime();
        pr.qiSeverity=Math.min(4,pr.qiSeverity+1);pr.qiDeadline=0;pr.qiStage=0;
        pr.qiAftereffectUntil=Math.max(pr.qiAftereffectUntil,now+QiDeviationRules.aftereffectTicks(pr.qiSeverity,pr.qiMistakes));
        pr.qiLastRecoveryTick=now;pr.qiStrain=Math.min(100,pr.qiStrain+6);
        pr.fatigue=Math.min(100,pr.fatigue+14);
        pr.realmProgress=Math.max(0,pr.realmProgress-5*pr.qiSeverity);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,500,1));
        MurimProgressData.get(p.server).setDirty();
        tell(p,why+" 후유증이 심해졌습니다. 기운을 안정시킨 뒤 다시 시도해야 합니다.");
    }
    public static void confront(ServerPlayer p){
        var pr=profile(p);long now=p.serverLevel().getGameTime();
        if(pr.qiSeverity<=0){tell(p,"지금은 주화입마가 없습니다. 깨달음 "+pr.insight+" · 영단 부담 "+pr.qiStrain+"/100.");return;}
        if(pr.qiDeadline>now){show(p,pr);return;}
        if(pr.qiLastRecoveryTick>=0&&now-pr.qiLastRecoveryTick<200){tell(p,"심신이 가라앉을 때까지 잠시 쉬어야 합니다.");return;}
        pr.qiStage=0;pr.qiMistakes=0;pr.qiDeadline=now+280;
        MurimProgressData.get(p.server).setDirty();show(p,pr);
    }
    public static void choose(ServerPlayer p,int choice){
        var data=MurimProgressData.get(p.server);var pr=profile(p);long now=p.serverLevel().getGameTime();
        if(pr.qiSeverity<=0||pr.qiDeadline<=0||now>pr.qiDeadline){tell(p,"심마 대응 시간이 끝났거나 치료가 시작되지 않았습니다.");return;}
        if(choice<1||choice>3)return;
        if(choice-1!=correct(pr)){
            pr.qiMistakes++;if(pr.qiMistakes>=2){fail(p,pr,"잘못된 기운을 두 번 운용해 심마가 폭주했습니다.");return;}
            pr.qiDeadline=now+240;data.setDirty();tell(p,"기혈이 다시 흔들립니다. 실수 1회. 한 번 더 잘못되면 주화입마가 악화됩니다.");show(p,pr);return;
        }
        pr.qiStage++;
        if(pr.qiStage>=QiDeviationRules.rounds(pr.qiSeverity)){
            int severity=pr.qiSeverity,insight=QiDeviationRules.insight(severity,pr.qiMistakes);
            pr.insight=Math.min(10000,pr.insight+insight);
            pr.realmProgress=Math.min(100,pr.realmProgress+severity*4);
            for(String art:pr.arts)pr.artMastery.merge(art,Math.min(30,4*severity),(old,add)->Math.min(100,old+add));
            pr.qiStrain=Math.max(0,pr.qiStrain-12-severity*4);
            pr.qiAftereffectUntil=now+QiDeviationRules.aftereffectTicks(severity,pr.qiMistakes);
            if(pr.qiPendingRealm>pr.realm && pr.qiPendingRealm<=7){pr.realm=pr.qiPendingRealm;pr.realmProgress=10+severity*2;}
            pr.qiPendingRealm=-1;pr.qiSeverity=0;pr.qiStage=0;pr.qiDeadline=0;pr.qiLastRecoveryTick=now;
            pr.fatigue=Math.min(100,pr.fatigue+severity*5);data.setDirty();
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,240*severity,Math.min(2,severity/2)));
            tell(p,"주화입마 극복! 깨달음 +"+insight+", 배운 무공의 숙련도 상승. 강도 "+severity+"에 따른 후유증으로 내력 회복이 한동안 느려집니다. 현재 경지: "+pr.realmName());
        }else{pr.qiDeadline=now+280;data.setDirty();show(p,pr);}
    }
    public static void tick(ServerTickEvent.Post event){
        if(++tickCounter%200!=0)return;
        for(ServerPlayer p:event.getServer().getPlayerList().getPlayers()){
            var data=MurimProgressData.get(p.server);var pr=data.player(p.getUUID(),p.getName().getString());
            long now=p.serverLevel().getGameTime();
            if(pr.qiSeverity>0){
                if(pr.qiDeadline>0&&now>pr.qiDeadline){fail(p,pr,"심마 대응에 실패했습니다.");continue;}
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,250,Math.min(2,pr.qiSeverity-1)));
            }else if(now<pr.qiAftereffectUntil){
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,250,0));
            }
            if(pr.qiStrain>0 && (pr.qiLastDoseTick<0 || now-pr.qiLastDoseTick>1200)){
                pr.qiStrain=Math.max(0,pr.qiStrain-1);data.setDirty();
            }
        }
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("murim").then(Commands.literal("qi")
            .then(Commands.literal("confront").executes(ctx->{confront(ctx.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("choose").then(Commands.argument("choice",IntegerArgumentType.integer(1,3)).executes(ctx->{
                choose(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"choice"));return 1;})))
            .executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();var pr=profile(p);
                tell(p,"주화입마 "+pr.qiSeverity+"단계, 영단 부담 "+pr.qiStrain+", 깨달음 "+pr.insight+
                    ", 후유증 "+Math.max(0,(pr.qiAftereffectUntil-p.serverLevel().getGameTime())/20)+"초, 무공 숙련 " +pr.artMastery);return 1;})
        ));
    }
}
