package kr.murimlife;
/** Client-only display cache. Authoritative validation always runs on the server. */
public final class ClientProfileCache {
    private ClientProfileCache(){}
    public static String summary="서버 정보를 불러오는 중", arts="기본검격", stats="", sect="무소속", slots="";
    public static void accept(MurimNetwork.ProfileSync p){
        summary=p.summary();arts=p.arts();stats=p.stats();sect=p.sect();slots=p.slots();
    }
}
