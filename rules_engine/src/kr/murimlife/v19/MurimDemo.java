package kr.murimlife.v19;

import java.nio.file.*;
import java.util.*;

/** A *headless simulation*, not Minecraft. Useful to verify design logic. */
public final class MurimDemo {
    public static void main(String[] args)throws Exception {
        var world=new MurimEngine.World(20261123);
        var hero=world.addActor("플레이어",false);
        var rival=world.addActor("백검",true);
        rival.stats.put("기교",32.0);rival.updatePeak();
        System.out.println("세계 상태: 마교="+world.demonFounded+", 혈교="+world.bloodFounded);
        System.out.println("시작 문파 모집: "+world.sects.values().stream().filter(s->s.recruitmentOpen(world.day)).map(s->s.name).toList());
        System.out.println("별호 검객: "+world.claimTitle(hero,"검객")+" / NPC 중복: "+world.claimTitle(rival,"검객"));
        hero.train("근력",50);hero.rest();
        System.out.println("수련 뒤 경지="+hero.title()+", 전투력="+String.format(Locale.ROOT,"%.1f",hero.combatPower()));
        var slash=new MurimEngine.Martial("basic_sword","기본검격","무소속","기초무공","active","jian","blade_combo",1.0,4,2,12,"original_game_design");
        rival.stance=MurimEngine.Stance.PARRY;rival.parryTicks=6;
        var combat=MurimEngine.Combat.strike(hero,rival,slash,"강철",new Random(10),world);
        System.out.println("공격 이벤트: "+combat.events());
        world.advanceDays(30);System.out.println("신진 대회 참가 가능="+world.newcomerTournament(hero));
        Path p=args.length>0?Path.of(args[0]):Files.createTempFile("murim-test-",".save");
        MurimSave.write(world,p);var restored=MurimSave.read(p);
        System.out.println("저장/불러오기: "+restored.actors.size()+"명, 현재 "+restored.day+"일, 별호="+restored.titleOwners);
        if(args.length==0)Files.deleteIfExists(p);
    }
}
