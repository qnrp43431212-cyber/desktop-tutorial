package kr.murimlife.v16;

import java.util.*;
/** Seven weapon grades, sixteen families. Server authoritative compatibility and balancing. */
public final class WeaponRules {
    private WeaponRules() {}
    public enum Grade {
        COMMON("범철",1.0,200), REFINED("정철",1.15,350), STEEL("강철",1.35,500),
        FOLDED("백련강",1.6,750), BLACK_IRON("현철",1.95,1100), MASTER("명장",2.3,1400),
        DIVINE("신병",2.8,1900);
        public final String name; public final double multiplier; public final int durability;
        Grade(String name,double multiplier,int durability) { this.name=name;this.multiplier=multiplier;this.durability=durability; }
    }
    public enum Family { JIAN, DAO, GREAT_JIAN, GREAT_DAO, DAGGER, SPEAR, STAFF, GLAIVE, FAN, WHIP, CHAIN, CLAW, GAUNTLET, NEEDLES, THROWING_KNIFE, HAMMER }
    public static boolean similar(Family a,Family b) {
        return (a==Family.JIAN && b==Family.DAO) || (a==Family.DAO && b==Family.JIAN)
            || (a==Family.GREAT_JIAN && b==Family.JIAN) || (a==Family.JIAN && b==Family.GREAT_JIAN)
            || (a==Family.GREAT_DAO && b==Family.DAO) || (a==Family.DAO && b==Family.GREAT_DAO)
            || (a==Family.SPEAR && b==Family.GLAIVE) || (a==Family.GLAIVE && b==Family.SPEAR)
            || (a==Family.STAFF && b==Family.SPEAR) || (a==Family.SPEAR && b==Family.STAFF)
            || (a==Family.DAGGER && b==Family.THROWING_KNIFE) || (a==Family.CLAW && b==Family.GAUNTLET);
    }
    public static double effectiveness(Family required, Family equipped,boolean passive) {
        if(passive) return 1.0;
        if(equipped==null) return 0;
        return required==equipped ? 1.0 : similar(required,equipped) ? .67 : 0.0;
    }
    public static double effectiveDamage(double base, Grade grade,Family required,Family equipped,boolean passive) {
        return base * grade.multiplier * effectiveness(required,equipped,passive);
    }
}
