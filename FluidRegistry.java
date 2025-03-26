package com.fluidphysics.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Global registry for tracking active fluid blocks.
 * Optimizes updates by only processing active fluid areas.
 * Manages chunk loading/unloading for fluid data.
 */
public class FluidRegistry {
    // Singleton instance
    private static FluidRegistry instance;
    
    // Active fluid blocks that need processing
    private final Set<BlockPos> activeBlocks = new HashSet<>();
    
    // Fluid data storage
    private final Map<BlockPos, FluidLevelData> fluidDataMap = new HashMap<>();
    
    // Configuration
    private final FluidPhysicsConfig config;
    
    /**
     * Private constructor for singleton pattern.
     * 
     * @param config The fluid physics configuration
     */
    private FluidRegistry(FluidPhysicsConfig config) {
        this.config = config;
    }
    
    /**
     * Gets the singleton instance, creating it if necessary.
     * 
     * @param config The fluid physics configuration
     * @return The singleton instance
     */
    public static synchronized FluidRegistry getInstance(FluidPhysicsConfig config) {
        if (instance == null) {
            instance = new FluidRegistry(config);
        }
        return instance;
    }
    
    /**
     * Registers a fluid block for processing.
     * 
     * @param pos The position of the fluid block
     * @param state The block state
     */
    public void registerFluidBlock(BlockPos pos, BlockState state) {
        // Create fluid data from the block state
        FluidLevelData fluidData = FluidLevelData.fromFluidState(state.getFluidState());
        
        // Only register if it's not empty
        if (!fluidData.isEmpty()) {
            fluidDataMap.put(pos.immutable(), fluidData);
            activeBlocks.add(pos.immutable());
        }
    }
    
    /**
     * Unregisters a fluid block.
     * 
     * @param pos The position of the fluid block
     */
    public void unregisterFluidBlock(BlockPos pos) {
        fluidDataMap.remove(pos.immutable());
        activeBlocks.remove(pos.immutable());
    }
    
    /**
     * Gets the fluid data for a block.
     * 
     * @param pos The position of the fluid block
     * @return The fluid data, or null if not registered
     */
    public FluidLevelData getFluidData(BlockPos pos) {
        return fluidDataMap.get(pos.immutable());
    }
    
    /**
     * Sets the fluid data for a block.
     * 
     * @param pos The position of the fluid block
     * @param fluidData The fluid data
     */
    public void setFluidData(BlockPos pos, FluidLevelData fluidData) {
        if (fluidData.isEmpty()) {
            unregisterFluidBlock(pos);
        } else {
            fluidDataMap.put(pos.immutable(), fluidData);
            activeBlocks.add(pos.immutable());
        }
    }
    
    /**
     * Gets all active fluid blocks.
     * 
     * @return A set of active block positions
     */
    public Set<BlockPos> getActiveBlocks() {
        return new HashSet<>(activeBlocks);
    }
    
    /**
     * Marks a block as active for processing.
     * 
     * @param pos The position to mark
     */
    public void markActive(BlockPos pos) {
        activeBlocks.add(pos.immutable());
    }
    
    /**
     * Marks a block as inactive.
     * 
     * @param pos The position to mark
     */
    public void markInactive(BlockPos pos) {
        activeBlocks.remove(pos.immutable());
    }
    
    /**
     * Checks if a block is active.
     * 
     * @param pos The position to check
     * @return True if the block is active
     */
    public boolean isActive(BlockPos pos) {
        return activeBlocks.contains(pos.immutable());
    }
    
    /**
     * Handles chunk loading.
     * 
     * @param world The Minecraft world
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    public void onChunkLoad(Level world, int chunkX, int chunkZ) {
        // Scan the chunk for fluid blocks
        scanChunkForFluids(world, chunkX, chunkZ);
    }
    
    /**
     * Handles chunk unloading.
     * 
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    public void onChunkUnload(int chunkX, int chunkZ) {
        // Remove all fluid data in this chunk
        removeFluidDataInChunk(chunkX, chunkZ);
    }
    
    /**
     * Scans a chunk for fluid blocks.
     * 
     * @param world The Minecraft world
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    private void scanChunkForFluids(Level world, int chunkX, int chunkZ) {
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        
        // Scan all blocks in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinBuildHeight(); y < world.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = world.getBlockState(pos);
                    
                    // If this is a fluid block, register it
                    if (!state.getFluidState().isEmpty()) {
                        registerFluidBlock(pos, state);
                    }
                }
            }
        }
    }
    
    /**
     * Removes all fluid data in a chunk.
     * 
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    private void removeFluidDataInChunk(int chunkX, int chunkZ) {
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        int endX = startX + 15;
        int endZ = startZ + 15;
        
        // Create a list of positions to remove
        Set<BlockPos> toRemove = new HashSet<>();
        
        // Find all positions in this chunk
        for (BlockPos pos : fluidDataMap.keySet()) {
            if (pos.getX() >= startX && pos.getX() <= endX && 
                pos.getZ() >= startZ && pos.getZ() <= endZ) {
                toRemove.add(pos);
            }
        }
        
        // Remove all positions
        for (BlockPos pos : toRemove) {
            fluidDataMap.remove(pos);
            activeBlocks.remove(pos);
        }
    }
    
    /**
     * Clears all fluid data.
     */
    public void clear() {
        fluidDataMap.clear();
        activeBlocks.clear();
    }
}
