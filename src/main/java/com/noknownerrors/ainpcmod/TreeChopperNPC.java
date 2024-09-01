package main.java.com.noknownerrors.ainpcmod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.trading.MerchantOffer;



public class TreeChopperNPC extends AbstractVillager {
    private final SimpleContainer inventory = new SimpleContainer(16);
    private boolean hasPlacedCraftingTable = false;

    public TreeChopperNPC(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractVillager.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.5D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ChopTreeGoal(this));
        this.goalSelector.addGoal(2, new CraftPlanksGoal(this));
        this.goalSelector.addGoal(3, new PlaceCraftingTableGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public SimpleContainer getInventory() {
        return inventory;
    }

    public boolean hasPlacedCraftingTable() {
        return hasPlacedCraftingTable;
    }

    public void setHasPlacedCraftingTable(boolean value) {
        hasPlacedCraftingTable = value;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.isBaby()) {
            if (!this.level.isClientSide && hand == InteractionHand.MAIN_HAND) {
                player.sendMessage(new TextComponent("Tree Chopper NPC Inventory:"), player.getUUID());
                
                for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                    ItemStack stack = this.inventory.getItem(i);
                    if (!stack.isEmpty()) {
                        player.sendMessage(new TextComponent(stack.getCount() + "x " + stack.getItem().getDescription().getString()), player.getUUID());
                    }
                }
                
                if (this.inventory.isEmpty()) {
                    player.sendMessage(new TextComponent("The inventory is empty."), player.getUUID());
                }
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    private static class ChopTreeGoal extends Goal {
        private final TreeChopperNPC npc;
        private BlockPos targetTree;

        public ChopTreeGoal(TreeChopperNPC npc) {
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            if (npc.hasPlacedCraftingTable()) return false;
            targetTree = findNearestTree();
            return targetTree != null;
        }

        @Override
        public void tick() {
            if (targetTree != null) {
                npc.getNavigation().moveTo(targetTree.getX(), targetTree.getY(), targetTree.getZ(), 1.0D);
                if (npc.distanceToSqr(targetTree.getX(), targetTree.getY(), targetTree.getZ()) < 4) {
                    npc.level.destroyBlock(targetTree, false);
                    npc.getInventory().addItem(new ItemStack(Items.OAK_LOG));
                    targetTree = null;
                }
            }
        }

        private BlockPos findNearestTree() {
            BlockPos npcPos = npc.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(npcPos.offset(-10, -5, -10), npcPos.offset(10, 5, 10))) {
                if (npc.level.getBlockState(pos).getBlock() == Blocks.OAK_LOG) {
                    return pos;
                }
            }
            return null;
        }
    }

    private static class CraftPlanksGoal extends Goal {
        private final TreeChopperNPC npc;

        public CraftPlanksGoal(TreeChopperNPC npc) {
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return npc.getInventory().countItem(Items.OAK_LOG) >= 1 && npc.getInventory().countItem(Items.OAK_PLANKS) < 4;
        }

        @Override
        public void tick() {
            SimpleContainer inventory = npc.getInventory();
            if (inventory.countItem(Items.OAK_LOG) >= 1) {
                npc.removeItemFromInventory(inventory, Items.OAK_LOG, 1);
                inventory.addItem(new ItemStack(Items.OAK_PLANKS, 4));
            }
        }
    }

    private static class PlaceCraftingTableGoal extends Goal {
        private final TreeChopperNPC npc;
        private BlockPos targetPos;

        public PlaceCraftingTableGoal(TreeChopperNPC npc) {
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return !npc.hasPlacedCraftingTable() && npc.getInventory().countItem(Items.OAK_PLANKS) >= 4;
        }

        @Override
        public void tick() {
            if (targetPos == null) {
                targetPos = findSuitableGround();
            }
            if (targetPos != null) {
                npc.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0D);
                if (npc.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ()) < 4) {
                    npc.level.setBlock(targetPos.above(), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                    npc.removeItemFromInventory(npc.getInventory(), Items.OAK_PLANKS, 4);
                    npc.setHasPlacedCraftingTable(true);
                }
            }
        }

        private BlockPos findSuitableGround() {
            BlockPos npcPos = npc.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(npcPos.offset(-5, -1, -5), npc.blockPosition().offset(5, -1, 5))) {
                if (npc.level.getBlockState(pos).getBlock() == Blocks.GRASS_BLOCK) {
                    return pos;
                }
            }
            return null;
        }
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob mate) {
        return null;
    }

    private void removeItemFromInventory(SimpleContainer inventory, Item item, int count) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(count, stack.getCount());
                stack.shrink(toRemove);
                count -= toRemove;
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                if (count == 0) {
                    break;
                }
            }
        }
    }

    @Override
    protected void updateTrades() {
        // This NPC doesn't trade.
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // This NPC doesn't reward trade XP, so we can leave this method empty
    }


}
