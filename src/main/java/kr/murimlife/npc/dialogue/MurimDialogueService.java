package kr.murimlife.npc.dialogue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.murimlife.MurimProgressData;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.economy.MerchantService;
import kr.murimlife.economy.TheftService;
import kr.murimlife.npc.NpcArenaService;
import kr.murimlife.npc.combat.WeaponTactics;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Server-only state machine; clickable chat options, no opaque client authority.
 * Combat conversations pause only the paired NPC; peaceful ones NEVER cause hostility.
 * Each answer has a new 15s deadline; no answer -> original fight resumes automatically.
 */
public final class MurimDialogueService {
    private static final class Session {
        final UUID playerId,npcId;
        final ServerLevel level;
        final int token;
        final boolean battle,oldNoAi;
        final UUID oldTarget;
        final Map<Integer,Set<Integer>> said=new HashMap<>();
        int turns,delayUses,infoUses,patience=4;
        long deadline;
        Session(ServerPlayer player,MurimHumanoid npc,int token,boolean battle){
            this.playerId=player.getUUID();this.npcId=npc.getUUID();this.level=player.serverLevel();
            this.token=token;this.battle=battle;this.oldNoAi=npc.isNoAi();
            this.oldTarget=npc.getTarget()==null?null:npc.getTarget().getUUID();
            this.deadline=battle?level.getGameTime()+DialogueRules.COMBAT_CHOICE_TICKS:Long.MAX_VALUE;
        }
    }
    private record Offer(UUID playerId,UUID npcId,ServerLevel level,long deadline,int token,boolean battle){}
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Map<UUID,Offer> OFFERS=new HashMap<>();
    private static final Map<UUID,Long> NPC_LAST_OFFER=new HashMap<>();
    private static final Map<UUID,Long> PLAYER_LAST_OFFER=new HashMap<>();
    private static final Map<UUID,Long> LAST_TILDE=new HashMap<>();
    private static final Random RNG=new Random();
    private static int nextToken=1;
    private static int ticks;
    private MurimDialogueService(){}

    private static String label(MurimHumanoid npc){return npc.getCustomName()==null?"무림인":npc.getCustomName().getString();}
    private static void tell(ServerPlayer p,String text){MurimPanelServer.notice(p,text);}
    private static Component clickable(String text,String command){
        return Component.literal("["+text+"]").withStyle(s->s.withColor(ChatFormatting.AQUA)
            .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,command)));
    }
    private static boolean spectator(MurimHumanoid npc){return npc.getPersistentData().getBoolean("MurimLifeSpectator");}
    private static boolean eligible(ServerPlayer p,MurimHumanoid n){
        return p.isAlive()&&!p.isSpectator()&&p.serverLevel()==n.level()&& DialogueRules.mayTalk(p.distanceToSqr(n),n.isAlive(),spectator(n));
    }
    private static boolean occupied(UUID npcId){
        for(Session s:SESSIONS.values())if(s.npcId.equals(npcId))return true;
        for(Offer o:OFFERS.values())if(o.npcId.equals(npcId))return true;
        return false;
    }
    private static boolean fighting(ServerPlayer p,MurimHumanoid npc){
        return npc.getTarget()==p || npc.isInDuel() && npc.getTarget()==p;
    }
    /** ~ key: aim at NPC to request; without a target, accepts the pending NPC invitation. */
    public static void onTilde(ServerPlayer player,int entityId){
        long now=player.serverLevel().getGameTime();
        if(now-LAST_TILDE.getOrDefault(player.getUUID(),-1000L)<10)return;
        LAST_TILDE.put(player.getUUID(),now);
        if(SESSIONS.containsKey(player.getUUID())){tell(player,"대화가 열려 있습니다. Alt를 누른 채 선택지를 클릭하십시오.");return;}
        Offer pending=OFFERS.get(player.getUUID());
        if(entityId<0 && pending!=null){respond(player,pending.token,true);return;}
        Entity entity=player.serverLevel().getEntity(entityId);
        if(!(entity instanceof MurimHumanoid)){
            if(CivicDialogueService.open(player,entityId))return;
            tell(player,"6블록 안의 주민·점소이·상인을 바라보고 ~ 키를 누르십시오.");return;
        }
        MurimHumanoid npc=(MurimHumanoid)entity;
        if(!eligible(player,npc)){tell(player,"대화 상대가 멀리 있습니다.");return;}
        if(pending!=null && pending.npcId.equals(npc.getUUID())){respond(player,pending.token,true);return;}
        if(occupied(npc.getUUID())){tell(player,"그 무림인은 이미 다른 대화를 진행 중입니다.");return;}
        if(npc.getTarget()!=null && npc.getTarget()!=player){
            tell(player,"상대가 다른 무인과 싸우고 있어 대화할 틈이 없습니다.");return;
        }
        boolean battle=fighting(player,npc);
        // The NPC may refuse an ill-timed interruption. Leaders are harder to persuade.
        int acceptance=battle? (npc.getHealth()<npc.getMaxHealth()*.35f?85:55):95;
        if(npc.getPersistentData().getBoolean("MurimLeader"))acceptance-=battle?20:10;
        if(RNG.nextInt(100)>=acceptance){
            tell(player,label(npc)+": "+(battle?"지금은 검으로 답하겠소. 말은 듣지 않겠소.":"지금은 다른 일이 있어 긴 이야기를 나누기 어렵습니다."));
            return;
        }
        if(!battle)kr.murimlife.quest.ReputationService.recognize(player,npc);
        begin(player,npc,battle);
    }
    private static void begin(ServerPlayer p,MurimHumanoid npc,boolean battle){
        if(!eligible(p,npc)||occupied(npc.getUUID())||SESSIONS.containsKey(p.getUUID()))return;
        // Also recheck here: an invitation may become stale when the NPC enters another fight.
        if(npc.getTarget()!=null && npc.getTarget()!=p){
            tell(p,"상대가 다른 무인과 싸우는 중이라 대화할 수 없습니다.");return;
        }
        Session s=new Session(p,npc,++nextToken,battle);
        SESSIONS.put(p.getUUID(),s);
        npc.getNavigation().stop();
        npc.setTarget(null);
        npc.getPersistentData().putBoolean("MurimLifeDialogueOldNoAi",s.oldNoAi);
        npc.getPersistentData().putBoolean("MurimLifeDialoguePause",true);
        npc.setNoAi(true);
        tell(p,label(npc)+": "+(battle?"검을 잠시 거두겠소. 서둘러 말하시오.":"무슨 일로 찾아오셨습니까?"));
        tell(p,battle?"전투 중 대화: 15초 안에 선택하십시오. 대화 중 피로도가 조금씩 회복됩니다.":
            "일상 대화: 제한 시간이 없으며 대화를 마쳐도 전투가 시작되지 않습니다.");
        showOptions(p,s);
    }
    private static void showOptions(ServerPlayer player,Session s){
        List<MurimPanelServer.Choice> options=new ArrayList<>();
        for(int i=0;i<6;i++){
            options.add(MurimPanelServer.option((i+1)+". "+DialogueRules.label(i,s.battle),
                "/murim talk choose "+s.token+" "+(i+1)));
        }
        if(!s.battle){
            String[] topics={"기연 소문","지금·다음 문파 모집","무림대회 지정일","각 문파·세가·무림맹·사도련 본부 위치",
                "상세 기연 정보","상점 열기","소매치기 시도","허위정보 추궁","무림 임무 문의","문파 NPC 영입 제안","문파 NPC 승급","문파 NPC 해임"};
            for(int i=0;i<topics.length;i++)options.add(MurimPanelServer.option((i+7)+". "+topics[i],
                "/murim talk choose "+s.token+" "+(i+7)));
            MurimHumanoid target=npc(s);
            if(target!=null&&!target.getPersistentData().getString("MurimCultPersonId").isBlank()){
                options.add(MurimPanelServer.option("19. 마교·혈교 입교 요청", "/murim talk choose "+s.token+" 19"));
                options.add(MurimPanelServer.option("20. 대마두 제자 요청", "/murim talk choose "+s.token+" 20"));
                options.add(MurimPanelServer.option("21. 사공 수련 요청", "/murim talk choose "+s.token+" 21"));
            }
        }
        String time=s.battle?"전투 중 협상 · 15초 안에 선택 · Alt + 마우스":"일상 대화 · Alt 누른 채 마우스 선택";
        MurimPanelServer.show(player,"무림인 · "+(npc(s)==null?"미확인":label(npc(s))),time,options);
        if(s.battle){
            int seconds=(int)Math.max(0,(s.deadline-s.level.getGameTime()+19)/20);
            player.displayClientMessage(Component.literal("[전투 대화] "+seconds+"초 남음"),true);
        }
    }
    public static void choose(ServerPlayer player,int token,int shownOption){
        Session s=SESSIONS.get(player.getUUID());
        int option=shownOption-1;
        if(s==null||s.token!=token||option<0||(s.battle?option>=6:option>=21)){
            tell(player,"만료된 선택지입니다. 새로운 대화 요청을 하십시오.");return;
        }
        MurimHumanoid npc=npc(s);
        if(npc==null||!eligible(player,npc)||DialogueRules.expired(s.battle,s.level.getGameTime(),s.deadline)){
            close(player,s,false,"거리·상황 변경 또는 시간 초과로 대화가 종료되었습니다.");return;
        }
        if(!s.battle && option>=6){
            if(option>=6 && option<=10)InformationService.ask(player,npc,switch(option){case 6->4;case 7->1;case 8->2;case 9->3;default->5;});
            else if(option==11){close(player,s,false,"상점을 열었습니다.");MerchantService.open(player,npc);return;}
            else if(option==12){close(player,s,false,"대화를 마치고 소매치기를 시도합니다.");TheftService.start(player,npc);return;}
            else if(option==13){close(player,s,false,"상인을 추궁합니다.");DisputeService.open(player,npc);return;}
            else if(option==14){close(player,s,false,"의뢰소를 열었습니다.");kr.murimlife.quest.QuestService.openFromNpc(player,npc);return;}
            else if(option==15)kr.murimlife.economy.NpcSectRoster.get(player.server).recruit(player,npc);
            else if(option==16)kr.murimlife.economy.NpcSectRoster.get(player.server).promote(player,npc);
            else if(option==17)kr.murimlife.economy.NpcSectRoster.get(player.server).release(player,npc);
            else if(option==18)kr.murimlife.cult.CultMentorshipService.join(player,npc);
            else if(option==19)kr.murimlife.cult.CultMentorshipService.ask(player,npc);
            else if(option==20)kr.murimlife.cult.CultMentorshipService.lesson(player,npc);
            if(SESSIONS.get(player.getUUID())==s && option!=11 && option!=13 && option!=14)showOptions(player,s);return;
        }
        s.turns++;
        String response=DialogueLines.select(s.battle,option,RNG,s.said);
        tell(player,label(npc)+": "+response);
        if(option==5){close(player,s,false,s.battle?"대화 종료 — 전투가 재개됩니다.":"일상 대화 종료.");return;}
        // Never convert an ordinary conversation to a fight, even after a taunt or rejection.
        if(!s.battle){showOptions(player,s);return;}
        if(option==0){
            boolean accept=RNG.nextInt(100)<(npc.getHealth()<npc.getMaxHealth()*.5f?78:36);
            if(accept){
                tell(player,label(npc)+": 이 싸움은 무승부로 끝내겠소.");
                close(player,s,true,"쌍방의 무승부 합의로 전투를 종료했습니다.");return;
            }
            tell(player,"상대가 무승부 제안을 거절했습니다.");
        }else if(option==1){
            // A stronger, healthier NPC is less likely to let an opponent rest repeatedly.
            boolean dominating=npc.getHealth()>npc.getMaxHealth()*.70f &&
                player.getHealth()<player.getMaxHealth()*.65f;
            if(++s.delayUses>=(dominating?2:3)){
                close(player,s,false,"상대가 시간 끌기를 간파하고 대화를 끝냈습니다.");return;
            }
        }else if(option==2){
            if(--s.patience<=1 && RNG.nextBoolean()){
                close(player,s,false,"상대가 도발을 더 듣지 않고 다시 공격 태세를 갖춥니다.");return;
            }
        }else if(option==3){
            // When the NPC is near defeat, the player's choice is an OFFER OF MERCY,
            // not a plea made by the winning NPC.
            if(npc.getHealth()<npc.getMaxHealth()*.27f &&
                player.getHealth()/player.getMaxHealth()>npc.getHealth()/npc.getMaxHealth()){
                tell(player,label(npc)+": 그 자비를 받아들이겠소. 목숨을 거두지 않아 주어 고맙소.");
                close(player,s,true,"자비를 베풀어 전투를 종료했습니다.");return;
            }
            boolean mercy=RNG.nextInt(100)<(player.getHealth()<player.getMaxHealth()*.4f?75:40);
            if(mercy){
                tell(player,label(npc)+": 이번만은 검을 거두겠소.");
                close(player,s,true,"자비를 받아 전투가 종료되었습니다.");return;
            }
            tell(player,"상대가 자비를 약속하지 않았습니다.");
        }else if(option==4){
            if(++s.infoUses>=3 && npc.getHealth()>npc.getMaxHealth()*.5f){
                close(player,s,false,"상대가 비급과 경지를 계속 캐묻는 것을 경계해 대화를 종료했습니다.");return;
            }
            if(RNG.nextInt(100)<55){
                String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(npc.getMainHandItem().getItem()).toString();
                String kind=WeaponTactics.fromItemId(id).name();
                String realm=npc.getPersistentData().getString("MurimRealm");
                String art=npc.getPersistentData().getString("MurimMartialArt");
                tell(player,"관찰한 정보: 주무기="+kind+" / 무공="+
                    (art.isBlank()?"미확인":art)+" / 경지="+(realm.isBlank()?"미확인":realm)+". 알려지지 않은 정보는 추측하지 않습니다.");
            }else tell(player,"상대가 무공과 경지를 밝히지 않았습니다.");
        }
        if(s.turns>=6){
            close(player,s,false,"상대가 충분히 들었다며 대화를 끝냈습니다. 전투가 재개됩니다.");return;
        }
        s.deadline=s.level.getGameTime()+DialogueRules.COMBAT_CHOICE_TICKS;
        showOptions(player,s);
    }
    /** Rare refusal after the session is closed permits ordinary hostile NPC AI again. */
    public static void forceHostility(ServerPlayer player,MurimHumanoid npc){
        Session s=SESSIONS.get(player.getUUID());
        if(s!=null && s.npcId.equals(npc.getUUID()))close(player,s,false,"스승의 거절로 대화가 끝났습니다.");
        npc.getPersistentData().putBoolean("MurimCultHostile",true);
        npc.getPersistentData().putUUID("MurimCultProvokedPlayer",player.getUUID());
        npc.setTarget(player);
    }
    public static void respond(ServerPlayer player,int token,boolean accept){
        Offer offer=OFFERS.get(player.getUUID());
        if(offer==null||offer.token!=token){tell(player,"요청이 만료되었거나 이미 처리되었습니다.");return;}
        OFFERS.remove(player.getUUID());
        MurimHumanoid npc=entity(offer.level,offer.npcId);
        if(!accept){MurimPanelServer.close(player);tell(player,"상대의 대화 요청을 거절했습니다.");return;}
        if(npc==null||!eligible(player,npc)||offer.level.getGameTime()>offer.deadline){
            tell(player,"대화 상대가 멀어지거나 요청이 만료되었습니다.");return;
        }
        begin(player,npc,offer.battle && fighting(player,npc));
    }
    private static MurimHumanoid entity(ServerLevel level,UUID uuid){
        return level.getEntity(uuid) instanceof MurimHumanoid npc?npc:null;
    }
    private static MurimHumanoid npc(Session s){return entity(s.level,s.npcId);}
    public static boolean isTalking(MurimHumanoid npc){
        return SESSIONS.values().stream().anyMatch(s->s.npcId.equals(npc.getUUID()));
    }
    /** Pair-only invulnerability; an external attacker INTERRUPTS the conversation instead. */
    public static void onDamage(LivingIncomingDamageEvent event){
        Entity victim=event.getEntity();
        Entity source=event.getSource().getEntity();
        if(!(victim instanceof ServerPlayer || victim instanceof MurimHumanoid))return;
        for(Session s:List.copyOf(SESSIONS.values())){
            if(!s.battle || !s.level.equals(victim.level()))continue;
            boolean victimPaired=victim.getUUID().equals(s.playerId)||victim.getUUID().equals(s.npcId);
            if(!victimPaired)continue;
            if(DialogueRules.pairedDamage(s.playerId,s.npcId,victim.getUUID(),
                    source==null?null:source.getUUID())){event.setCanceled(true);return;}
            ServerPlayer p=s.level.getServer().getPlayerList().getPlayer(s.playerId);
            close(p,s,false,"외부 공격으로 대화가 중단되었습니다.");
        }
    }
    private static void close(ServerPlayer p,Session s,boolean peaceful,String why){
        if(SESSIONS.get(s.playerId)!=s)return;
        SESSIONS.remove(s.playerId);
        MurimHumanoid npc=npc(s);
        if(npc!=null){
            npc.getPersistentData().remove("MurimLifeDialoguePause");
            npc.getPersistentData().remove("MurimLifeDialogueOldNoAi");
            npc.setNoAi(s.oldNoAi);
            if(peaceful){npc.setDuel(false);npc.setTarget(null);}
            else if(s.battle && npc.isAlive() && p!=null && p.isAlive()
                    && npc.distanceToSqr(p)<36*36 && s.oldTarget!=null && s.oldTarget.equals(p.getUUID())){
                npc.setTarget(p);
            }
        }
        if(peaceful && s.battle && npc!=null && p!=null)NpcArenaService.finishByNegotiation(npc,p);
        if(p!=null){MurimPanelServer.close(p);tell(p,why);}
    }
    public static void onTick(ServerTickEvent.Post event){
        // Called on the server thread. No force-loading or artificial game-time pause.
        for(Session s:List.copyOf(SESSIONS.values())){
            ServerPlayer p=event.getServer().getPlayerList().getPlayer(s.playerId);
            MurimHumanoid npc=npc(s);
            if(p==null||npc==null||!eligible(p,npc)||DialogueRules.expired(s.battle,s.level.getGameTime(),s.deadline)){
                close(p,s,false,"대화가 끝났습니다."+(s.battle?" 전투가 재개됩니다.":""));continue;
            }
            if(s.battle && s.level.getGameTime()%20==0){
                var data=MurimProgressData.get(p.server);
                var progress=data.player(p.getUUID(),p.getName().getString());
                double updated=DialogueRules.recoverFatigue(progress.fatigue,1.2);
                if(updated!=progress.fatigue){progress.fatigue=updated;data.setDirty();}
                var tag=npc.getPersistentData();
                tag.putFloat("MurimFatigue",(float)DialogueRules.recoverFatigue(tag.getFloat("MurimFatigue"),1.2));
                int remaining=(int)Math.max(0,(s.deadline-s.level.getGameTime()+19)/20);
                p.displayClientMessage(Component.literal("[전투 대화] "+remaining+"초 남음 · 피로도 회복 중"),true);
            }
        }
        for(Offer o:List.copyOf(OFFERS.values())){
            ServerPlayer p=event.getServer().getPlayerList().getPlayer(o.playerId);
            if(p==null||o.level.getGameTime()>o.deadline)OFFERS.remove(o.playerId);
        }
        if(++ticks%1200==0){
            long now=event.getServer().overworld().getGameTime();
            NPC_LAST_OFFER.entrySet().removeIf(e->now<e.getValue() || now-e.getValue()>2400);
            PLAYER_LAST_OFFER.entrySet().removeIf(e->now<e.getValue() || now-e.getValue()>1200);
            LAST_TILDE.entrySet().removeIf(e->now<e.getValue() || now-e.getValue()>1200);
        }
        if(ticks%40!=0)return; // bounded AI initiations, at most once every 2 seconds
        for(ServerPlayer player:event.getServer().getPlayerList().getPlayers()){
            if(player.isSpectator()||!player.isAlive()||SESSIONS.containsKey(player.getUUID())||OFFERS.containsKey(player.getUUID()))continue;
            ServerLevel level=player.serverLevel();long now=level.getGameTime();
            if(now-PLAYER_LAST_OFFER.getOrDefault(player.getUUID(),-10000L)<300)continue;
            for(MurimHumanoid npc:level.getEntitiesOfClass(MurimHumanoid.class,player.getBoundingBox().inflate(6.0))){
                if(!eligible(player,npc)||occupied(npc.getUUID()))continue;
                if(npc.getTarget()!=null && npc.getTarget()!=player)continue;
                if(!npc.getPersistentData().getBoolean("MurimLifeResident")&&!npc.isInDuel())continue;
                if(now-NPC_LAST_OFFER.getOrDefault(npc.getUUID(),-10000L)<1600)continue;
                boolean battle=fighting(player,npc);
                // Noncombat conversation requests are rarer; active duel participants can seek peace.
                if(RNG.nextInt(battle?16:180)!=0)continue;
                Offer offer=new Offer(player.getUUID(),npc.getUUID(),level,now+DialogueRules.OFFER_TICKS,++nextToken,battle);
                OFFERS.put(player.getUUID(),offer);
                NPC_LAST_OFFER.put(npc.getUUID(),now);PLAYER_LAST_OFFER.put(player.getUUID(),now);
                tell(player,label(npc)+(battle?"가 잠시 대화를 청합니다.":"이 말을 걸고 싶어 합니다."));
                MurimPanelServer.show(player,label(npc),"상대가 대화를 신청했습니다.",List.of(
                    MurimPanelServer.option("대화 수락","/murim talk reply "+offer.token+" 1"),
                    MurimPanelServer.option("거절","/murim talk reply "+offer.token+" 0")));
                break;
            }
        }
    }
    /** Operator-only deterministic testing of the NPC-initiated invitation path. */
    public static void testInvite(ServerPlayer player){
        if(SESSIONS.containsKey(player.getUUID())||OFFERS.containsKey(player.getUUID())){
            tell(player,"이미 대화나 요청이 진행 중입니다.");return;
        }
        ServerLevel world=player.serverLevel();
        for(MurimHumanoid npc:world.getEntitiesOfClass(MurimHumanoid.class,player.getBoundingBox().inflate(6.0))){
            if(!eligible(player,npc)||occupied(npc.getUUID())||
                npc.getPersistentData().getBoolean("MurimLifeSpectator"))continue;
            boolean battle=fighting(player,npc);
            Offer offer=new Offer(player.getUUID(),npc.getUUID(),world,
                    world.getGameTime()+DialogueRules.OFFER_TICKS,++nextToken,battle);
            OFFERS.put(player.getUUID(),offer);
            tell(player,label(npc)+"가 먼저 대화를 청했습니다. (개발 시험)");
            MurimPanelServer.show(player,label(npc),"상대가 먼저 말을 걸었습니다.",List.of(
                MurimPanelServer.option("수락","/murim talk reply "+offer.token+" 1"),
                MurimPanelServer.option("거절","/murim talk reply "+offer.token+" 0")));
            return;
        }
        tell(player,"6블록 안에 대화할 수 있는 무림 NPC가 없습니다.");
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("murim").then(Commands.literal("talk")
            .then(Commands.literal("testinvite").requires(src->src.hasPermission(2)).executes(ctx->{
                testInvite(ctx.getSource().getPlayerOrException());return 1;
            }))
            .then(Commands.literal("choose")
                .then(Commands.argument("token",IntegerArgumentType.integer(1))
                    .then(Commands.argument("option",IntegerArgumentType.integer(1,21)).executes(ctx->{
                        ServerPlayer player=ctx.getSource().getPlayerOrException();
                        choose(player,IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"option"));return 1;
                    }))))
            .then(Commands.literal("reply")
                .then(Commands.argument("token",IntegerArgumentType.integer(1))
                    .then(Commands.argument("accept",IntegerArgumentType.integer(0,1)).executes(ctx->{
                        ServerPlayer player=ctx.getSource().getPlayerOrException();
                        respond(player,IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"accept")==1);return 1;
                    }))))
            .then(Commands.literal("end").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();
                Session s=SESSIONS.get(p.getUUID());if(s!=null)close(p,s,false,"대화를 종료했습니다.");return 1;
            }))
        ));
    }
}
