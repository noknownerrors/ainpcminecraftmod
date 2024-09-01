package main.java.com.noknownerrors.ainpcmod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import main.java.com.noknownerrors.ainpcmod.ModItems;
import main.java.com.noknownerrors.ainpcmod.ClientSetup;
import main.java.com.noknownerrors.ainpcmod.ModEventSubscriber;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.eventbus.api.IEventBus;

@Mod("ainpcmod")
public class TreeChopperMod {
    public TreeChopperMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        modEventBus.register(ModEventSubscriber.class);
        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.init();
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        
    }
}


