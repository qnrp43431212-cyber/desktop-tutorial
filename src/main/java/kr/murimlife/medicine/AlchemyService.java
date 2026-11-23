package kr.murimlife.medicine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import kr.murimlife.ModBlocks;
import kr.murimlife.MurimProgressData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** World block and real ingredient consumption, not a free command-based item faucet. */
public final class AlchemyService {
    private AlchemyService(){}
    public static void message(ServerPlayer p,String message){p.sendSystemMessage(Component.literal("[무림 약방] "+message));}
    public static boolean nearPot(ServerPlayer p){
        BlockPos base=p.blockPosition();
        for(BlockPos pos:BlockPos.betweenClosed(base.offset(-4,-3,-4),base.offset(4,3,4)))
            if(p.serverLevel().getBlockState(pos).is(ModBlocks.HERBAL_BREWING_POT.get()))return true;
        return false;
    }
    public static void show(ServerPlayer p){
        if(!nearPot(p)){message(p,"실제 약탕기에서 5블록 이내로 접근하십시오.");return;}
        var pr=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
        message(p,"약탕기: 23개 제조식 · 내공 수련 "+pr.training+"회. 모든 제조에는 유리병 1개가 필요합니다.");
        for(var f:ElixirCatalog.recipes()){
            p.sendSystemMessage(Component.literal("["+f.title()+" · "+f.ingredientA()+"×"+f.countA()+
                " + "+f.ingredientB()+"×"+f.countB()+"]").withStyle(style->style.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/murim alchemy brew "+f.id()))));
        }
    }
    private static Item item(String id){
        ResourceLocation key=ResourceLocation.parse(id);
        return BuiltInRegistries.ITEM.containsKey(key)?BuiltInRegistries.ITEM.get(key):net.minecraft.world.item.Items.AIR;
    }
    private static int count(ServerPlayer player,Item item){
        int sum=0;for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);if(stack.is(item))sum+=stack.getCount();
        }return sum;
    }
    private static void remove(ServerPlayer player,Item item,int amount){
        for(int i=0;i<player.getInventory().getContainerSize()&&amount>0;i++){
            var stack=player.getInventory().getItem(i);if(!stack.is(item))continue;
            int used=Math.min(amount,stack.getCount());stack.shrink(used);amount-=used;
        }
    }
    public static void brew(ServerPlayer p,String id){
        if(!nearPot(p)){message(p,"약탕기가 너무 멀리 있습니다.");return;}
        var f=ElixirCatalog.get(id);
        if(f==null||id.equals("unknown_elixir")){message(p,"존재하지 않는 약재 조합입니다.");return;}
        var state=MurimProgressData.get(p.server);var pr=state.player(p.getUUID(),p.getName().getString());
        int needed=f.price()>1800?75:f.price()>600?30:5;
        if(pr.training<needed){message(p,"약성 조절을 위해 누적 수련 "+needed+"회가 필요합니다.");return;}
        Item a=item(f.ingredientA()),b=item(f.ingredientB()),bottle=net.minecraft.world.item.Items.GLASS_BOTTLE;
        if(a==net.minecraft.world.item.Items.AIR||b==net.minecraft.world.item.Items.AIR){message(p,"재료가 등록되지 않았습니다.");return;}
        Map<Item,Integer> need=new LinkedHashMap<>();need.merge(a,f.countA(),Integer::sum);
        need.merge(b,f.countB(),Integer::sum);need.merge(bottle,1,Integer::sum);
        for(var e:need.entrySet())if(count(p,e.getKey())<e.getValue()){
            message(p,"재료 부족: "+e.getKey().getDescription().getString()+" ×"+e.getValue());return;
        }
        for(var e:need.entrySet())remove(p,e.getKey(),e.getValue());
        Item medicine=item("murimlife:"+id);
        if(medicine==net.minecraft.world.item.Items.AIR){message(p,"등록되지 않은 영약입니다.");return;}
        var stack=new ItemStack(medicine);if(!p.getInventory().add(stack))p.drop(stack,false);
        pr.counters.merge("alchemyCrafted",1,Integer::sum);state.setDirty();
        message(p,f.title()+" 조제 완료. 과용 시 주화입마 위험이 증가합니다.");
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("murim").then(Commands.literal("alchemy")
            .then(Commands.literal("brew").then(Commands.argument("recipe",StringArgumentType.word()).executes(ctx->{
                brew(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"recipe"));return 1;})))
            .executes(ctx->{show(ctx.getSource().getPlayerOrException());return 1;})
        ));
    }
}
