package kr.murimlife.cult;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CultCommands {
    private CultCommands(){}
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(Commands.literal("murim").then(Commands.literal("cult")
            .then(Commands.literal("status").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();CultRoster roster=CultRoster.get(p.server);
                p.sendSystemMessage(Component.literal(roster.report(p.server,"마교")));
                p.sendSystemMessage(Component.literal(roster.report(p.server,"혈교")));
                p.sendSystemMessage(Component.literal("대마두의 사공은 천마/혈마의 교주 전용 기술과 다릅니다. 교주가 되어도 자동 습득하지 않습니다."));
                return 1;
            }))
            .then(Commands.literal("spawn").requires(s->s.hasPermission(2))
                .then(Commands.argument("cult",StringArgumentType.word()).executes(ctx->{
                    ServerPlayer p=ctx.getSource().getPlayerOrException();
                    String cult=StringArgumentType.getString(ctx,"cult");
                    p.sendSystemMessage(Component.literal(CultRoster.get(p.server).spawnOne(p,cult)));return 1;
                })))
        ));
    }
}
