package main.java.com.noknownerrors.ainpcmod;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "ainpcmod");

    public static final RegistryObject<Item> NPC_WAND = ITEMS.register("npc_wand", 
        () -> new NPCWandItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
}