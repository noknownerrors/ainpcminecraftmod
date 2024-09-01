package main.java.com.noknownerrors.ainpcmod;

import main.java.com.noknownerrors.ainpcmod.ModEntities;
import main.java.com.noknownerrors.ainpcmod.TreeChopperNPC;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ainpcmod", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TREE_CHOPPER_NPC.get(), TreeChopperNPC.createAttributes().build());
    }
}
