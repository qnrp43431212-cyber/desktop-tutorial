package kr.murimlife.quest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.murimlife.MurimProgressData;
import kr.murimlife.economy.CurrencyRules;
import kr.murimlife.economy.GovernanceRules;
import kr.murimlife.economy.MoneyService;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.dialogue.NpcRoles;
import kr.murimlife.opportunity.OpportunitySites;
import kr.murimlife.worldgen.SiteLedger;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** A server-validated mission board opened via ~ dialogue, renewed every 3 Minecraft days. */
public final class QuestService extends SavedData {
    private static final long PERIOD_TICKS=72000L;
    private static long ticks;
    public static final class Assignment {
        public String id;public long since,deadline;public int trainingStart,combatStart,guardStart,delivered,x,z;
        Assignment(String id,long since,MurimProgressData.Profile p,ServerPlayer player){
            this.id=id;this.since=since;this.deadline=since+PERIOD_TICKS;
            trainingStart=p.training;combatStart=p.counters.getOrDefault("combatVictories",0);
            guardStart=p.counters.getOrDefault("opportunityKills",0);if(player!=null){x=player.getBlockX();z=player.getBlockZ();}
        }
    }
    private record Offer(UUID npc,String faction,int grade,long expires,long boardCycle,List<String> ids){}
    private static final Map<UUID,Offer> SESSIONS=new HashMap<>();
    private final Map<UUID,Assignment> active=new HashMap<>();
    private QuestService(){}
    private static QuestService load(CompoundTag tag,HolderLookup.Provider provider){
        QuestService d=new QuestService();var list=tag.getList("Assignments",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)try{
            var t=list.getCompound(i);UUID owner=UUID.fromString(t.getString("Player"));
            String id=t.getString("Quest");if(QuestCatalog.find(id)==null)continue;
            var assignment=new Assignment(id,t.getLong("Start"),new MurimProgressData.Profile(),null);
            assignment.deadline=t.getLong("Deadline");assignment.trainingStart=t.getInt("TrainingStart");
            assignment.combatStart=t.getInt("CombatStart");assignment.guardStart=t.getInt("GuardStart");
            assignment.delivered=t.getInt("Delivered");assignment.x=t.getInt("X");assignment.z=t.getInt("Z");
            d.active.put(owner,assignment);
        }catch(IllegalArgumentException ignored){}return d;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        var list=new ListTag();for(var e:active.entrySet()){
            var a=e.getValue();var t=new CompoundTag();t.putString("Player",e.getKey().toString());t.putString("Quest",a.id);
            t.putLong("Start",a.since);t.putLong("Deadline",a.deadline);t.putInt("TrainingStart",a.trainingStart);
            t.putInt("CombatStart",a.combatStart);t.putInt("GuardStart",a.guardStart);t.putInt("Delivered",a.delivered);
            t.putInt("X",a.x);t.putInt("Z",a.z);list.add(t);
        }tag.put("Assignments",list);return tag;
    }
    public static QuestService get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(QuestService::new,QuestService::load),"murimlife_quests_v124");}
    private static void say(ServerPlayer p,String m){p.sendSystemMessage(Component.literal("[무림 임무] "+m));}
    private static Component click(String label,String command){return Component.literal("["+label+"]").withStyle(s->s.withColor(ChatFormatting.AQUA)
        .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,command)));}
    private static String factionOf(LivingEntity npc){
        if(npc instanceof MurimHumanoid human){
            String site=human.getPersistentData().getString("MurimNpcSite").toLowerCase(Locale.ROOT);
            if(site.contains("sado")||site.contains("sadaoryeon"))return "sado";
            if(site.contains("alliance")||site.contains("murim_maeng"))return "alliance";
        }
        if(npc instanceof net.minecraft.world.entity.npc.Villager)return "civilian";
        return NpcRoles.role(npc)==NpcRoles.Role.RESIDENT?"civilian":"";
    }
    private static boolean eligible(ServerPlayer player,String faction){
        var p=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        boolean unaffiliated=p.sect.isBlank()||"무소속".equals(p.sect);
        if("civilian".equals(faction))return unaffiliated;
        if(unaffiliated)return true; // Mercenary: guild rank cap is one grade below regular members.
        String alignment=p.foundedSect.isBlank()?p.sect:p.sectAlignment;
        var align=GovernanceRules.alignment(alignment);
        if("alliance".equals(faction))return align==GovernanceRules.Alignment.RIGHTEOUS&&!p.righteousLicenseRevoked;
        if("sado".equals(faction))return align==GovernanceRules.Alignment.UNORTHODOX;
        return false;
    }
    private static int unlocked(ServerPlayer player,String faction){
        var p=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        boolean mercenary="무소속".equals(p.sect)||p.sect.isBlank();
        int grade=GovernanceRules.missionGrade(p.realm,p.reputation,mercenary);
        return "civilian".equals(faction)?Math.min(1,grade):grade;
    }
    public static void openFromNpc(ServerPlayer player,LivingEntity npc){
        if(npc==null||!npc.isAlive()||npc.level()!=player.level()||player.distanceToSqr(npc)>36){say(player,"대화 대상이 너무 멀리 있습니다.");return;}
        String faction=factionOf(npc);
        if(faction.isBlank()){say(player,"이 인물은 임무 접수 권한이 없습니다. 무림맹·사도련 거점의 무림인 또는 주민을 찾으십시오.");return;}
        if(!eligible(player,faction)){say(player,"현재 소속으로는 해당 임무를 받을 수 없습니다.");return;}
        showRanks(player,npc.getUUID(),faction);
    }
    /** Headquarters boards are available even if the site's NPC roster is disabled. */
    private static boolean nearHQ(ServerPlayer player,String faction){
        if(player.serverLevel()!=player.server.overworld())return false;
        String site="alliance".equals(faction)?"murim_alliance":"sadaoryeon";
        return SiteLedger.get(player.serverLevel()).jobs().stream().anyMatch(j->
            j.stage==SiteLedger.Stage.COMPLETE&&
            (j.site.equals(site)||(faction.equals("sado")&&j.site.equals("sado")))&&
            Math.abs(player.getBlockX()-j.x)<=40&&Math.abs(player.getBlockZ()-j.z)<=40);
    }
    public static void openHeadquarters(ServerPlayer player,String faction){
        if(!List.of("alliance","sado").contains(faction)||!nearHQ(player,faction)){
            say(player,"실제 생성된 무림맹/사도련 본부 40블록 이내에서 접수할 수 있습니다.");return;
        }
        if(!eligible(player,faction)){say(player,"소속 조건에 맞지 않습니다.");return;}
        showRanks(player,null,faction);
    }
    private static void showRanks(ServerPlayer player,UUID npc,String faction){
        var p=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        say(player,"임무 접수처: "+faction+" / 평판 "+p.reputation+" / 3일마다 목록 갱신 (미완료 임무는 3일 내 실패)");
        int max=unlocked(player,faction);
        SESSIONS.put(player.getUUID(),new Offer(npc,faction,-1,player.serverLevel().getGameTime()+1200,-1,List.of()));
        for(int i=0;i<=max;i++)player.sendSystemMessage(click(QuestCatalog.GRADES[i]+" 등급 임무 보기", "/murim quest rank "+i));
    }
    private static boolean sessionValid(ServerPlayer player,Offer s){
        if(s==null||player.serverLevel().getGameTime()>s.expires()||!eligible(player,s.faction()))return false;
        if(s.npc()==null)return nearHQ(player,s.faction());
        var entity=player.serverLevel().getEntity(s.npc());
        return entity instanceof LivingEntity npc && npc.isAlive() && npc.level()==player.level()&&
            player.distanceToSqr(npc)<=36 && factionOf(npc).equals(s.faction());
    }
    public static void selectRank(ServerPlayer p,int grade){
        Offer old=SESSIONS.get(p.getUUID());
        if(!sessionValid(p,old)||grade<0||grade>unlocked(p,old.faction())){say(p,"임무 창이 만료되었거나 해당 등급을 받을 수 없습니다.");return;}
        long cycle=p.server.overworld().getGameTime()/PERIOD_TICKS;
        var pool=new ArrayList<>(QuestCatalog.list(old.faction(),grade));
        Collections.shuffle(pool,new Random(Objects.hash(p.server.overworld().getSeed(),p.getUUID(),old.faction(),grade,cycle)));
        var chosen=pool.stream().limit(5).map(QuestCatalog.Quest::id).toList();
        SESSIONS.put(p.getUUID(),new Offer(old.npc(),old.faction(),grade,p.serverLevel().getGameTime()+1200,cycle,chosen));
        say(p,QuestCatalog.GRADES[grade]+" 등급 / 표시 5개 / 갱신 주기 "+cycle);
        int n=0;for(String id:chosen){var q=QuestCatalog.find(id);n++;
            p.sendSystemMessage(click(n+". "+q.title()+" / "+description(q)+" / "+CurrencyRules.describe(q.rewardMun()),
                "/murim quest accept "+n));
        }
    }
    private static String description(QuestCatalog.Quest q){return switch(q.objective()){
        case "TRAIN"->"수련 "+q.count()+"회";
        case "WIN"->"실제 적대 전투 승리 "+q.count()+"회";
        case "DELIVER"->q.item()+" "+q.count()+"개 납품";
        case "GUARD"->"기연 수호 적 "+q.count()+"명 처치";
        case "EXPLORE"->"등록된 기연 구조물 방문";
        default->"알 수 없는 목표";
    };}
    public static void accept(ServerPlayer p,int index){
        var session=SESSIONS.get(p.getUUID());var data=get(p.server);
        long now=p.serverLevel().getGameTime(),cycle=p.server.overworld().getGameTime()/PERIOD_TICKS;
        if(!sessionValid(p,session)||session.grade()<0||cycle!=session.boardCycle()||
            index<1||index>session.ids().size()){say(p,"유효하지 않은 임무입니다. 접수처에서 목록을 새로 여십시오.");return;}
        if(data.active.containsKey(p.getUUID())){say(p,"진행 중인 임무가 있습니다. /murim quest status");return;}
        var q=QuestCatalog.find(session.ids().get(index-1));
        if(q==null||!q.faction().equals(session.faction())||q.grade()!=session.grade())return;
        var pr=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
        data.active.put(p.getUUID(),new Assignment(q.id(),now,pr,p));data.setDirty();SESSIONS.remove(p.getUUID());
        say(p,"임무 수락: "+q.title()+" · "+description(q)+" / 3일 내 완수. /murim quest status");
    }
    public static void status(ServerPlayer p){
        var data=get(p.server);var a=data.active.get(p.getUUID());
        var profile=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
        say(p,"평판 "+profile.reputation+"/1000 · 정체 은폐 "+(ReputationService.disguised(p)?"중":"없음")+
            " / 구매 할인 "+GovernanceRules.priceDiscountPercent(profile.reputation)+"% (가면 착용은 평판 수치를 지우지 않음)");
        if(a==null){say(p,"진행 중인 임무가 없습니다.");return;}
        var q=QuestCatalog.find(a.id);long left=Math.max(0,(a.deadline-p.serverLevel().getGameTime()+23999)/24000);
        int done=progress(a,q,profile,p);
        say(p,q.title()+" / "+description(q)+" / 진행 "+done+"/"+q.count()+" / 남은 시간 약 "+left+"일");
        if(q.objective().equals("DELIVER"))p.sendSystemMessage(click("물품 납품","/murim quest deliver"));
    }
    private static int progress(Assignment a,QuestCatalog.Quest q,MurimProgressData.Profile p,ServerPlayer player){
        return switch(q.objective()){
            case "TRAIN"->Math.max(0,p.training-a.trainingStart);
            case "WIN"->Math.max(0,p.counters.getOrDefault("combatVictories",0)-a.combatStart);
            case "GUARD"->Math.max(0,p.counters.getOrDefault("opportunityKills",0)-a.guardStart);
            case "DELIVER"->a.delivered;
            case "EXPLORE"->{
                long dx=(long)player.getBlockX()-a.x,dz=(long)player.getBlockZ()-a.z;
                if(dx*dx+dz*dz<64L*64L||player.serverLevel()!=player.server.overworld())yield 0;
                yield OpportunitySites.get(player.serverLevel()).all().stream().anyMatch(s->{
                    long x=(long)player.getBlockX()-s.x(),z=(long)player.getBlockZ()-s.z();
                    return x*x+z*z<=18*18;})?1:0;
            }
            default->0;
        };
    }
    private static void win(ServerPlayer p,Assignment a,QuestCatalog.Quest q){
        var profiles=MurimProgressData.get(p.server);var profile=profiles.player(p.getUUID(),p.getName().getString());
        // Reputation only changes social standing: karma is awarded by explicit charity objectives, never by fame.
        int bonus=1+Math.min(20,profile.reputation/50);
        long payout=(long)q.rewardMun()*(100+bonus)/100;
        if(!MoneyService.change(profiles,profile,payout)) {say(p,"보유금 한도 때문에 보상을 받을 수 없습니다.");return;}
        profile.reputation=GovernanceRules.reputation(profile.reputation,q.reputation());
        if(q.good()>0)profiles.goodDeed(p.getUUID(),profile,q.good());
        profile.counters.merge("completedQuests",1,Integer::sum);profiles.setDirty();
        var service=get(p.server);service.active.remove(p.getUUID());service.setDirty();
        say(p,"임무 완료! +"+CurrencyRules.describe(payout)+", 평판 +"+q.reputation()+
            (q.good()>0?", 선업 +"+q.good():"")+" (선업과 평판은 별개입니다.)");
    }
    private static void fail(MinecraftServer server,UUID player,Assignment a,String reason){
        var q=QuestCatalog.find(a.id);var data=MurimProgressData.get(server);var p=data.playerProfiles().get(player);
        int loss=6+q.grade()*6;
        if(p!=null){p.reputation=GovernanceRules.reputation(p.reputation,-loss);data.setDirty();}
        var service=get(server);service.active.remove(player);service.setDirty();
        var online=server.getPlayerList().getPlayer(player);
        if(online!=null)say(online,"임무 실패 ("+reason+") / 평판 -"+loss);
    }
    public static void deliver(ServerPlayer p){
        var service=get(p.server);var a=service.active.get(p.getUUID());if(a==null){say(p,"진행 중인 임무가 없습니다.");return;}
        var q=QuestCatalog.find(a.id);
        if(!q.objective().equals("DELIVER")){say(p,"이 임무는 납품형이 아닙니다.");return;}
        if(p.serverLevel().getGameTime()>a.deadline){fail(p.server,p.getUUID(),a,"기한 초과");return;}
        var id=ResourceLocation.tryParse(q.item());
        if(id==null||!BuiltInRegistries.ITEM.containsKey(id))return;
        Item item=BuiltInRegistries.ITEM.get(id);if(item==Items.AIR)return;
        int want=q.count()-a.delivered,owned=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))
            owned+=p.getInventory().getItem(i).getCount();
        if(owned<=0){say(p,"납품 가능한 물품이 없습니다: "+q.item());return;}
        int take=Math.min(owned,want),pending=take;
        for(int i=0;i<p.getInventory().getContainerSize()&&pending>0;i++){
            var stack=p.getInventory().getItem(i);
            if(stack.is(item)){int n=Math.min(stack.getCount(),pending);stack.shrink(n);pending-=n;}
        }
        a.delivered+=take;service.setDirty();say(p,"납품 "+take+"개, 누적 "+a.delivered+"/"+q.count());
        if(a.delivered>=q.count())win(p,a,q);
    }
    public static void abandon(ServerPlayer p){
        var service=get(p.server);var a=service.active.get(p.getUUID());
        if(a==null){say(p,"포기할 임무가 없습니다.");return;}
        fail(p.server,p.getUUID(),a,"자발적 포기");
    }
    public static void tick(ServerTickEvent.Post event){
        if(++ticks%100!=0)return;
        var server=event.getServer();var service=get(server);
        for(var e:new ArrayList<>(service.active.entrySet())){
            var a=e.getValue();if(server.overworld().getGameTime()>a.deadline){fail(server,e.getKey(),a,"기한 3일 경과");continue;}
            var player=server.getPlayerList().getPlayer(e.getKey());if(player==null)continue;
            var profile=MurimProgressData.get(server).player(player.getUUID(),player.getName().getString());
            var q=QuestCatalog.find(a.id);
            if(progress(a,q,profile,player)>=q.count())win(player,a,q);
        }
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> d){
        d.register(Commands.literal("murim").then(Commands.literal("quest")
            .then(Commands.literal("status").executes(ctx->{status(ctx.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("board").then(Commands.literal("alliance").executes(ctx->{openHeadquarters(ctx.getSource().getPlayerOrException(),"alliance");return 1;}))
                .then(Commands.literal("sado").executes(ctx->{openHeadquarters(ctx.getSource().getPlayerOrException(),"sado");return 1;})))
            .then(Commands.literal("rank").then(Commands.argument("grade",IntegerArgumentType.integer(0,4)).executes(ctx->{
                selectRank(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"grade"));return 1;})))
            .then(Commands.literal("accept").then(Commands.argument("slot",IntegerArgumentType.integer(1,5)).executes(ctx->{
                accept(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"slot"));return 1;})))
            .then(Commands.literal("deliver").executes(ctx->{deliver(ctx.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("abandon").executes(ctx->{abandon(ctx.getSource().getPlayerOrException());return 1;}))
            .executes(ctx->{status(ctx.getSource().getPlayerOrException());return 1;})
        ));
    }
}
