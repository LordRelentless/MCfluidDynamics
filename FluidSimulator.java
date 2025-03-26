package com.fluidphysics.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Core fluid simulation logic that processes fluid movement based on physics rules.
 * This class is independent of Minecraft implementation details.
 */
public class FluidSimulator {
    // Configuration
    private final FluidPhysicsConfig config;
    
    // Active fluid blocks that need processing
    private final Set<BlockPos> activeBlocks = new HashSet<>();
    
    // Fluid data cache to avoid repeated lookups
    private final Map<BlockPos, FluidLevelData> fluidDataCache = new HashMap<>();
    
    // Flow calculator for mathematical calculations
    private final FlowCalculator flowCalculator;
    
    /**
     * Creates a new FluidSimulator with the specified configuration.
     * 
     * @param config The fluid physics configuration
     */
    public FluidSimulator(FluidPhysicsConfig config) {
        this.config = config;
        this.flowCalculator = new FlowCalculator(config);
    }
    
    /**
     * Processes a single simulation tick for the entire world.
     * 
     * @param world The Minecraft world
     */
    public void processTick(Level world) {
        // Create a copy of active blocks to avoid concurrent modification
        Set<BlockPos> currentActiveBlocks = new HashSet<>(activeBlocks);
        
        // Process each active block
        for (BlockPos pos : currentActiveBlocks) {
            processFluidBlock(world, pos);
        }
        
        // Apply changes to the world
        applyChanges(world);
        
        // Clear the cache for the next tick
        fluidDataCache.clear();
    }
    
    /**
     * Processes a single fluid block, calculating flows to adjacent blocks.
     * 
     * @param world The Minecraft world
     * @param pos The position of the fluid block
     */
    private void processFluidBlock(Level world, BlockPos pos) {
        FluidLevelData fluidData = getFluidData(world, pos);
        
        // Skip if this block is empty
        if (fluidData.isEmpty()) {
            activeBlocks.remove(pos);
            return;
        }
        
        // Process gravity flow (down)
        processGravityFlow(world, pos, fluidData);
        
        // Process horizontal flows based on pressure
        if (config.enablePressure) {
            processHorizontalFlows(world, pos, fluidData);
        }
        
        // Update momentum
        if (config.enableMomentum) {
            processMomentum(world, pos, fluidData);
        }
    }
    
    /**
     * Processes gravity-based flow (downward).
     * 
     * @param world The Minecraft world
     * @param pos The position of the fluid block
     * @param fluidData The fluid data for this block
     */
    private void processGravityFlow(Level world, BlockPos pos, FluidLevelData fluidData) {
        BlockPos downPos = pos.below();
        FluidLevelData downFluidData = getFluidData(world, downPos);
        
        // Calculate flow amount based on gravity
        int flowAmount = flowCalculator.calculateGravityFlow(fluidData, downFluidData);
        
        if (flowAmount > 0) {
            // Apply the flow
            applyFlow(pos, downPos, flowAmount, fluidData.getFluidType());
            
            // Set momentum to downward
            fluidData.setMomentum(FlowDirection.DOWN);
            
            // Mark blocks as active
            activeBlocks.add(pos);
            activeBlocks.add(downPos);
        }
    }
    
    /**
     * Processes horizontal flows based on pressure differences.
     * 
     * @param world The Minecraft world
     * @param pos The position of the fluid block
     * @param fluidData The fluid data for this block
     */
    private void processHorizontalFlows(Level world, BlockPos pos, FluidLevelData fluidData) {
        // Skip if this block is empty or has too little fluid
        if (fluidData.getLevel() <= 1) {
            return;
        }
        
        // Check all four horizontal directions
        processDirectionalFlow(world, pos, fluidData, BlockPos.NORTH, FlowDirection.NORTH);
        processDirectionalFlow(world, pos, fluidData, BlockPos.EAST, FlowDirection.EAST);
        processDirectionalFlow(world, pos, fluidData, BlockPos.SOUTH, FlowDirection.SOUTH);
        processDirectionalFlow(world, pos, fluidData, BlockPos.WEST, FlowDirection.WEST);
    }
    
    /**
     * Processes flow in a specific direction.
     * 
     * @param world The Minecraft world
     * @param pos The position of the fluid block
     * @param fluidData The fluid data for this block
     * @param offset The position offset
     * @param direction The flow direction
     */
    private void processDirectionalFlow(Level world, BlockPos pos, FluidLevelData fluidData, 
                                       BlockPos offset, FlowDirection direction) {
        BlockPos targetPos = pos.offset(offset.getX(), offset.getY(), offset.getZ());
        FluidLevelData targetFluidData = getFluidData(world, targetPos);
        
        // Calculate flow amount based on pressure difference
        int flowAmount = flowCalculator.calculatePressureFlow(fluidData, targetFluidData, 
                                                             fluidData.getMomentum() == direction);
        
        if (flowAmount > 0) {
            // Apply the flow
            applyFlow(pos, targetPos, flowAmount, fluidData.getFluidType());
            
            // Set momentum
            fluidData.setMomentum(direction);
            
            // Mark blocks as active
            activeBlocks.add(pos);
            activeBlocks.add(targetPos);
        }
    }
    
    /**
     * Processes momentum-based flow continuation.
     * 
     * @param world The Minecraft world
     * @param pos The position of the fluid block
     * @param fluidData The fluid data for this block
     */
    private void processMomentum(Level world, BlockPos pos, FluidLevelData fluidData) {
        FlowDirection momentum = fluidData.getMomentum();
        
        // Skip if there's no momentum or too little fluid
        if (momentum == FlowDirection.NONE || fluidData.getLevel() <= 1) {
            return;
        }
        
        // Get the target position based on momentum direction
        BlockPos targetPos = pos.offset(
            momentum.getXOffset(),
            momentum.getYOffset(),
            momentum.getZOffset()
        );
        
        FluidLevelData targetFluidData = getFluidData(world, targetPos);
        
        // Calculate flow amount based on momentum
        int flowAmount = flowCalculator.calculateMomentumFlow(fluidData, targetFluidData);
        
        if (flowAmount > 0) {
            // Apply the flow
            applyFlow(pos, targetPos, flowAmount, fluidData.getFluidType());
            
            // Mark blocks as active
            activeBlocks.add(pos);
            activeBlocks.add(targetPos);
        }
    }
    
    /**
     * Applies a flow between two blocks.
     * 
     * @param sourcePos The source position
     * @param targetPos The target position
     * @param amount The amount to flow
     * @param fluidType The fluid type
     */
    private void applyFlow(BlockPos sourcePos, BlockPos targetPos, int amount, FluidType fluidType) {
        FluidLevelData sourceData = fluidDataCache.get(sourcePos);
        FluidLevelData targetData = fluidDataCache.get(targetPos);
        
        // Remove fluid from source
        int actualAmount = sourceData.removeFluid(amount);
        
        // Add fluid to target
        int overflow = targetData.addFluid(actualAmount, fluidType);
        
        // If there's overflow, add it back to the source
        if (overflow > 0) {
            sourceData.addFluid(overflow, fluidType);
        }
    }
    
    /**
     * Applies all cached changes to the Minecraft world.
     * 
     * @param world The Minecraft world
     */
    private void applyChanges(Level world) {
        for (Map.Entry<BlockPos, FluidLevelData> entry : fluidDataCache.entrySet()) {
            BlockPos pos = entry.getKey();
            FluidLevelData fluidData = entry.getValue();
            
            // Update the block state in the world based on fluid data
            updateBlockState(world, pos, fluidData);
        }
    }
    
    /**
     * Updates the block state in the world based on fluid data.
     * 
     * @param world The Minecraft world
     * @param pos The position
     * @param fluidData The fluid data
     */
    private void updateBlockState(Level world, BlockPos pos, FluidLevelData fluidData) {
        // This is a placeholder - actual implementation would convert FluidLevelData
        // to the appropriate Minecraft BlockState/FluidState
        
        // If the block is empty, remove fluid
        if (fluidData.isEmpty()) {
            // Remove fluid block
            // world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }
        
        // If the block is a source, set a source block
        if (fluidData.isSource()) {
            // Set source block
            // world.setBlockAndUpdate(pos, fluidData.getFluidType().getSource().defaultBlockState());
            return;
        }
        
        // Otherwise, set a flowing block with the appropriate level
        // BlockState state = fluidData.getFluidType().getFlowing().defaultBlockState()
        //     .setValue(FlowingFluidBlock.LEVEL, 8 - fluidData.getLevel());
        // world.setBlockAndUpdate(pos, state);
    }
    
    /**
     * Gets the fluid data for a block, using the cache if available.
     * 
     * @param world The Minecraft world
     * @param pos The position
     * @return The fluid data
     */
    private FluidLevelData getFluidData(Level world, BlockPos pos) {
        // Check cache first
        if (fluidDataCache.containsKey(pos)) {
            return fluidDataCache.get(pos);
        }
        
        // Get the fluid state from the world
        BlockState blockState = world.getBlockState(pos);
        FluidState fluidState = blockState.getFluidState();
        
        // Create fluid data from the fluid state
        FluidLevelData fluidData = FluidLevelData.fromFluidState(fluidState);
        
        // Cache the result
        fluidDataCache.put(pos, fluidData);
        
        return fluidData;
    }
    
    /**
     * Registers a block as active for processing.
     * 
     * @param pos The position to register
     */
    public void registerActiveBlock(BlockPos pos) {
        activeBlocks.add(pos);
    }
    
    /**
     * Unregisters a block from active processing.
     * 
     * @param pos The position to unregister
     */
    public void unregisterActiveBlock(BlockPos pos) {
        activeBlocks.remove(pos);
    }
}
