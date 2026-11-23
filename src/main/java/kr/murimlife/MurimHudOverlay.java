package kr.murimlife;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * World-visible, no-blur MURIM LIFE UI. This is NOT a Minecraft Screen:
 * - no vanilla Button rendering, no renderBackground(), no world dimming;
 * - the side dialogue never occupies the centre of the player's view;
 * - hold either Alt to release the cursor; on release it is captured again;
 * - all dialogue commands are validated by existing server command/session handlers.
 */
public final class MurimHudOverlay {
    private MurimHudOverlay(){}
    private record Choice(String title,String command){}
    private record Hit(int x1,int y1,int x2,int y2,Runnable action) {
        boolean contains(double x,double y){return x>=x1&&x<x2&&y>=y1&&y<y2;}
    }
    private static final List<Hit> HITS=new ArrayList<>();
    private static final List<Choice> OPTIONS=new ArrayList<>();
    private static String npcName="", npcLine="";
    private static String notice="";
    private static long noticeUntil;
    private static boolean talkOpen,profileOpen,pointerReleased;
    private static int tab,slot,page,dialogScroll;
    private static final String[] TABS={"정보","무공","스탯","문파","팀"};
    private static final int INK=0xFFF3E9D3, GOLD=0xFFE9C986, MUTED=0xFFBFC5B5;

    public static boolean profileOpen(){return profileOpen;}
    public static boolean dialogueOpen(){return talkOpen;}
    public static boolean visible(){return profileOpen||talkOpen;}
    public static void toggleProfile(){
        profileOpen=!profileOpen;
        if(profileOpen)PacketDistributor.sendToServer(new MurimNetwork.SelectSlot(-1));
    }
    public static void dialogue(String title,String description,String serializedChoices){
        npcName=title==null?"무림인":title;
        npcLine=description==null?"":description;
        OPTIONS.clear(); dialogScroll=0;
        if(serializedChoices!=null&&!serializedChoices.isBlank()){
            for(String entry:serializedChoices.split("\\n")){
                String[] bits=entry.split("\\t",2);
                if(bits.length==2 && !bits[0].isBlank() && bits[1].startsWith("/murim ") && OPTIONS.size()<32)
                    OPTIONS.add(new Choice(bits[0],bits[1].substring(1)));
            }
        }
        talkOpen=true;
    }
    public static void notice(String message){
        notice=message==null?"":message;
        Minecraft mc=Minecraft.getInstance();
        noticeUntil=mc.level==null?0:mc.level.getGameTime()+120;
    }
    public static void closeDialogue(){talkOpen=false;OPTIONS.clear();}
    public static void reset(){
        talkOpen=false;profileOpen=false;OPTIONS.clear();HITS.clear();
        if(pointerReleased){pointerReleased=false;Minecraft mc=Minecraft.getInstance();if(mc.screen==null)mc.mouseHandler.grabMouse();}
    }
    public static void updateAlt(Minecraft mc){
        if(mc.player==null||mc.level==null){reset();return;}
        if(mc.screen!=null){pointerReleased=false;return;}
        boolean alt=(InputConstants.isKeyDown(mc.getWindow().getWindow(),GLFW.GLFW_KEY_LEFT_ALT)
            ||InputConstants.isKeyDown(mc.getWindow().getWindow(),GLFW.GLFW_KEY_RIGHT_ALT)) && visible();
        if(alt&&!pointerReleased){mc.mouseHandler.releaseMouse();pointerReleased=true;}
        else if(!alt&&pointerReleased){mc.mouseHandler.grabMouse();pointerReleased=false;}
    }
    public static boolean pointerReleased(){return pointerReleased;}
    public static boolean handleMouseClick(double x,double y,int button){
        if(!pointerReleased||!visible())return false;
        if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT){
            // The last rendered frame owns the button hitboxes. Commands are never executed client-side.
            for(int i=HITS.size()-1;i>=0;i--){Hit h=HITS.get(i);if(h.contains(x,y)){h.action.run();return true;}}
        }
        // While Alt is down, clicking does not mine/attack/place behind the UI.
        return true;
    }
    public static boolean scroll(double amount){
        if(!pointerReleased||!talkOpen||OPTIONS.size()<=8)return false;
        dialogScroll=Math.max(0,Math.min(OPTIONS.size()-8,dialogScroll+(amount>0?-2:2)));
        return true;
    }
    private static boolean inside(double x,double y,int a,int b,int c,int d){return x>=a&&x<c&&y>=b&&y<d;}
    private static void border(GuiGraphics g,int x,int y,int w,int h,int bg){
        // Solid pixels and solid text; deliberately no translucent background under labels.
        g.fill(x-3,y-3,x+w+3,y+h+3,0xE90A1316);
        g.fill(x,y,x+w,y+h,bg);
        g.fill(x,y,x+w,y+2,0xFFBC9359);
        g.fill(x,y+h-2,x+w,y+h,0xFFBC9359);
        g.fill(x,y,x+2,y+h,0xFFBC9359);
        g.fill(x+w-2,y,x+w,y+h,0xFFBC9359);
    }
    private static String fit(Font f,String s,int width){
        if(s==null)return "";
        if(f.width(s)<=width)return s;
        String shortText=f.plainSubstrByWidth(s,Math.max(1,width-f.width("…")));
        return shortText+"…";
    }
    private static void txt(GuiGraphics g,Font f,String text,int x,int y,int maxWidth,int color){
        g.drawString(f,fit(f,text,maxWidth),x,y,color,false);
    }
    private static void button(GuiGraphics g,Font f,String label,int x,int y,int w,int h,
                               boolean selected,boolean enabled,double mx,double my,Runnable action){
        boolean hover=pointerReleased&&inside(mx,my,x,y,x+w,y+h);
        g.fill(x,y,x+w,y+h,!enabled?0xFF273132:selected?0xFF5C4533:hover?0xFF3C514C:0xFF263B39);
        g.fill(x,y,x+w,y+1,hover?GOLD:0xFF71867A);
        g.fill(x,y+h-1,x+w,y+h,0xFF101A1D);
        txt(g,f,label,x+8,y+(h-8)/2,w-15,!enabled?0xFF65726B:selected?GOLD:INK);
        if(enabled&&action!=null)HITS.add(new Hit(x,y,x+w,y+h,action));
    }
    private static void command(String cmd){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null&&mc.player.connection!=null&&cmd!=null&&cmd.startsWith("murim "))
            mc.player.connection.sendCommand(cmd);
    }
    private static void dialogue(GuiGraphics g,Minecraft mc,double mx,double my){
        int sw=mc.getWindow().getGuiScaledWidth(),sh=mc.getWindow().getGuiScaledHeight();
        int w=Math.min(312,Math.max(214,sw/3));
        int x=sw-w-12,y=24;
        int visible=Math.min(8,OPTIONS.size());
        int panelH=Math.min(sh-42,119+visible*22);
        border(g,x,y,w,panelH,0xFF142324);
        Font f=mc.font;
        txt(g,f,"江 湖 · 대화",x+13,y+13,w-22,GOLD);
        txt(g,f,npcName,x+13,y+31,w-25,INK);
        // Two lines of crisp, full-alpha body text. Never run the game's blur shader.
        List<net.minecraft.util.FormattedCharSequence> lines=f.split(Component.literal(npcLine),w-30);
        for(int i=0;i<Math.min(2,lines.size());i++)g.drawString(f,lines.get(i),x+13,y+49+i*11,MUTED,false);
        g.fill(x+11,y+75,x+w-11,y+76,0xFF668074);
        int start=Math.min(dialogScroll,Math.max(0,OPTIONS.size()-8));
        for(int i=0;i<visible;i++){
            int index=start+i;
            if(index>=OPTIONS.size())break;
            Choice choice=OPTIONS.get(index);
            button(g,f,choice.title(),x+11,y+82+i*22,w-22,20,false,true,mx,my,
                ()->command(choice.command()));
        }
        if(OPTIONS.size()>8)txt(g,f,"Alt + 휠: 더 많은 선택지",x+12,y+panelH-20,w-24,GOLD);
        else txt(g,f,"Alt: 마우스 선택   |   ~: 새 대화",x+12,y+panelH-20,w-24,0xFF93B0A0);
        // The HUD must not block the player's view or pause any NPC AI.
    }
    private static List<String> skills(){
        if(ClientProfileCache.arts==null||ClientProfileCache.arts.isBlank())return List.of();
        return Arrays.stream(ClientProfileCache.arts.split("\\|"))
            .filter(s->!s.isBlank())
            .filter(s->{var art=MurimCombat.ARTS.get(s);return art!=null&&!"passive".equals(art.type());})
            .toList();
    }
    private static void profile(GuiGraphics g,Minecraft mc,double mx,double my){
        int sw=mc.getWindow().getGuiScaledWidth(),sh=mc.getWindow().getGuiScaledHeight();
        int w=Math.min(465,sw-22),h=Math.min(290,sh-22),x=(sw-w)/2,y=(sh-h)/2;
        border(g,x,y,w,h,0xFF182A2A);
        Font f=mc.font;
        txt(g,f,"江 湖 錄   강호록",x+15,y+12,w-44,GOLD);
        button(g,f,"×",x+w-28,y+9,17,17,false,true,mx,my,()->profileOpen=false);
        for(int i=0;i<TABS.length;i++){
            final int idx=i;
            int bw=(w-24)/5;
            button(g,f,TABS[i],x+12+i*bw,y+35,bw-3,23,tab==i,true,mx,my,()->{tab=idx;page=0;});
        }
        g.fill(x+13,y+67,x+w-13,y+h-21,0xFF101C1E);
        String summary=ClientProfileCache.summary==null?"":ClientProfileCache.summary;
        txt(g,f,summary,x+20,y+76,w-40,INK);
        txt(g,f,"소속: "+ClientProfileCache.sect,x+20,y+91,w-40,GOLD);
        if(tab==0){
            txt(g,f,"경지와 전투력, 문파 및 별호는 서버 기록으로 계산됩니다.",x+20,y+113,w-40,MUTED);
            txt(g,f,"M 강호록   /   R 무공 선택   /   G 패링",x+20,y+133,w-40,INK);
            txt(g,f,"NPC 대화: ~     |     Alt 누른 채 마우스로 선택",x+20,y+153,w-40,MUTED);
            txt(g,f,"상세 기록: /murim status",x+20,y+174,w-40,GOLD);
        }else if(tab==1){
            List<String> known=skills();
            int maxPage=Math.max(0,(known.size()-1)/8);
            page=Math.min(maxPage,page);
            txt(g,f,"배치할 무공을 선택하십시오. 현재 슬롯: "+(slot+1)+"/12",x+20,y+112,w-40,GOLD);
            for(int i=0;i<8;i++){
                int index=page*8+i;
                if(index>=known.size())break;
                String skill=known.get(index);
                int bx=x+20+(i%2)*(w-47)/2,by=y+128+(i/2)*23;
                button(g,f,skill,bx,by,(w-55)/2,20,false,true,mx,my,
                    ()->PacketDistributor.sendToServer(new MurimNetwork.AssignSkill(slot,skill)));
            }
            button(g,f,"◀",x+22,y+h-48,35,18,false,page>0,mx,my,()->page=Math.max(0,page-1));
            button(g,f,"칸 변경",x+69,y+h-48,72,18,false,true,mx,my,()->slot=(slot+1)%12);
            button(g,f,"▶",x+w-58,y+h-48,35,18,false,page<maxPage,mx,my,()->page++);
        }else if(tab==2){
            String stats=ClientProfileCache.stats==null?"":ClientProfileCache.stats;
            String[] segments=stats.split("  ");
            for(int i=0;i<segments.length&&i<15;i++){
                int col=i%3,row=i/3;
                txt(g,f,segments[i],x+19+col*(w-30)/3,y+118+row*21,(w-42)/3,INK);
            }
        }else if(tab==3){
            txt(g,f,"문파와 소속 정보",x+20,y+115,w-40,GOLD);
            txt(g,f,"세부 운영 명령: /murim sect",x+20,y+136,w-40,INK);
            txt(g,f,"문파 모집: /murim recruitment",x+20,y+157,w-40,MUTED);
        }else{
            txt(g,f,"팀 및 동료",x+20,y+115,w-40,GOLD);
            txt(g,f,"팀 관리 기능은 서버 규칙과 연결됩니다.",x+20,y+136,w-40,MUTED);
        }
        txt(g,f,"마우스 선택: Alt   •   배경 흐림 없음   •   M 닫기",x+16,y+h-14,w-25,0xFF90A69A);
    }
    public static void render(GuiGraphics g,Minecraft mc){
        if(mc.player==null||mc.screen!=null)return;
        HITS.clear();
        double mx=pointerReleased?mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth():-1;
        double my=pointerReleased?mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight():-1;
        if(talkOpen)dialogue(g,mc,mx,my);
        if(profileOpen)profile(g,mc,mx,my);
        if(pointerReleased){
            // draw a custom cursor, since the operating-system cursor may be themed differently.
            g.fill((int)mx,(int)my,(int)mx+2,(int)my+11,GOLD);
            g.fill((int)mx,(int)my,(int)mx+8,(int)my+2,GOLD);
        }
        if(!notice.isBlank()&&mc.level!=null&&mc.level.getGameTime()<noticeUntil){
            int y=mc.getWindow().getGuiScaledHeight()-64;
            int x=Math.max(6,(mc.getWindow().getGuiScaledWidth()-mc.font.width(notice))/2);
            g.fill(x-7,y-6,x+mc.font.width(notice)+7,y+16,0xEE172927);
            txt(g,mc.font,notice,x,y,mc.getWindow().getGuiScaledWidth()-14,INK);
        }
    }
}
