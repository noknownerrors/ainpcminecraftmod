package main.java.com.noknownerrors.ainpcmod;

import main.java.com.noknownerrors.ainpcmod.TreeChopperNPCRenderer;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class ClientSetup {
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::clientSetup);
    }

    private static void clientSetup(final FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.TREE_CHOPPER_NPC.get(), TreeChopperNPCRenderer::new);
    }
}
