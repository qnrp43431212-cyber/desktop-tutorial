package kr.murimlife.rules;

public final class CultRules {
    public enum WorldState { BOTH, ONLY_DEMON, ONLY_BLOOD }
    public enum Cult { DEMON, BLOOD }
    private CultRules() {}
    public static WorldState fromRoll(int roll) {
        return switch (Math.floorMod(roll, 3)) {
            case 0 -> WorldState.BOTH;
            case 1 -> WorldState.ONLY_DEMON;
            default -> WorldState.ONLY_BLOOD;
        };
    }
    public static boolean exists(WorldState world,Cult cult) {
        return world == WorldState.BOTH || world == WorldState.ONLY_DEMON && cult == Cult.DEMON
            || world == WorldState.ONLY_BLOOD && cult == Cult.BLOOD;
    }
    public static boolean canFound(boolean alreadyExists, boolean hasRelic, boolean passedTrial) {
        return !alreadyExists && hasRelic && passedTrial;
    }
    public static boolean canClaimLeadership(boolean founder, boolean leaderDefeated) {
        return founder || leaderDefeated;
    }
}
