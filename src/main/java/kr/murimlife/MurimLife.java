package kr.murimlife;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import kr.murimlife.worldgen.WorldgenEngine;
import kr.murimlife.npc.NpcResidentService;
import kr.murimlife.npc.NpcArenaService;
import kr.murimlife.npc.dialogue.MurimDialogueService;
import kr.murimlife.npc.MurimNpcTypes;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.epic.MurimEpicBridge;
import kr.murimlife.npc.combat.MurimClashEvents;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

@Mod(MurimLife.MOD_ID)
public final class MurimLife {
    public static final String MOD_ID = "murimlife";
    public MurimLife(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        MurimNpcTypes.ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e)->e.put(MurimNpcTypes.HUMAN.get(),MurimHumanoid.attributes().build()));
        modBus.addListener((FMLCommonSetupEvent e)->e.enqueueWork(MurimEpicBridge::registerArmature));
        MurimEpicBridge.register();
        if(FMLEnvironment.dist==Dist.CLIENT)kr.murimlife.npc.client.MurimNpcClient.register(modBus);
        ModItems.ARMOR_MATERIALS.register(modBus);
        ModItems.ITEMS.register(modBus);
        MurimCreativeTab.TABS.register(modBus);
        modBus.addListener(MurimNetwork::register);
        // One integrated mod: register v1.17 worldgen on the same NeoForge runtime bus.
        // The generator is OPT-IN (config/murimlife-worldgen.json); no separate @Mod entry.
        NeoForge.EVENT_BUS.addListener(WorldgenEngine::onTick);
        NeoForge.EVENT_BUS.addListener(NpcResidentService::onTick);
        NeoForge.EVENT_BUS.addListener(NpcResidentService::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(NpcArenaService::onTick);
        NeoForge.EVENT_BUS.addListener(MurimDialogueService::onTick);
        NeoForge.EVENT_BUS.addListener(MurimDialogueService::onDamage);
        NeoForge.EVENT_BUS.addListener(NpcArenaService::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(MurimClashEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(NpcArenaService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(MurimClashEvents::onPlayerSwing);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.opportunity.OpportunitySites::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.opportunity.OpportunityTraps::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.economy.SectPayroll::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.economy.NpcSectRoster::onDeath);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.economy.TheftService::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.economy.RumorLedger::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.economy.SectDiscipline::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.quest.QuestService::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.quest.ReputationService::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.cult.CultRoster::onDeath);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.cult.CultRoster::onJoin);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.cult.CultRaidService::tick);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.cult.CultRaidService::onDeath);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.cult.CultRaidService::onJoin);
        NeoForge.EVENT_BUS.addListener(kr.murimlife.qi.QiDeviationService::tick);
    }
}
