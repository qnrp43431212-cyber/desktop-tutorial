package kr.murimlife.npc.dialogue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Seller disputes have four interactive, server-validated resolutions. */
public final class DisputeService {
    private record Request(UUID merchant,long until){}
    private static final Map<UUID,Request> REQUESTS=new HashMap<>();
    private DisputeService(){}
    public static void open(ServerPlayer p,LivingEntity merchant){
        if(NpcRoles.role(merchant)!=NpcRoles.Role.MERCHANT ||p.distanceToSqr(merchant)>36)return;
        REQUESTS.put(p.getUUID(),new Request(merchant.getUUID(),p.serverLevel().getGameTime()+600));
        List<MurimPanelServer.Choice> options=new ArrayList<>();
        for(int i=1;i<=4;i++){
            String title=switch(i){case 1->"환불과 사과 요구";case 2->"위협하여 두 배 배상 요구 (악업)";
                case 3->"시장에 거짓 정보 고발";default->"이번만 용서";};
            options.add(MurimPanelServer.option(i+". "+title,"/murim dispute choose "+i));
        }
        MurimPanelServer.show(p,"강호 분쟁 · 상인 추궁","허위정보를 판 상인에게 어떻게 대응하겠습니까?",options);
    }
    public static void choose(ServerPlayer p,int option){
        Request request=REQUESTS.remove(p.getUUID());
        MurimPanelServer.close(p);
        if(request==null||p.serverLevel().getGameTime()>request.until())return;
        var entity=p.serverLevel().getEntity(request.merchant());
        if(!(entity instanceof LivingEntity target)||!target.isAlive()||p.distanceToSqr(target)>36)return;
        InformationService.dispute(p,target,option);
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("murim").then(Commands.literal("dispute")
            .then(Commands.literal("choose").then(Commands.argument("action",IntegerArgumentType.integer(1,4)).executes(ctx->{
                choose(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"action"));return 1;})))));
    }
}
