package kr.murimlife.npc.dialogue;
/** Pure rules: no Minecraft dependencies; only the server decides whether talking stops a fight. */
public final class DialogueRules {
    /** Protect only mutually directed attacks, never a self-hit or third party. */
    public static boolean pairedDamage(java.util.UUID player,java.util.UUID npc,
                                       java.util.UUID victim,java.util.UUID attacker){
        return attacker!=null && ((victim.equals(player) && attacker.equals(npc)) ||
                                  (victim.equals(npc) && attacker.equals(player)));
    }
    public static final int COMBAT_CHOICE_TICKS=300; // 15 seconds per answer
    public static final int OFFER_TICKS=200; // NPC invitation expires in 10 seconds
    public static final int MAX_DISTANCE_SQUARED=36;
    private DialogueRules(){}
    public static boolean choiceValid(int choice){return choice>=0&&choice<6;}
    public static boolean expired(boolean combat,long now,long end){return combat && now>=end;}
    public static boolean mayTalk(double distanceSquared, boolean alive, boolean spectator){
        return alive && !spectator && distanceSquared<=MAX_DISTANCE_SQUARED;
    }
    public static double recoverFatigue(double fatigue,double amount){
        return Math.max(0,Math.min(100,fatigue)-Math.max(0,amount));
    }
    public static String label(int option,boolean combat){
        if(!choiceValid(option))throw new IllegalArgumentException("Invalid option " + option);
        String[] combatLabels={"무승부 제안","시간 끌기·대화","도발","목숨 구걸·자비","무공·경지 정보","대화 끝내기"};
        String[] dailyLabels={"인사·안부","잡담","농담·가벼운 도발","부탁·조언","무공·경지 정보","대화 끝내기"};
        return (combat?combatLabels:dailyLabels)[option];
    }
}
