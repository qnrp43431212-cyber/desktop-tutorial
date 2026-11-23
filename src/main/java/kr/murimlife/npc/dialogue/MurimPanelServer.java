package kr.murimlife.npc.dialogue;

import kr.murimlife.MurimNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;

/** Transfer UI presentation only; command/session authority stays server-side. */
public final class MurimPanelServer {
    private MurimPanelServer(){}
    public record Choice(String label,String command){}
    public static Choice option(String label,String command){return new Choice(label,command);}
    public static void show(ServerPlayer player,String title,String subtitle,List<Choice> choices){
        StringBuilder encoded=new StringBuilder();
        for(Choice choice:choices){
            if(encoded.length()>6000)break;
            if(choice.command()==null||!choice.command().startsWith("/murim "))continue;
            if(!encoded.isEmpty())encoded.append('\n');
            encoded.append(clean(choice.label(),110)).append('\t').append(clean(choice.command(),160));
        }
        PacketDistributor.sendToPlayer(player,new MurimNetwork.DialogueView(
            clean(title,90),clean(subtitle,420),encoded.toString()));
    }
    public static void notice(ServerPlayer player,String message){
        PacketDistributor.sendToPlayer(player,new MurimNetwork.DialogueNotice(clean(message,250)));
    }
    public static void close(ServerPlayer player){
        PacketDistributor.sendToPlayer(player,new MurimNetwork.DialogueDismiss());
    }
    private static String clean(String text,int max){
        if(text==null)return "";
        String t=text.replace('\t',' ').replace('\n',' ').replace('\r',' ');
        return t.length()>max?t.substring(0,max):t;
    }
}
