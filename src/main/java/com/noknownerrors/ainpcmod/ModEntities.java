package main.java.com.noknownerrors.ainpcmod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, "ainpcmod");

    public static final RegistryObject<EntityType<TreeChopperNPC>> TREE_CHOPPER_NPC = ENTITIES.register("tree_chopper_npc",
            () -> EntityType.Builder.of(TreeChopperNPC::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .build("tree_chopper_npc"));
}
