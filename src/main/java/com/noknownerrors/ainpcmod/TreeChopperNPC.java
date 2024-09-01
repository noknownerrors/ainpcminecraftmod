package main.java.com.noknownerrors.ainpcmod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class TreeChopperNPC extends AbstractVillager {
    private final SimpleContainer inventory = new SimpleContainer(8);
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
        this.goalSelector.addGoal(2, new PlaceCraftingTableGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
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
    protected void updateTrades() {
        // This NPC doesn't trade, so we leave this method empty
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // This NPC doesn't trade, so we leave this method empty
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
                    npc.level.destroyBlock(targetTree, true);
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

    private static class PlaceCraftingTableGoal extends Goal {
        private final TreeChopperNPC npc;
        private BlockPos targetPos;

        public PlaceCraftingTableGoal(TreeChopperNPC npc) {
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return !npc.hasPlacedCraftingTable() && npc.getInventory().countItem(Items.OAK_LOG) >= 4;
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
                    removeOakLogs(4);
                    npc.setHasPlacedCraftingTable(true);
                }
            }
        }

        private void removeOakLogs(int count) {
            SimpleContainer inventory = npc.getInventory();
            for (int i = 0; i < inventory.getContainerSize() && count > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.getItem() == Items.OAK_LOG) {
                    int toRemove = Math.min(count, stack.getCount());
                    stack.shrink(toRemove);
                    count -= toRemove;
                    if (stack.isEmpty()) {
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
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
    public MerchantOffers getOffers() {
        return new MerchantOffers();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob mate) {
        // This NPC doesn't breed, so we return null
        return null;
    }
}