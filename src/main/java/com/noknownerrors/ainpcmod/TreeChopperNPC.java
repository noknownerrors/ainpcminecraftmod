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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.server.level.ServerLevel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.List;
import java.util.UUID;

import org.lwjgl.system.CallbackI.F;

import com.mojang.authlib.GameProfile;

import java.util.HashSet;



public class TreeChopperNPC extends AbstractVillager {
    private final SimpleContainer inventory = new SimpleContainer(32);
    private boolean hasPlacedCraftingTable = false;
    protected boolean isChoppingTree = false;
    protected UUID uuid = null;
    GameProfile gameProfile = null;
    private int breakProgress = 0;

    public TreeChopperNPC(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.uuid = UUID.randomUUID();
        this.gameProfile = new GameProfile(uuid, "TreeChopperNPC");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractVillager.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level.isClientSide && this.isAlive()) {
            this.pickUpNearbyItems();
        }
    }

    private void pickUpNearbyItems() {
        AABB boundingBox = this.getBoundingBox().inflate(1.0D, 0.0D, 1.0D);
        List<ItemEntity> items = this.level.getEntitiesOfClass(ItemEntity.class, boundingBox);
        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isRemoved() && !itemEntity.getItem().isEmpty()) {
                this.takeItem(itemEntity);
            }
        }
    }

    private void takeItem(ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getItem();
        ItemStack remainingStack = this.inventory.addItem(itemStack);
        if (remainingStack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(remainingStack);
        }
    }

    protected boolean breakBlockTick(BlockPos pos, int ticksToBreak) {
        if (this.breakProgress < ticksToBreak) {
            this.swing(InteractionHand.MAIN_HAND);
            breakProgress++;
            return false;
        } else {
            ServerLevel serverLevel = (ServerLevel) this.level;
            BlockState blockState = serverLevel.getBlockState(pos);
            Player fakePlayer = FakePlayerFactory.get(serverLevel, this.gameProfile);
            blockState.getBlock().playerDestroy(serverLevel, fakePlayer, pos, blockState, null, ItemStack.EMPTY);
            this.level.destroyBlock(pos, false);
            this.breakProgress = 0;
            return true;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CreateCraftingTableGoal(this));
        this.goalSelector.addGoal(2, new ChopTreeGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new WalkTowardsItemGoal(this, 0.6D, 8.0F));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(50, new WaterAvoidingRandomStrollGoal(this, 0.6D));
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

    public int calculateBreakTime(BlockPos pos, ItemStack tool) {
        ServerLevel serverLevel = (ServerLevel) this.level;
        BlockState blockState = serverLevel.getBlockState(pos);
    
        // Create a fake player
        FakePlayer fakePlayer = FakePlayerFactory.get(serverLevel, this.gameProfile);
    
        // Set the tool in the fake player's hand
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);
    
        // Calculate the destroy speed
        float destroySpeed = blockState.getDestroySpeed(serverLevel, pos);
        float digSpeed = fakePlayer.getDigSpeed(blockState, pos);
    
        // Calculate ticks to break
        int ticksToBreak = (int) Math.ceil(destroySpeed / digSpeed);
    
        System.out.println("Ticks to break: " + ticksToBreak);
        return ticksToBreak;
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

    public class WalkTowardsItemGoal extends Goal {
        private final AbstractVillager npc;
        private final double speed;
        private final float maxDistance;
        private ItemEntity targetItem;
    
        public WalkTowardsItemGoal(AbstractVillager npc, double speed, float maxDistance) {
            this.npc = npc;
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }
    
        @Override
        public boolean canUse() {
            List<ItemEntity> nearbyItems = npc.level.getEntitiesOfClass(ItemEntity.class, npc.getBoundingBox().inflate(maxDistance));
            if (!nearbyItems.isEmpty()) {
                targetItem = nearbyItems.get(0);
                return true;
            }
            return false;
        }
    
        @Override
        public boolean canContinueToUse() {
            return targetItem != null && !targetItem.isRemoved() && npc.distanceToSqr(targetItem) > 1.0;
        }
    
        @Override
        public void start() {
            npc.getNavigation().moveTo(targetItem, speed);
        }
    
        @Override
        public void stop() {
            targetItem = null;
            npc.getNavigation().stop();
        }
    
        @Override
        public void tick() {
            if (targetItem != null && !targetItem.isRemoved()) {
                npc.getNavigation().moveTo(targetItem, speed);
            }
        }
    }

    private static class ChopTreeGoal extends Goal {
        private final TreeChopperNPC npc;
        private BlockPos targetedTreeBlock;
        private final int SEARCH_RADIUS = 16;
        private final int CHOP_RADIUS = 15;
        private final int MAX_CHOP_COUNT = 16;
        private final int BREAK_DELAY = 20; // delay between chopping trees
        private int chopCount = 0;
        private Set<BlockPos> visited = new HashSet<>();
        private Queue<BlockPos> queue = new LinkedList<>();
        private int delay = 0;
    
        public ChopTreeGoal(TreeChopperNPC npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }
    
        @Override
        public boolean canUse() {
            if (npc.hasPlacedCraftingTable()) return false;
            if (npc.isChoppingTree) return false;
            if (delay > 0) {
                delay--;
                return false;
            }
            targetedTreeBlock = findNearestLog();
            return targetedTreeBlock != null;
        }
    
        @Override
        public boolean canContinueToUse() {
            if (npc.getNavigation().isInProgress() || npc.isChoppingTree) {
                return true;
            } else {
                System.out.println("Chop tree goal is no longer valid.");
                return false;
            }
        }
    
        @Override
        public void start() {
            if (targetedTreeBlock == null) {
                targetedTreeBlock = findNearestLog();
            }
            npc.getNavigation().moveTo(targetedTreeBlock.getX(), targetedTreeBlock.getY(), targetedTreeBlock.getZ(), 1.0D);
            System.out.println("Moving towards tree at " + targetedTreeBlock);
        }
    
        @Override
        public void tick() {
            if (targetedTreeBlock == null) {
                return;
            }
            if (npc.getNavigation().isInProgress()) {
                System.out.println("navigation in progress to " + targetedTreeBlock);
                return;
            }
            if (npc.distanceToSqr(targetedTreeBlock.getX(), targetedTreeBlock.getY(), targetedTreeBlock.getZ()) <= CHOP_RADIUS * CHOP_RADIUS) {
                chopTree();
            } else {
                // add code to clear obstacles in the path
                System.out.println("Logs out of chop radius, ending current goal " + targetedTreeBlock);
                npc.getNavigation().moveTo(targetedTreeBlock.getX(), targetedTreeBlock.getY(), targetedTreeBlock.getZ(), 1.0D);
                npc.isChoppingTree = false;
                chopCount = 0;
                visited.clear();
                queue.clear();
                delay = BREAK_DELAY;
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
    
                if (isTreeLog(block) && npc.level.getBlockState(pos.below()).getMaterial().isSolid()) {
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
    
        private void chopTree() {
            npc.isChoppingTree = true;
            equipBestAxe();
            int ticksToBreak = npc.calculateBreakTime(targetedTreeBlock, npc.getItemInHand(InteractionHand.MAIN_HAND));
            if (npc.breakBlockTick(targetedTreeBlock, ticksToBreak)) {
                chopCount++;
                System.out.println("Chopped log at " + targetedTreeBlock);
            } else {
                return;
            }
            for (BlockPos neighbor : getNeighbors(targetedTreeBlock)) {
                if (!visited.contains(neighbor) && isTreeLog(npc.level.getBlockState(neighbor).getBlock())) {
                    queue.add(neighbor.immutable());
                    visited.add(neighbor.immutable());
                }
            }
            targetedTreeBlock = queue.poll();
            if (targetedTreeBlock != null) {
                npc.getNavigation().moveTo(targetedTreeBlock.getX(), targetedTreeBlock.getY(), targetedTreeBlock.getZ(), 1.0D);
            }
            if (targetedTreeBlock == null || chopCount >= MAX_CHOP_COUNT) {
                System.out.println("Finished chopping tree.");
                npc.isChoppingTree = false;
                chopCount = 0;
                visited.clear();
                queue.clear();
                delay = BREAK_DELAY;
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
            return pos.distSqr(targetedTreeBlock) <= CHOP_RADIUS * CHOP_RADIUS;
        }
    
        private Iterable<BlockPos> getNeighbors(BlockPos pos) {
            return BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1));
        }
    
        private boolean isTreeLog(Block block) {
            return block.defaultBlockState().is(BlockTags.LOGS);
        }
    
        private boolean isTreeLeaves(Block block) {
            return block.defaultBlockState().is(BlockTags.LEAVES);
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
            if (npc.isChoppingTree) {
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
