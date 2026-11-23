package kr.murimlife.v19;

import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Pure Java 21 deterministic rules engine for MURIM LIFE.
 * No Minecraft dependencies; deliberately separate from the NeoForge adapter.
 * Provisional balance numbers are CONFIGURATION, not final design choices.
 */
public final class MurimEngine {
    private MurimEngine(){}
    public static final String[] REALMS={"삼류","이류","일류","절정","초절정","화경","현경","생사경"};
    public static final String[] STATS={"근력","민첩","체질","끈기","기교","감각","정신력","오성","기감"};
    public static final String[] QUALITIES={"범철","정철","강철","백련강","현철","명장","신병"};
    public static final List<String> DEFAULT_SECTS=List.of("소림","무당","화산","아미","청성","곤륜","공동","점창","종남","개방","남궁세가","사천당문","하북팽가","제갈세가","천마신교","혈교");
    // Experimental values only; tuned with tests instead of presented as canon.
    private static final double[] REALM_FLOORS={0,160,240,360,600,950,1470,2400,4000};
    public enum Stance { NEUTRAL, ATTACK, GUARD, PARRY, FEINT, EVADE, RECOVER }
    public enum EventKind { SPARKS, CLASH, PARRY, PERFECT_PARRY, OPENING, STAGGER, HIT, MISS, RETREAT, SURRENDER }
    public enum FateStatus { HIDDEN, RUMORED, CLAIMED, FALSE_RUMOR, TAKEN_BY_OTHER }
    public enum SectType { ORTHODOX, UNORTHODOX, DEMON, BLOOD }
    public enum ConflictPhase { DISAGREEMENT, ARGUMENT, THREAT, PHYSICAL, FIGHT, MEDIATED, DISPERSED }
    public enum LearningSource { SECT_TEACHER, BOOKSELLER, SECRET_MANUAL, MASTER, FATE, RESEARCH }

    public static final class Actor {
        public final UUID id; public final String name; public final boolean npc;
        public final Map<String, Double> stats=new LinkedHashMap<>();
        public final Map<String, Integer> skillMastery=new LinkedHashMap<>();
        public final Set<String> knownMartials=new LinkedHashSet<>();
        public final Set<String> relics=new LinkedHashSet<>();
        public final LinkedHashSet<String> epithets=new LinkedHashSet<>();
        public final Map<UUID,Integer> affection=new HashMap<>();
        public final Map<UUID,Integer> trust=new HashMap<>();
        public final Map<String,Integer> reputation=new HashMap<>();
        public final Set<String> knownRumors=new HashSet<>();
        public String displayedEpithet="",sect="무소속",weapon="jian",trait="평범한 체질",team="";
        public UUID spouse=null; public boolean adult=true,alive=true;
        public int realm=0,good=0,evil=0,silver=0,wins=0,trainingDays=0;
        public long becameMartialAt=0;
        public double fatigue=0,inner=100,stamina=100,injury=0,breath=100,balance=100;
        public double potentialBonus=0,peakPower=0;
        public Stance stance=Stance.NEUTRAL;
        public int openingTicks=0,parryTicks=0;
        public Actor(UUID id,String name,boolean npc){
            this.id=Objects.requireNonNull(id);this.name=Objects.requireNonNull(name);this.npc=npc;
            for(String s:STATS)stats.put(s,10.0);
        }
        public double stat(String name){return stats.getOrDefault(name,0.0);}
        public double intrinsicPower(){
            double sum=0;
            double[] weights={1.20,1.07,1.08,.98,1.16,1.13,.93,.92,1.02};
            for(int i=0;i<STATS.length;i++)sum+=stat(STATS[i])*weights[i];
            int mastery=skillMastery.values().stream().mapToInt(Integer::intValue).sum();
            return Math.max(1,sum+mastery*.2+inner*.08+potentialBonus);
        }
        public double combatPower(){
            double fatiguePenalty=Math.max(.35,1-fatigue*.006);
            double injuryPenalty=Math.max(.30,1-injury*.007);
            double resourcePenalty=.6+.4*Math.min(1,stamina/100);
            return intrinsicPower()*fatiguePenalty*injuryPenalty*resourcePenalty;
        }
        public void updatePeak(){peakPower=Math.max(peakPower,intrinsicPower());}
        public String subStage(){
            // The stage is a measure of developed potential, not a transient debuff.
            double low=REALM_FLOORS[realm],next=REALM_FLOORS[realm+1];
            double p=Math.max(peakPower,intrinsicPower());
            if(p>=next*1.13)return "초월";
            double f=(p-low)/Math.max(1,next-low);
            return f<.34?"초입":f<.70?"소성":"대성";
        }
        public String title(){return REALMS[realm]+" "+subStage();}
        public boolean canBreakthrough(){return subStage().equals("대성") || subStage().equals("초월");}
        public void train(String stat,int repetitions){
            if(!stats.containsKey(stat) || repetitions<1||repetitions>1000)throw new IllegalArgumentException("invalid training");
            for(int i=0;i<repetitions;i++){
                double old=stats.get(stat);
                double potentialFactor=1.0+Math.max(0,potentialBonus)*.0005;
                stats.put(stat,old+potentialFactor/(2.0+Math.pow(old/13.0,1.3)));
            }
            stamina=Math.max(0,stamina-repetitions*.5);
            fatigue=Math.min(100,fatigue+repetitions*.45);
            trainingDays+=repetitions/10;
            updatePeak();
        }
        public void rest(){fatigue=Math.max(0,fatigue-28);stamina=Math.min(100,stamina+45);breath=100;injury=Math.max(0,injury-2);}
        public int angerToward(Actor other){return (int)Math.max(0,other.evil/10.0-trust.getOrDefault(other.id,0));}
    }
    public record Martial(String id,String name,String sect,String tier,String kind,String weapon,String animation,
                          double impact,double damage,int innerCost,int cooldownTicks,String provenance) {
        public boolean passive(){return "passive".equals(kind);}
        public boolean legendary(){return "신공".equals(tier)||"전설급".equals(tier);}
    }
    public static final class Fate {
        public final String id,name,hint,reward;
        public FateStatus status=FateStatus.HIDDEN;
        public long discoveredDay=-1, resolvedDay=-1;
        public UUID firstDiscoverer=null,winner=null;
        public Fate(String id,String name,String hint,String reward){this.id=id;this.name=name;this.hint=hint;this.reward=reward;}
    }
    public static final class Sect {
        public final String name; public final SectType type;public final int recruitmentOffset;
        public final Set<UUID> members=new LinkedHashSet<>();
        public UUID leader;
        public int fame=1;
        public Sect(String name,SectType type,int offset){this.name=name;this.type=type;this.recruitmentOffset=offset;}
        public boolean recruitmentOpen(long day){return type==SectType.ORTHODOX && Math.floorMod(day-recruitmentOffset,21)<3;}
    }
    public record Contest(double dummyScore,double carryingScore,double sparScore,boolean passed,boolean offCycle){}
    public record CombatEvent(EventKind kind,double intensity,String detail){}
    public record CombatResult(double damage,boolean hit,boolean clashed,boolean parried,
                               List<CombatEvent> events){}
    public static final class World {
        public final long seed;public long day=0;
        public final Map<UUID,Actor> actors=new LinkedHashMap<>();
        public final Map<String,Sect> sects=new LinkedHashMap<>();
        public final Map<String,UUID> titleOwners=new LinkedHashMap<>();
        public final Map<String,Fate> fates=new LinkedHashMap<>();
        public final Set<UUID> alreadyWonNewcomer=new HashSet<>();
        public boolean demonFounded,bloodFounded;
        public final List<String> journal=new ArrayList<>();
        public World(long seed){
            this.seed=seed;
            int roll=new Random(seed^0x6c8e9cf5aa111c32L).nextInt(3);
            demonFounded=roll!=2; bloodFounded=roll!=1;
            for(int i=0;i<DEFAULT_SECTS.size();i++){
                String s=DEFAULT_SECTS.get(i);
                SectType type=s.equals("천마신교")?SectType.DEMON:s.equals("혈교")?SectType.BLOOD: i<=9?SectType.ORTHODOX:(i<=13?SectType.ORTHODOX:SectType.UNORTHODOX);
                int offset=(i==0)?0:Math.floorMod(Objects.hash(seed,s),21);
                sects.put(s,new Sect(s,type,offset));
            }
            addFate("demons_relic","마도귀환","절벽 너머 검은 철령에 대한 오래된 소문","천마계승령");
            addFate("blood_relic","혈해재림","지하 제단과 붉은 신물을 보았다는 증언","혈교성유령");
            addFate("lone_sword","무명의 검흔","산중 잊힌 절검벽의 검흔","독고구검");
            addFate("mountain_healer","산속 신의의 마지막 처방","희귀한 약초와 의술서의 소문","내상치유비록");
            addFate("hidden_valley","사라진 곡주의 발자국","안개계곡에서 발견된 고인의 흔적","비밀검보");
            if(!demonFounded && !bloodFounded)throw new AssertionError("cult invariant");
        }
        public void addFate(String id,String name,String hint,String reward){fates.put(id,new Fate(id,name,hint,reward));}
        public Actor addActor(String name,boolean npc){
            UUID id=UUID.nameUUIDFromBytes((seed+":"+name+":"+actors.size()).getBytes(StandardCharsets.UTF_8));
            Actor a=new Actor(id,name,npc);a.becameMartialAt=day;a.updatePeak();actors.put(id,a);return a;
        }
        public Actor addActor(UUID id,String name,boolean npc){Actor a=new Actor(id,name,npc);a.becameMartialAt=day;a.updatePeak();actors.put(id,a);return a;}
        public boolean claimTitle(Actor a,String name){
            UUID owner=titleOwners.get(name);
            if(name.isBlank()||owner!=null&&!owner.equals(a.id))return false;
            titleOwners.put(name,a.id);a.epithets.add(name);if(a.displayedEpithet.isBlank())a.displayedEpithet=name;return true;
        }
        public boolean setDisplayedTitle(Actor a,String name){if(!a.epithets.contains(name))return false;a.displayedEpithet=name;return true;}
        public void retireTitle(Actor a,String title){if(!a.epithets.remove(title))return;if(a.id.equals(titleOwners.get(title)))titleOwners.remove(title);if(a.displayedEpithet.equals(title))a.displayedEpithet=a.epithets.stream().findFirst().orElse("");}
        public boolean applicantIsNew(Actor a){return day-a.becameMartialAt>=30&&day-a.becameMartialAt<=43 && a.realm<=2;}
        public boolean newcomerTournament(Actor a){return applicantIsNew(a)&&!alreadyWonNewcomer.contains(a.id);}
        public boolean grandTournamentOpen(){return day>=42 && (day-42)%84<3;}
        public int grandPrize(Actor a){return a.realm<=2?800:a.realm<=4?2200:5000;}
        public int winTournament(Actor a,boolean newcomer){
            if(newcomer && !newcomerTournament(a))return 0;
            if(!newcomer && !grandTournamentOpen())return 0;
            int prize=newcomer?250:grandPrize(a);
            a.silver+=prize;a.wins+=3;a.good+=1;
            if(newcomer)alreadyWonNewcomer.add(a.id);
            claimTitle(a,newcomer?"신진무림대회 우승자":"비무대회 우승자");
            return prize;
        }
        public boolean unlockCult(Actor a,String item){
            if(item.equals("천마계승령") || item.equals("혈교성유령")){a.relics.add(item);return true;}return false;
        }
        public boolean foundCult(Actor a,String cult){
            if(cult.equals("천마신교") && !demonFounded && a.relics.contains("천마계승령")){
                demonFounded=true;Sect s=sects.get(cult);s.leader=a.id;s.members.add(a.id);a.sect=cult;return true;
            }
            if(cult.equals("혈교") && !bloodFounded && a.relics.contains("혈교성유령")){
                bloodFounded=true;Sect s=sects.get(cult);s.leader=a.id;s.members.add(a.id);a.sect=cult;return true;
            }
            return false;
        }
        public boolean conquerCult(Actor challenger,Actor chief,String cult){
            Sect sect=sects.get(cult);
            if(sect==null||!(sect.type==SectType.DEMON||sect.type==SectType.BLOOD)||!Objects.equals(sect.leader,chief.id)||chief.alive)return false;
            sect.leader=challenger.id;sect.members.add(challenger.id);challenger.sect=cult;return true;
        }
        public Sect createSect(Actor founder,String name,boolean officiallyRegistered,int silverCost){
            if(name.isBlank()||sects.containsKey(name)||silverCost<0||founder.silver<silverCost)return null;
            founder.silver-=silverCost;
            Sect s=new Sect(name,officiallyRegistered?SectType.ORTHODOX:SectType.UNORTHODOX,Math.floorMod(Objects.hash(seed,name),21));
            s.leader=founder.id;s.members.add(founder.id);sects.put(name,s);founder.sect=name;return s;
        }
        public Contest testOrthodox(Actor a,String sect,double target,double dummyDamage,int carryingTime,int carryingLimit,
                                    double sparQuality,boolean offCycle){
            Sect s=sects.get(sect);
            if(s==null||s.type!=SectType.ORTHODOX)throw new IllegalArgumentException("not orthodox");
            double ratio=dummyDamage/Math.max(1,target);
            double dummy=ratio>=1? Math.min(100,70+20*(ratio-1)):ratio>=.98?65:Math.max(0,65*ratio);
            double carrying= carryingTime<=carryingLimit?75:Math.max(0,75-(carryingTime-carryingLimit)*1.5);
            double spar=Math.max(0,Math.min(100,sparQuality));
            double total=dummy*.32+carrying*.29+spar*.39;
            boolean qualified=total>=(offCycle?78:65);
            return new Contest(dummy,carrying,spar,qualified,offCycle);
        }
        public boolean joinSect(Actor a,String sectName,Contest c){
            Sect sect=sects.get(sectName);
            if(sect==null||!c.passed|| (sect.type==SectType.DEMON&&!demonFounded) || (sect.type==SectType.BLOOD&&!bloodFounded))return false;
            if(!a.sect.equals("무소속"))sects.values().forEach(s->s.members.remove(a.id));
            a.sect=sectName;sect.members.add(a.id);return true;
        }
        public Fate inquireFate(Actor a,int inquiryCount){
            // Stable deterministic check for each inquiry, rare, no '5 questions guaranteed'.
            Random r=new Random(seed ^a.id.getLeastSignificantBits()^day*17389L^inquiryCount*0x9E3779B97F4A7C15L);
            if(r.nextDouble()>=.13)return null;
            List<Fate> available=fates.values().stream().filter(f->f.status==FateStatus.HIDDEN).toList();
            if(available.isEmpty())return null;
            Fate f=available.get(r.nextInt(available.size()));
            f.status=FateStatus.RUMORED;f.discoveredDay=day;f.firstDiscoverer=a.id;a.knownRumors.add(f.id);
            journal.add("day="+day+" "+a.name+" heard "+f.name);
            return f;
        }
        public boolean claimFate(Actor a,String fateId){
            Fate fate=fates.get(fateId);
            if(fate==null||fate.status!=FateStatus.RUMORED)return false;
            fate.status=FateStatus.CLAIMED;fate.resolvedDay=day;fate.winner=a.id;a.relics.add(fate.reward);
            if(fate.reward.equals("천마계승령")||fate.reward.equals("혈교성유령"))unlockCult(a,fate.reward);
            return true;
        }
        public boolean resolveFateByRival(String fateId,FateStatus outcome){
            Fate f=fates.get(fateId);
            if(f==null||f.status!=FateStatus.RUMORED||day-f.discoveredDay<2 || outcome==FateStatus.RUMORED||outcome==FateStatus.HIDDEN)return false;
            f.status=outcome;f.resolvedDay=day;return true;
        }
        public int befriend(Actor a,Actor b,int favor){
            if(a.id.equals(b.id))return 0;
            int v=Math.max(-15,Math.min(15,favor));
            int result=b.affection.merge(a.id,v,Integer::sum);
            if(v>0)b.trust.merge(a.id,Math.max(1,v/2),Integer::sum);
            return result;
        }
        public boolean marry(Actor a,Actor b){
            if(a.id.equals(b.id)||!a.adult||!b.adult||a.spouse!=null||b.spouse!=null||
                a.affection.getOrDefault(b.id,0)<75||b.affection.getOrDefault(a.id,0)<75||
                a.trust.getOrDefault(b.id,0)<45||b.trust.getOrDefault(a.id,0)<45)return false;
            a.spouse=b.id;b.spouse=a.id;return true;
        }
        public boolean recruitCompanion(Actor recruiter,Actor candidate){
            if(!candidate.npc ||candidate.trust.getOrDefault(recruiter.id,0)<35 ||candidate.affection.getOrDefault(recruiter.id,0)<50)return false;
            if(recruiter.team.isBlank())recruiter.team="party:"+recruiter.id;
            candidate.team=recruiter.team;return true;
        }
        public boolean allies(Actor a,Actor b){return a.id.equals(b.id)||(!a.team.isBlank()&&a.team.equals(b.team));}
        public boolean trainBreakthrough(Actor a,boolean meditationTrialWon,boolean defeatedStrongerEnemy,double gap){
            if(a.realm>=7||!a.canBreakthrough())return false;
            // Early tiers are simpler; from '일류->절정' onward major insight is mandatory.
            if(a.realm>=2 && !(meditationTrialWon||defeatedStrongerEnemy&&gap>=.18))return false;
            a.realm++;a.fatigue=Math.min(100,a.fatigue+12);a.updatePeak();return true;
        }
        public void nextDay(){
            day++;
            for(Actor a:actors.values()){
                if(a.npc&&a.alive){
                    // Growth rate slows as aptitude develops, no offscreen instant teleport.
                    a.train(STATS[Math.floorMod(Objects.hash(seed,day,a.id),STATS.length)],1);
                    if(day%3==0)a.rest();
                }
            }
            if(day%7==0)journal.add("day="+day+" wandering martial artists exchange news");
        }
        public void advanceDays(int n){if(n<0||n>50000)throw new IllegalArgumentException();for(int i=0;i<n;i++)nextDay();}
    }

    public static final class Combat {
        public static double compatibility(String required,String equipped){
            if(required==null||required.isBlank()||required.equals("unarmed"))return 1;
            if(required.equals(equipped))return 1;
            if((required.equals("jian")&&equipped.equals("dao"))||(required.equals("dao")&&equipped.equals("jian")))return .66;
            if(required.equals("staff")&&equipped.equals("spear"))return .65;
            if(required.equals("spear")&&equipped.equals("staff"))return .61;
            if(required.equals("claw")&&equipped.equals("gauntlet"))return .7;
            if(required.equals("gauntlet")&&equipped.equals("unarmed"))return 1;
            return 0;
        }
        public static int quality(String grade){for(int i=0;i<QUALITIES.length;i++)if(QUALITIES[i].equals(grade))return i;return 0;}
        public static CombatResult strike(Actor source,Actor target,Martial art,String weaponGrade,Random random,World world){
            if(!source.alive||!target.alive||world.allies(source,target))return new CombatResult(0,false,false,false,List.of());
            if(art.passive())return new CombatResult(0,false,false,false,List.of());
            double matching=compatibility(art.weapon,source.weapon);
            if(matching==0||source.inner<art.innerCost||source.stamina<5)return new CombatResult(0,false,false,false,List.of());
            source.inner-=art.innerCost;source.stamina=Math.max(0,source.stamina-8);source.fatigue=Math.min(100,source.fatigue+2);
            List<CombatEvent> events=new ArrayList<>();
            double fast=(source.stat("민첩")+source.stat("감각")+source.stat("기교"))/3.0;
            double resist=(target.stat("민첩")+target.stat("감각")+target.stat("기교"))/3.0;
            double chance=Math.max(.15,Math.min(.92,.65+(fast-resist)*.009+(target.openingTicks>0?.21:0)));
            if(target.stance==Stance.EVADE)chance*=.6;
            if(source.stance==Stance.FEINT && target.stance==Stance.GUARD){target.openingTicks=9;chance+=.16;events.add(new CombatEvent(EventKind.OPENING,1,"허초로 방어 틈 유도"));}
            if(random.nextDouble()>chance){events.add(new CombatEvent(EventKind.MISS,0,"회피/빗맞음"));return new CombatResult(0,false,false,false,events);}
            double damage=art.damage * Math.max(1,source.combatPower()/105.0)*matching*(1+quality(weaponGrade)*.16);
            if(target.stance==Stance.PARRY && target.parryTicks>0){
                double control=(target.stat("기교")+target.stat("감각")+target.balance*.15)/(source.stat("기교")+source.stat("민첩")+40.0);
                double success=Math.max(.17,Math.min(.90,.42+.38*(control-0.65)-target.fatigue*.002));
                if(random.nextDouble()<success){
                    boolean perfect=random.nextDouble()<.28;
                    double opening=perfect?15:8;
                    source.openingTicks=(int)opening;
                    source.balance=Math.max(0,source.balance-(perfect?25:12));
                    target.parryTicks=0;
                    events.add(new CombatEvent(perfect?EventKind.PERFECT_PARRY:EventKind.PARRY,1,"패링 성공"));
                    events.add(new CombatEvent(EventKind.SPARKS,0.7,"무기 불꽃"));
                    return new CombatResult(0,false,false,true,events);
                }
                damage*=.65;events.add(new CombatEvent(EventKind.CLASH,.4,"패링 불완전"));
            }
            boolean clash=source.stance==Stance.ATTACK && target.stance==Stance.ATTACK;
            if(clash){
                double diff=source.combatPower()-target.combatPower();
                events.add(new CombatEvent(EventKind.SPARKS,.75,"합 교환 불꽃"));
                events.add(new CombatEvent(EventKind.CLASH,Math.abs(diff),"무기 충돌"));
                damage*=.42;
                if(Math.abs(diff)>25){
                    Actor pushed=diff>0?target:source;
                    pushed.balance=Math.max(0,pushed.balance-Math.min(28,Math.abs(diff)*.08));
                    events.add(new CombatEvent(EventKind.STAGGER,Math.abs(diff),pushed.name+" 밀림"));
                }
            }
            if(target.stance==Stance.GUARD)damage*=.48;
            if(target.openingTicks>0){damage*=1.32;events.add(new CombatEvent(EventKind.OPENING,1,"빈틈 공격"));}
            target.injury=Math.min(100,target.injury+damage*.9);
            target.stamina=Math.max(0,target.stamina-damage*.35);
            target.balance=Math.max(0,target.balance-damage*.22);
            events.add(new CombatEvent(EventKind.HIT,damage,"명중"));
            source.wins+=0; // Winning only occurs after confirmed battle outcome.
            return new CombatResult(damage,true,clash,false,List.copyOf(events));
        }
    }

    public static final class SocialConflict {
        public final Actor a,b;public ConflictPhase phase=ConflictPhase.DISAGREEMENT;
        public int tension=0;public boolean bystandersIntervened=false;
        public SocialConflict(Actor a,Actor b){this.a=a;this.b=b;}
        public ConflictPhase step(Random r,Actor mediator){
            if(phase==ConflictPhase.FIGHT||phase==ConflictPhase.MEDIATED||phase==ConflictPhase.DISPERSED)return phase;
            tension+=5+Math.max(0,a.evil+b.evil)/10;
            if(mediator!=null && mediator.alive && r.nextDouble()<Math.max(.10,Math.min(.85,.32+mediator.combatPower()/(a.combatPower()+b.combatPower()+100)))){
                bystandersIntervened=true;phase=ConflictPhase.MEDIATED;return phase;
            }
            if(r.nextDouble()<.13){phase=ConflictPhase.DISPERSED;return phase;}
            phase=switch(phase){
                case DISAGREEMENT -> ConflictPhase.ARGUMENT;
                case ARGUMENT -> ConflictPhase.THREAT;
                case THREAT -> ConflictPhase.PHYSICAL;
                case PHYSICAL -> ConflictPhase.FIGHT;
                default -> phase;
            };
            return phase;
        }
    }
}
