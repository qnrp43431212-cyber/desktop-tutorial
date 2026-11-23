package kr.murimlife.npc.combat;
/** Server + client motion smoothing math. Changes only Murim NPCs, never Epic Fight's global engine. */
public final class MotionBlend {
    private MotionBlend() {}
    public static float approach(float current,float target,float factor,float maxDelta) {
        if(!Float.isFinite(current)||!Float.isFinite(target))return 0;
        float delta=(target-current)*Math.clamp(factor,0f,1f);
        return current+Math.clamp(delta,-Math.max(0f,maxDelta),Math.max(0f,maxDelta));
    }
    public static float approachDegrees(float current,float target,float factor,float maxDegrees) {
        float delta=wrap(target-current)*Math.clamp(factor,0f,1f);
        return wrap(current+Math.clamp(delta,-Math.max(0,maxDegrees),Math.max(0,maxDegrees)));
    }
    public static float wrap(float angle) {
        float result=angle%360f;
        return result>=180f?result-360f:result< -180f?result+360f:result;
    }
    public static float movementSpeed(float previous,float requested,boolean duel) {
        return approach(previous,Math.clamp(requested,0f,duel?1.4f:1.0f),duel?.31f:.18f,duel?.24f:.12f);
    }
}
