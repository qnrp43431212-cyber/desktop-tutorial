package kr.murimlife.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.murimlife.MurimProgressData;
import kr.murimlife.npc.dialogue.NpcRoles;
import kr.murimlife.npc.dialogue.MurimPanelServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** Merchant-specific catalogs; replenishes stock on independent 2-day cycles. No arbitrary item sales. */
public final class MerchantService {
    public record StockItem(String id,int buy,int sell,int min,int max){}
    private static final Map<String,List<StockItem>> CATALOGS=Map.of(
        "food",List.of(new StockItem("minecraft:bread",18,9,4,12),new StockItem("minecraft:cooked_beef",35,16,3,8),new StockItem("minecraft:apple",13,6,5,15),new StockItem("minecraft:carrot",8,4,6,18),new StockItem("minecraft:honey_bottle",40,20,2,6)),
        "herb",List.of(new StockItem("minecraft:glow_berries",32,14,4,10),new StockItem("minecraft:fermented_spider_eye",75,33,2,6),new StockItem("minecraft:golden_carrot",90,42,3,7),new StockItem("minecraft:glass_bottle",12,6,7,15),new StockItem("minecraft:blaze_powder",155,62,1,4),
            new StockItem("murimlife:ginseng_root",75,28,1,5),
            new StockItem("murimlife:clear_mind_pill",250,90,1,3),
            new StockItem("murimlife:minor_inner_pill",350,120,1,3),
            new StockItem("murimlife:unknown_elixir",420,75,0,2),
            new StockItem("murimlife:snow_lotus",180,55,0,3),
            new StockItem("murimlife:great_inner_pill",1800,600,0,1)),
        "weapon",List.of(new StockItem("minecraft:iron_sword",800,380,1,3),new StockItem("minecraft:shield",480,220,1,3),new StockItem("minecraft:bow",320,150,2,5),new StockItem("minecraft:arrow",14,6,12,40),new StockItem("minecraft:iron_ingot",125,55,3,9)),
        "scroll",List.of(new StockItem("minecraft:book",45,20,4,12),new StockItem("minecraft:paper",9,4,10,25),new StockItem("minecraft:compass",285,130,1,4),new StockItem("minecraft:map",115,45,3,7),new StockItem("minecraft:lapis_lazuli",34,14,4,12)),
        "travel",List.of(new StockItem("minecraft:leather",70,30,4,10),new StockItem("minecraft:lead",130,55,2,6),new StockItem("minecraft:bread",19,8,4,12),new StockItem("minecraft:emerald",420,215,1,3),new StockItem("minecraft:gold_ingot",570,260,1,2)),
        "sundry",List.of(new StockItem("minecraft:bread",20,9,3,10),new StockItem("minecraft:torch",8,3,8,30),new StockItem("minecraft:string",10,4,3,6),new StockItem("minecraft:leather",75,35,3,8),new StockItem("minecraft:iron_ingot",120,55,2,6)));
    private record Session(UUID entity,long until){}
    private static final Map<UUID,Session> OPEN=new HashMap<>();
    private MerchantService(){}
    private static Component link(String label,String command){return Component.literal("["+label+"]").withStyle(s->
        s.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,command)));}
    private static List<StockItem> catalog(LivingEntity npc){return CATALOGS.getOrDefault(NpcRoles.tradeCategory(npc),CATALOGS.get("sundry"));}
    private static boolean merchant(LivingEntity npc){return NpcRoles.role(npc)==NpcRoles.Role.MERCHANT;}
    private static int stock(LivingEntity npc,int i){return npc.getPersistentData().getInt("MurimStock"+i);}
    private static void restock(LivingEntity npc){
        CompoundTag tag=npc.getPersistentData();long cycle=npc.level().getGameTime()/48000L;
        if(tag.getBoolean("MurimStockInitialized") && cycle<=tag.getLong("MurimStockCycle"))return;
        tag.putBoolean("MurimStockInitialized",true);tag.putLong("MurimStockCycle",cycle);
        List<StockItem> stock=catalog(npc);
        for(int i=0;i<stock.size();i++){
            var v=stock.get(i);
            int count=v.min()+Math.floorMod(Objects.hash(npc.getUUID(),cycle,i),v.max()-v.min()+1);
            if(v.id().equals("murimlife:unknown_elixir") && Math.floorMod(Objects.hash(npc.getUUID(),cycle),4)!=0)count=0;
            if(v.id().equals("murimlife:great_inner_pill") && Math.floorMod(Objects.hash(npc.getUUID(),cycle),6)!=0)count=0;
            count=Math.max(0,count-tag.getInt("MurimTrustPenalty")/2);
            tag.putInt("MurimStock"+i,count);
        }
    }
    public static void open(ServerPlayer player,LivingEntity npc){
        if(!merchant(npc)||!npc.isAlive()||player.distanceToSqr(npc)>36 ||player.level()!=npc.level()){
            MoneyService.message(player,"거래할 수 있는 상인이 아닙니다.");return;
        }
        restock(npc);OPEN.put(player.getUUID(),new Session(npc.getUUID(),player.serverLevel().getGameTime()+2400));
        render(player,npc);
    }
    private static void render(ServerPlayer player,LivingEntity npc){
        var profile=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        String subtitle="보유 "+CurrencyRules.describe(MoneyService.balance(profile))+" · 이틀마다 재입고";
        List<MurimPanelServer.Choice> options=new ArrayList<>();
        int index=0;
        for(var item:catalog(npc)){
            Item registry=BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.id()));
            if(registry==Items.AIR){index++;continue;}
            int which=index+1;
            options.add(MurimPanelServer.option(
                which+". "+registry.getDescription().getString()+" · "+CurrencyRules.describe(
                kr.murimlife.quest.ReputationService.buyingPrice(player,item.buy()))+" · 재고 "+stock(npc,index),
                "/murim shop buy "+which));
            index++;
        }
        ItemStack held=player.getMainHandItem();
        int price=kr.murimlife.quest.ReputationService.sellingPrice(player,sellPrice(npc,held));
        if(price>0)options.add(MurimPanelServer.option("손에 든 "+held.getHoverName().getString()+" 판매 · "+CurrencyRules.describe(price),"/murim shop sell"));
        options.add(MurimPanelServer.option("상품·재고 새로고침","/murim shop list"));
        MurimPanelServer.show(player,"강호 상점 · "+npc.getDisplayName().getString(),subtitle,options);
    }
    private static LivingEntity valid(ServerPlayer p){
        Session session=OPEN.get(p.getUUID());if(session==null||p.serverLevel().getGameTime()>session.until())return null;
        var entity=p.serverLevel().getEntity(session.entity());
        if(!(entity instanceof LivingEntity npc)||!npc.isAlive()||!merchant(npc)||npc.level()!=p.level()||p.distanceToSqr(npc)>36)return null;
        return npc;
    }
    public static int sellPrice(LivingEntity npc,ItemStack stack){
        if(stack==null||stack.isEmpty())return 0;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for(var s:catalog(npc))if(s.id().equals(id))return s.sell();
        return 0;
    }
    private static void buy(ServerPlayer p,int which){
        LivingEntity npc=valid(p);if(npc==null){MoneyService.message(p,"상점이 종료되었습니다. 상인에게 다시 ~키로 말을 거십시오.");return;}
        restock(npc);var list=catalog(npc);int index=which-1;
        if(index<0||index>=list.size())return;
        var candidate=list.get(index);
        if(stock(npc,index)<1){MoneyService.message(p,"재고가 없습니다. 2일 뒤 재입고됩니다.");return;}
        var id=ResourceLocation.parse(candidate.id());
        if(!BuiltInRegistries.ITEM.containsKey(id))return;
        ItemStack goods=new ItemStack(BuiltInRegistries.ITEM.get(id));
        if(goods.isEmpty())return;
        if(!MoneyService.pay(p,kr.murimlife.quest.ReputationService.buyingPrice(p,candidate.buy()),"상품 구매"))return;
        npc.getPersistentData().putInt("MurimStock"+index,stock(npc,index)-1);
        if(!p.getInventory().add(goods))p.drop(goods,false); // Never delete a paid item on inventory overflow.
        MoneyService.message(p,"구매 완료: "+goods.getHoverName().getString());
        render(p,npc);
    }
    private static void sell(ServerPlayer p){
        LivingEntity npc=valid(p);if(npc==null){MoneyService.message(p,"판매할 상인이 없습니다.");return;}
        ItemStack held=p.getMainHandItem();int price=kr.murimlife.quest.ReputationService.sellingPrice(p,sellPrice(npc,held));
        if(price<=0){MoneyService.message(p,"이 상인은 그 물건을 매입하지 않습니다.");return;}
        var data=MurimProgressData.get(p.server);var profile=data.player(p.getUUID(),p.getName().getString());
        if(!MoneyService.change(data,profile,price))return;
        String name=held.getHoverName().getString();held.shrink(1);
        MoneyService.message(p,name+" 1개 판매 완료: +"+CurrencyRules.describe(price));
        render(p,npc);
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("murim").then(Commands.literal("shop")
            .then(Commands.literal("list").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();
                LivingEntity npc=valid(p);if(npc!=null){restock(npc);render(p,npc);}return 1;}))
            .then(Commands.literal("sell").executes(ctx->{sell(ctx.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("buy").then(Commands.argument("slot",IntegerArgumentType.integer(1,20)).executes(ctx->{
                buy(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"slot"));return 1;})))
        ));
    }
}
