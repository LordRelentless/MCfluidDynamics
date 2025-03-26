package com.fluidphysics.minecraft;

import com.fluidphysics.core.FluidLevelData;
import com.fluidphysics.core.FluidRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Custom implementation of Minecraft's fluid blocks.
 * Overrides vanilla fluid behavior to integrate with the fluid simulation system.
 */
public class FluidPhysicsBlock extends Block {
    // The fluid registry
    private final FluidRegistry fluidRegistry;
    
    /**
     * Creates a new FluidPhysicsBlock.
     * 
     * @param properties The block properties
     * @param fluidRegistry The fluid registry
     */
    public FluidPhysicsBlock(Properties properties, FluidRegistry fluidRegistry) {
        super(properties);
        this.fluidRegistry = fluidRegistry;
    }
    
    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        
        // Register this block with the fluid registry
        fluidRegistry.registerFluidBlock(pos, state);
    }
    
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, world, pos, newState, isMoving);
        
        // Unregister this block from the fluid registry
        fluidRegistry.unregisterFluidBlock(pos);
    }
    
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        
        // Mark this block as active for processing
        fluidRegistry.markActive(pos);
        
        // Also mark the source of the change as active
        fluidRegistry.markActive(fromPos);
    }
    
    /**
     * Updates the block state based on fluid data.
     * 
     * @param world The Minecraft world
     * @param pos The position
     * @param fluidData The fluid data
     * @return The updated block state
     */
    public static BlockState updateBlockState(Level world, BlockPos pos, FluidLevelData fluidData) {
        // Get the current block state
        BlockState currentState = world.getBlockState(pos);
        
        // If the fluid data is empty, remove the fluid block
        if (fluidData.isEmpty()) {
            return Block.pushEntitiesUp(currentState, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), world, pos);
        }
        
        // Get the fluid type
        FlowingFluid fluid = (FlowingFluid) Fluids.WATER; // Default to water
        
        // If this is a source block, set a source block
        if (fluidData.isSource()) {
            return fluid.getSource().defaultBlockState();
        }
        
        // Otherwise, set a flowing block with the appropriate level
        int level = 8 - fluidData.getLevel(); // Convert to Minecraft's level system (8 = empty, 0 = full)
        return fluid.getFlowing(level, false);
    }
    
    /**
     * Creates fluid data from a block state.
     * 
     * @param state The block state
     * @return The fluid data
     */
    public static FluidLevelData createFluidData(BlockState state) {
        FluidState fluidState = state.getFluidState();
        
        // If this is not a fluid, return empty data
        if (fluidState.isEmpty()) {
            return new FluidLevelData();
        }
        
        // Create fluid data from the fluid state
        return FluidLevelData.fromFluidState(fluidState);
    }
}
