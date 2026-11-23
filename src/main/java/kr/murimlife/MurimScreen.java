package kr.murimlife;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Clear, high-contrast M-menu. Removes background blur but retains dim overlay. */
public final class MurimScreen extends Screen {
    private int tab=0, targetSlot=0, page=0;
    private static final String[] TABS={"정보","무공","스탯","문파","팀"};
    private final List<Button> martialButtons=new ArrayList<>();
    private final List<Button> tabButtons=new ArrayList<>();
    private Button slotPicker, prevPage, nextPage;
    public MurimScreen(){super(Component.literal("江 湖 錄 · 강호록"));}

    @Override protected void init() {
        PacketDistributor.sendToServer(new MurimNetwork.SelectSlot(-1));
        int cx=width/2, top=height/2-105;
        for(int i=0;i<TABS.length;i++) {
            final int idx=i;
            Button b=addRenderableWidget(Button.builder(Component.literal(TABS[i]),btn->{tab=idx;page=0;})
                .bounds(cx-162+i*65,top+49,63,20).build());
            tabButtons.add(b);
        }
        slotPicker=addRenderableWidget(Button.builder(Component.literal("배치 칸: 1 / 12"),b->{
            targetSlot=(targetSlot+1)%12;
            b.setMessage(Component.literal("배치 칸: "+(targetSlot+1)+" / 12"));
        }).bounds(cx-74,top+79,148,19).build());
        for(int k=0;k<9;k++){
            final int idx=k,ix=k%3,iy=k/3;
            Button btn=addRenderableWidget(Button.builder(Component.empty(),button->{
                List<String> known=knownActive();int position=page*9+idx;
                if(position<known.size())PacketDistributor.sendToServer(new MurimNetwork.AssignSkill(targetSlot,known.get(position)));
            }).bounds(cx-161+ix*108,top+104+iy*22,104,20).build());
            martialButtons.add(btn);
        }
        prevPage=addRenderableWidget(Button.builder(Component.literal("◀ 이전"),b->page=Math.max(0,page-1))
            .bounds(cx-160,top+175,70,20).build());
        nextPage=addRenderableWidget(Button.builder(Component.literal("다음 ▶"),b->page++)
            .bounds(cx+90,top+175,70,20).build());
        addRenderableWidget(Button.builder(Component.literal("닫기"),b->onClose())
            .bounds(cx-38,top+213,76,20).build());
    }

    private void left(GuiGraphics g,String message,int x,int y,int maxWidth,int color){
        String text=message==null?"":message;
        if(text.isBlank())text="기록 없음";
        boolean shortened=false;
        while(text.length()>1 && font.width(text)>maxWidth-8){text=text.substring(0,text.length()-1);shortened=true;}
        g.drawString(font,shortened?text+"…":text,x,y,color,true);
    }
    private void line(GuiGraphics g,int y){g.fill(width/2-163,y,width/2+163,y+1,0xFF53645B);}
    private void row(GuiGraphics g,String title,String details,int y){
        int left=width/2-153;
        g.drawString(font,title,left,y,0xFFFFDDA3,true);
        left(g,details,left+76,y,232,0xFFF6F1E6);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick) {
        // Do not call Screen.renderBackground(): it invokes the game's world blur.
        // Dim the world, but draw every menu panel fully opaque and use font shadows.
        g.fill(0,0,width,height,0x99000000);
        int cx=width/2,top=height/2-105;
        g.fill(cx-181,top-25,cx+181,top+245,0xFF0C1515);
        g.fill(cx-176,top-20,cx+176,top+241,0xFF21302B);
        g.fill(cx-176,top-20,cx+176,top-17,0xFFF0C681);
        g.fill(cx-167,top+74,cx+167,top+208,0xFF121F20);
        g.fill(cx-167,top+74,cx+167,top+76,0xFF947A53);
        g.pose().pushPose();
        g.pose().translate(cx,top-11,0);
        g.pose().scale(1.25F,1.25F,1F);
        g.drawCenteredString(font,"江 湖 錄 · 강호록",0,0,0xFFFFE2A8);
        g.pose().popPose();
        String summary=ClientProfileCache.summary;
        String[] parts=summary==null?new String[0]:summary.split(" / ");
        left(g,parts.length>0?parts[0]:"경지를 불러오는 중",cx-156,top+8,312,0xFFFFFFFF);
        String second=parts.length>1?String.join("   |   ",Arrays.copyOfRange(parts,1,parts.length)):"전투 정보 동기화 중";
        left(g,second,cx-156,top+22,312,0xFFE8D5B1);
        left(g,"소속  "+ClientProfileCache.sect,cx-156,top+36,312,0xFFE0EDE4);

        boolean skillTab=tab==1;
        slotPicker.visible=skillTab;
        List<String> known=knownActive();
        int maxPage=Math.max(0,(known.size()-1)/9);page=Math.min(page,maxPage);
        for(int i=0;i<martialButtons.size();i++){
            Button button=martialButtons.get(i);int index=page*9+i;
            button.visible=skillTab && index<known.size();button.active=button.visible;
            if(button.visible)button.setMessage(Component.literal(known.get(index)));
        }
        for(int i=0;i<tabButtons.size();i++){
            tabButtons.get(i).setMessage(Component.literal((i==tab?"§6":"§f")+TABS[i]));
        }
        prevPage.visible=skillTab;nextPage.visible=skillTab;
        prevPage.active=page>0;nextPage.active=page<maxPage;
        if(tab==0){
            row(g,"경지·전투력","수련과 실제 전투에 따라 변화",top+88);
            line(g,top+105);
            row(g,"조작","M 강호록  /  R 무공 선택  /  G 패링",top+116);
            line(g,top+134);
            left(g,"별호와 소속은 월드 저장 데이터에 기록됩니다.",cx-153,top+146,306,0xFFFFFFFF);
            left(g,"상세 정보: /murim status",cx-153,top+165,306,0xFFCEDECE);
            left(g,"대회·문파 모집: /murim tournament, /murim recruitment",cx-153,top+184,306,0xFFD8C9A7);
        } else if(tab==1){
            left(g,"배운 액티브 무공: "+known.size()+"개    "+(page+1)+" / "+(maxPage+1)+"쪽",cx-153,top+197,306,0xFFFFDDA3);
        } else if(tab==2){
            left(g,"기본 능력 · 실전 및 수련으로 성장",cx-153,top+84,306,0xFFFFDDA3);
            line(g,top+99);
            String[] stats=ClientProfileCache.stats==null?new String[0]:ClientProfileCache.stats.split("  ");
            for(int i=0;i<Math.min(stats.length,15);i++){
                int x=cx-152+(i%3)*108,y=top+111+(i/3)*18;
                left(g,stats[i],x,y,100,0xFFF7F6E9);
            }
            if(stats.length==0)left(g,"스탯 정보를 기다리는 중",cx-152,top+116,300,0xFFE0E5D9);
        } else if(tab==3){
            row(g,"무림맹","정파 문파 공식 등록",top+90);
            line(g,top+111);
            row(g,"창설","사파 · 혈교 · 천마신교",top+123);
            line(g,top+144);
            left(g,"특별 창설 조건과 비용은 추후 구현됩니다.",cx-153,top+158,305,0xFFE7DFD0);
            left(g,"현재 상태: 문파 창설 UI 설계 단계",cx-153,top+181,305,0xFFFFDDA3);
        } else {
            row(g,"팀","동료 및 플레이어 파티",top+92);
            line(g,top+112);
            left(g,"같은 점수판 팀원 간 피해를 방지합니다.",cx-153,top+126,306,0xFFF0EBDA);
            left(g,"동료 영입·초대 UI는 개발 중입니다.",cx-153,top+157,306,0xFFFFDDA3);
        }
        super.render(g,mx,my,partialTick);
    }
    private static List<String> knownActive(){
        if(ClientProfileCache.arts==null || ClientProfileCache.arts.isBlank())return List.of();
        return Arrays.stream(ClientProfileCache.arts.split("\\|"))
            .filter(name->!name.isBlank())
            .filter(name->{var art=MurimCombat.ARTS.get(name);return art!=null && !"passive".equals(art.type());})
            .toList();
    }
    @Override public boolean isPauseScreen(){return false;}
}
