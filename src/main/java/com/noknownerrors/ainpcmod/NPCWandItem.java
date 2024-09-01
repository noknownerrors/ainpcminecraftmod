package main.java.com.noknownerrors.ainpcmod;

import main.java.com.noknownerrors.ainpcmod.TreeChopperNPC;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class NPCWandItem extends Item {
    public NPCWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            ServerLevel level = (ServerLevel) context.getLevel();
            BlockPos pos = context.getClickedPos().above();
            
            TreeChopperNPC npc = ModEntities.TREE_CHOPPER_NPC.get().create(level);
            if (npc != null) {
                npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                level.addFreshEntity(npc);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
