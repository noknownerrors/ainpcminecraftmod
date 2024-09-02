package main.java.com.noknownerrors.ainpcmod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;



public class TreeChopperNPC extends AbstractVillager {
    private final SimpleContainer inventory = new SimpleContainer(16);
    private boolean hasPlacedCraftingTable = false;
    private static boolean isChoppingTree = false;

    public TreeChopperNPC(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractVillager.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CreateCraftingTableGoal(this));
        this.goalSelector.addGoal(2, new ChopTreeGoal(this));
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
        private final int SEARCH_RADIUS = 16;
        private final int CHOP_RADIUS = 4;
        private final int MAX_STUCK_TICKS = 20;
        private int stuckTicks = 0;
        private int breakProgress = 0;

        public ChopTreeGoal(TreeChopperNPC npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (npc.hasPlacedCraftingTable()) return false;
            targetTree = findNearestLog();
            return targetTree != null;
        }

        @Override
        public boolean canContinueToUse() {
            return targetTree != null;
        }

        @Override
        public void start() {
            if (targetTree != null) {
                npc.getNavigation().moveTo(targetTree.getX(), targetTree.getY(), targetTree.getZ(), 1.0D);
                System.out.println("Moving towards tree at " + targetTree);
                stuckTicks = 0;
            }
        }

        @Override
        public void tick() {
            if (targetTree != null) {
                if (npc.distanceToSqr(targetTree.getX(), targetTree.getY(), targetTree.getZ()) <= CHOP_RADIUS * CHOP_RADIUS) {
                    chopTree(targetTree);
                } else if (!npc.getNavigation().isInProgress()) {
                    if (stuckTicks >= MAX_STUCK_TICKS) {
                        System.out.println("Got stuck while navigating to the tree. Chopping the tree from current position.");
                        chopTree(targetTree);
                    } else {
                        stuckTicks++;
                    }
                } else {
                    stuckTicks = 0;
                }
            }
        }

        private BlockPos findNearestLog() {
            BlockPos npcPos = npc.blockPosition();
            BlockPos nearestLog = null;
            double nearestDistanceSq = Double.MAX_VALUE;

            for (BlockPos pos : BlockPos.betweenClosed(npcPos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
                    npcPos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
                BlockState state = npc.level.getBlockState(pos);
                Block block = state.getBlock();

                if (isTreeLog(block)) {
                    double distanceSq = pos.distSqr(npcPos);
                    if (distanceSq < nearestDistanceSq) {
                        nearestLog = pos.immutable();
                        nearestDistanceSq = distanceSq;
                    }
                }
            }

            if (nearestLog != null) {
                System.out.println("Found nearest log at " + nearestLog);
            } else {
                System.out.println("No logs found within search radius.");
            }

            return nearestLog;
        }

        private void chopTree(BlockPos pos) {
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> queue = new LinkedList<>();
            queue.add(pos);
    
            while (!queue.isEmpty()) {
                isChoppingTree = true;
                BlockPos current = queue.poll();
                if (visited.contains(current) || !isWithinChopRadius(current)) {
                    continue;
                }
    
                visited.add(current);
                BlockState state = npc.level.getBlockState(current);
                Block block = state.getBlock();
    
                if (isTreeBlock(block)) {
                    if (isTreeLog(block)) {
                        equipBestAxe();
                    }
                    breakBlock(current, block);
                    if (isTreeLog(block)) {
                        npc.getInventory().addItem(new ItemStack(block));
                        System.out.println("Chopped log at " + current + ". Current log count: " + npc.getInventory().countItem(Items.OAK_LOG));
                    }
                }
    
                for (BlockPos neighbor : getNeighbors(current)) {
                    if (hasSpaceToMove(neighbor)) {
                        npc.getNavigation().moveTo(neighbor.getX(), neighbor.getY(), neighbor.getZ(), 1.0D);
                    }
                    if (!visited.contains(neighbor) && isTreeBlock(npc.level.getBlockState(neighbor).getBlock())) {
                        queue.add(neighbor);
                    }
                }
            }
            isChoppingTree = false;
            targetTree = findNearestLog();
        }

        private void breakBlock(BlockPos pos, Block block) {
            float hardness = block.defaultBlockState().getDestroySpeed(npc.level, pos);
            int ticksToBreak;
            if (isTreeLog(block)) {
                ticksToBreak = (int) (1.5f / hardness);
            } else {
                ticksToBreak = (int) (0.5f / hardness);
            }
    
            if (breakProgress < ticksToBreak) {
                npc.swing(InteractionHand.MAIN_HAND);
                breakProgress++;
            } else {
                npc.level.destroyBlock(pos, false);
                breakProgress = 0;
            }
        }

        private void equipBestAxe() {
            float bestAxeSpeed = -1.0f;
            int bestAxeSlot = -1;
        
            for (int i = 0; i < npc.getInventory().getContainerSize(); i++) {
                ItemStack item = npc.getInventory().getItem(i);
                if (item.getItem() instanceof AxeItem) {
                    float speed = ((AxeItem) item.getItem()).getTier().getSpeed();
                    if (speed > bestAxeSpeed) {
                        bestAxeSpeed = speed;
                        bestAxeSlot = i;
                    }
                }
            }
        
            if (bestAxeSlot != -1) {
                npc.setItemInHand(InteractionHand.MAIN_HAND, npc.getInventory().getItem(bestAxeSlot));
            } else {
                npc.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }

        private boolean isWithinChopRadius(BlockPos pos) {
            return pos.distSqr(targetTree) <= CHOP_RADIUS * CHOP_RADIUS;
        }

        private boolean hasSpaceToMove(BlockPos pos) {
            BlockState state = npc.level.getBlockState(pos);
            return state.isAir() || !state.getMaterial().blocksMotion();
        }

        private Iterable<BlockPos> getNeighbors(BlockPos pos) {
            return BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1));
        }

        private boolean isTreeLog(Block block) {
            return block.defaultBlockState().is(BlockTags.LOGS);
        }

        private boolean isTreeBlock(Block block) {
            return isTreeLog(block) || block.defaultBlockState().is(BlockTags.LEAVES);
        }
    }
    

    private static class CreateCraftingTableGoal extends Goal {
        private final TreeChopperNPC npc;
        private BlockPos targetPos;
        private final int SEARCH_RADIUS = 16;
    
        public CreateCraftingTableGoal(TreeChopperNPC npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }
    
        @Override
        public boolean canUse() {
            if (isChoppingTree) {
                return false;
            }
    
            if (npc.hasPlacedCraftingTable()) {
                return false;
            }
    
            if (isCraftingTableNearby()) {
                return false;
            }
    
            return npc.getInventory().countItem(Items.OAK_LOG) >= 1;
        }
    
        @Override
        public void tick() {
            if (targetPos == null) {
                targetPos = findSuitableGround();
                if (targetPos != null) {
                    System.out.println("Found suitable ground for crafting table at " + targetPos);
                }
            }
    
            if (targetPos != null) {
                npc.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.0D);
                if (npc.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5) < 2.25) {
                    SimpleContainer inventory = npc.getInventory();
                    if (inventory.countItem(Items.OAK_PLANKS) < 4 && inventory.countItem(Items.OAK_LOG) >= 1) {
                        npc.removeItemFromInventory(inventory, Items.OAK_LOG, 1);
                        inventory.addItem(new ItemStack(Items.OAK_PLANKS, 4));
                        System.out.println("Crafted planks. New plank count: " + inventory.countItem(Items.OAK_PLANKS));
                    }
    
                    if (inventory.countItem(Items.OAK_PLANKS) >= 4) {
                        npc.level.setBlock(targetPos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                        npc.removeItemFromInventory(inventory, Items.OAK_PLANKS, 4);
                        npc.setHasPlacedCraftingTable(true);
                        System.out.println("Placed crafting table at " + targetPos);
                        targetPos = null;
                    }
                }
            }
        }
    
        private boolean isCraftingTableNearby() {
            BlockPos npcPos = npc.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(npcPos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
                    npcPos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
                if (npc.level.getBlockState(pos).is(Blocks.CRAFTING_TABLE)) {
                    return true;
                }
            }
            return false;
        }
    
        private BlockPos findSuitableGround() {
            BlockPos npcPos = npc.blockPosition();
            BlockPos[] adjacentPositions = {
                npcPos.north(), npcPos.south(), npcPos.east(), npcPos.west()
            };
    
            for (BlockPos pos : adjacentPositions) {
                if (npc.level.getBlockState(pos).isAir() && npc.level.getBlockState(pos.below()).getMaterial().isSolid()) {
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
