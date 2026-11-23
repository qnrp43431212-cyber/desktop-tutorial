from pathlib import Path
base=Path(__file__).resolve().parents[1]/'src/main/java/kr/murimlife'

def edit(rel,fn):
 p=base/rel; s=p.read_text(encoding='utf-8'); t=fn(s)
 if s==t: print('NOT MODIFIED',rel)
 p.write_text(t,encoding='utf-8')
 print('UPDATED',rel)

def murim(s):
 s=s.replace('private static void tell(ServerPlayer p,String text){p.sendSystemMessage(Component.literal("[무림 대화] "+text));}',
             'private static void tell(ServerPlayer p,String text){MurimPanelServer.notice(p,text);}')
 s=s.replace('tell(player,"대화 중입니다. 채팅창에서 선택하거나 종료하십시오.");',
             'tell(player,"대화가 열려 있습니다. Alt를 누른 채 선택지를 클릭하십시오.");')
 a=s.index('    private static void showOptions(ServerPlayer player,Session s){')
 b=s.index('    public static void choose(',a)
 block='''    private static void showOptions(ServerPlayer player,Session s){
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
'''
 s=s[:a]+block+s[b:]
 s=s.replace('if(p!=null)tell(p,why);','if(p!=null){MurimPanelServer.close(p);tell(p,why);}')
 prefix='''                player.sendSystemMessage(clickable("대화 수락 (~키도 가능)","/murim talk reply "+offer.token+" 1"));
                player.sendSystemMessage(clickable("거절","/murim talk reply "+offer.token+" 0"));'''
 repl='''                MurimPanelServer.show(player,label(npc),"상대가 대화를 신청했습니다.",List.of(
                    MurimPanelServer.option("대화 수락","/murim talk reply "+offer.token+" 1"),
                    MurimPanelServer.option("거절","/murim talk reply "+offer.token+" 0")));'''
 assert prefix in s
 s=s.replace(prefix,repl)
 prefix='''            player.sendSystemMessage(clickable("수락 (~키도 가능)","/murim talk reply "+offer.token+" 1"));
            player.sendSystemMessage(clickable("거절","/murim talk reply "+offer.token+" 0"));'''
 assert prefix in s
 s=s.replace(prefix,'''            MurimPanelServer.show(player,label(npc),"상대가 먼저 말을 걸었습니다.",List.of(
                MurimPanelServer.option("수락","/murim talk reply "+offer.token+" 1"),
                MurimPanelServer.option("거절","/murim talk reply "+offer.token+" 0")));''')
 s=s.replace('if(!accept){tell(player,"상대의 대화 요청을 거절했습니다.");return;}','if(!accept){MurimPanelServer.close(player);tell(player,"상대의 대화 요청을 거절했습니다.");return;}')
 return s
edit('npc/dialogue/MurimDialogueService.java',murim)

def civic(s):
 s=s.replace('private static void say(ServerPlayer p,String t){p.sendSystemMessage(Component.literal("[무림 대화] "+t));}',
             'private static void say(ServerPlayer p,String t){MurimPanelServer.notice(p,t);}')
 s=s.replace('''        for(int i=0;i<OPTIONS.length;i++){
            final int index=i+1;
            p.sendSystemMessage(Component.literal("["+index+". "+OPTIONS[i]+"]").withStyle(style->style.withColor(ChatFormatting.AQUA)
                .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/murim civic choose "+s.token()+" "+index))));
        }''','''        List<MurimPanelServer.Choice> options=new ArrayList<>();
        for(int i=0;i<OPTIONS.length;i++)options.add(MurimPanelServer.option((i+1)+". "+OPTIONS[i],
            "/murim civic choose "+s.token()+" "+(i+1)));
        MurimPanelServer.show(p,"객잔·마을 · 정보 대화","Alt 누른 채 선택 · 마우스 휠로 목록 이동",options);''')
 s=s.replace('if(option==10){OPEN.remove(p.getUUID());say(p,"대화를 마쳤습니다.");return;}',
             'if(option==10){OPEN.remove(p.getUUID());MurimPanelServer.close(p);say(p,"대화를 마쳤습니다.");return;}')
 s=s.replace('if(option==7){OPEN.remove(p.getUUID());TheftService.start(p,npc);return;}',
             'if(option==7){OPEN.remove(p.getUUID());MurimPanelServer.close(p);TheftService.start(p,npc);return;}')
 return s
edit('npc/dialogue/CivicDialogueService.java',civic)

def dispute(s):
 a=s.index('        p.sendSystemMessage(Component.literal("[강호 분쟁]')
 b=s.index('    }\n    public static void choose(',a)
 s=s[:a]+'''        List<MurimPanelServer.Choice> options=new ArrayList<>();
        for(int i=1;i<=4;i++){
            String title=switch(i){case 1->"환불과 사과 요구";case 2->"위협하여 두 배 배상 요구 (악업)";
                case 3->"시장에 거짓 정보 고발";default->"이번만 용서";};
            options.add(MurimPanelServer.option(i+". "+title,"/murim dispute choose "+i));
        }
        MurimPanelServer.show(p,"강호 분쟁 · 상인 추궁","허위정보를 판 상인에게 어떻게 대응하겠습니까?",options);
'''+s[b:]
 s=s.replace('Request request=REQUESTS.remove(p.getUUID());','Request request=REQUESTS.remove(p.getUUID());\n        MurimPanelServer.close(p);')
 return s
edit('npc/dialogue/DisputeService.java',dispute)

def merchant(s):
 s=s.replace('import kr.murimlife.npc.dialogue.NpcRoles;','import kr.murimlife.npc.dialogue.NpcRoles;\nimport kr.murimlife.npc.dialogue.MurimPanelServer;')
 start=s.index('    private static void render(ServerPlayer player,LivingEntity npc){')
 end=s.index('    private static LivingEntity valid(',start)
 s=s[:start]+'''    private static void render(ServerPlayer player,LivingEntity npc){
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
'''+s[end:]
 s=s.replace('MoneyService.message(p,"구매 완료: "+goods.getHoverName().getString());','MoneyService.message(p,"구매 완료: "+goods.getHoverName().getString());\n        render(p,npc);')
 s=s.replace('MoneyService.message(p,name+" 1개 판매 완료: +"+CurrencyRules.describe(price));','MoneyService.message(p,name+" 1개 판매 완료: +"+CurrencyRules.describe(price));\n        render(p,npc);')
 return s
edit('economy/MerchantService.java',merchant)

def quest(s):
 s=s.replace('import kr.murimlife.npc.dialogue.NpcRoles;','import kr.murimlife.npc.dialogue.NpcRoles;\nimport kr.murimlife.npc.dialogue.MurimPanelServer;')
 s=s.replace('''        for(int i=0;i<=max;i++)player.sendSystemMessage(click(QuestCatalog.GRADES[i]+" 등급 임무 보기", "/murim quest rank "+i));''','''        List<MurimPanelServer.Choice> options=new ArrayList<>();
        for(int i=0;i<=max;i++)options.add(MurimPanelServer.option(
            QuestCatalog.GRADES[i]+" 등급 임무 보기", "/murim quest rank "+i));
        MurimPanelServer.show(player,"무림 의뢰소","등급 선택 · 3일마다 갱신",options);''')
 s=s.replace('''        int n=0;for(String id:chosen){var q=QuestCatalog.find(id);n++;
            p.sendSystemMessage(click(n+". "+q.title()+" / "+description(q)+" / "+CurrencyRules.describe(q.rewardMun()),
                "/murim quest accept "+n));
        }''','''        List<MurimPanelServer.Choice> options=new ArrayList<>();
        int n=0;for(String id:chosen){var q=QuestCatalog.find(id);n++;
            options.add(MurimPanelServer.option(n+". "+q.title()+" / "+description(q)+" / "+CurrencyRules.describe(q.rewardMun()),
                "/murim quest accept "+n));
        }
        MurimPanelServer.show(p,"무림 의뢰소 · "+QuestCatalog.GRADES[grade],"표시 5개 · 3일마다 재등록",options);''')
 s=s.replace('''        say(p,"임무 수락: "+q.title()+" · "+description(q)+" / 3일 내 완수. /murim quest status");''','''        say(p,"임무 수락: "+q.title()+" · "+description(q)+" / 3일 내 완수. /murim quest status");
        MurimPanelServer.close(p);''')
 return s
edit('quest/QuestService.java',quest)
