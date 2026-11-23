package kr.murimlife.cult;

/** Server-side minimum realm gates. Ranks are never awarded through UI alone. */
public final class CultRankRules {
    private CultRankRules() {}
    // World realm indices: 삼류=0, 이류=1, 일류=2, 절정=3, 초절정=4, 화경=5, 현경=6, 생사경=7.
    public static int minimum(String rank) {
        return switch (rank) {
            case "외문제자", "평교도" -> 0;
            case "내문제자", "정예교도" -> 1;
            case "사부", "전주" -> 3;
            case "장로", "마두" -> 4;
            case "대마두", "호법" -> 5;
            case "가주" -> 4;
            case "장문인", "방장", "방주", "대호법", "좌호법", "우호법" -> 6;
            case "교주", "천마", "혈마" -> 7;
            default -> 8; // Unknown titles are not silently granted.
        };
    }
    public static boolean eligible(String rank,int realm){return realm>=minimum(rank)&&realm<=7;}
    public static int seatMinimum(int seat) {
        if(seat<0||seat>=12)throw new IllegalArgumentException("Unknown cult seat: "+seat);
        return seat<4?6:seat<8?5:4;
    }
    /** Vacant senior seat belongs to the nearest *qualified* lower member, never a conjured replacement. */
    public static int qualifiedSuccessor(int vacancy,int[] realms) {
        if(vacancy<0||vacancy>=12||realms.length!=12)throw new IllegalArgumentException("Seat data invalid");
        for(int i=vacancy+1;i<12;i++)if(realms[i]>=seatMinimum(vacancy))return i;
        return -1;
    }
}
