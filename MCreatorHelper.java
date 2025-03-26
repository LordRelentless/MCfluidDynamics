package com.fluidphysics.mcreator;

import com.fluidphysics.core.*;

/**
 * MCreator integration helper class.
 * Provides simplified interfaces for MCreator to interact with the fluid physics system.
 */
public class MCreatorHelper {
    // Singleton instance of the fluid simulator
    private static FluidSimulator fluidSimulator;
    
    // Singleton instance of the fluid registry
    private static FluidRegistry fluidRegistry;
    
    // Configuration
    private static FluidPhysicsConfig config;
    
    /**
     * Initializes the fluid physics system with the specified configuration.
     * Call this method from your MCreator mod's initialization procedure.
     * 
     * @param simulationPrecision Simulation precision (1-4)
     * @param flowRate Flow rate multiplier
     * @param enablePressure Whether to enable pressure-based flow
     * @param enableMomentum Whether to enable momentum-based flow
     * @param enableFiniteFluids Whether to enable finite fluids
     * @param updateFrequency How often the simulation runs (in ticks)
     * @param activeRange Maximum distance for active fluid simulation
     */
    public static void initialize(int simulationPrecision, float flowRate, 
                                 boolean enablePressure, boolean enableMomentum, 
                                 boolean enableFiniteFluids, int updateFrequency, 
                                 int activeRange) {
        // Create configuration
        config = new FluidPhysicsConfig(
            simulationPrecision,
            flowRate,
            enablePressure,
            enableMomentum,
            enableFiniteFluids,
            updateFrequency,
            activeRange
        );
        
        // Initialize registry and simulator
        fluidRegistry = FluidRegistry.getInstance(config);
        fluidSimulator = new FluidSimulator(config);
        
        System.out.println("[FluidPhysics] Initialized fluid physics system");
    }
    
    /**
     * Processes a world tick for the fluid simulation.
     * Call this method from your MCreator mod's world tick procedure.
     * 
     * @param world The Minecraft world
     */
    public static void processTick(Object world) {
        // Skip if not initialized
        if (fluidSimulator == null || fluidRegistry == null) {
            return;
        }
        
        // Process the tick
        fluidSimulator.processTick((net.minecraft.world.level.Level) world);
    }
    
    /**
     * Registers a fluid block for processing.
     * Call this method from your MCreator mod's block place procedure.
     * 
     * @param world The Minecraft world
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     */
    public static void registerFluidBlock(Object world, int x, int y, int z) {
        // Skip if not initialized
        if (fluidRegistry == null) {
            return;
        }
        
        // Create block position
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        
        // Get block state
        net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) world;
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        
        // Register the block
        fluidRegistry.registerFluidBlock(pos, state);
    }
    
    /**
     * Unregisters a fluid block.
     * Call this method from your MCreator mod's block break procedure.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     */
    public static void unregisterFluidBlock(int x, int y, int z) {
        // Skip if not initialized
        if (fluidRegistry == null) {
            return;
        }
        
        // Create block position
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        
        // Unregister the block
        fluidRegistry.unregisterFluidBlock(pos);
    }
    
    /**
     * Gets the fluid level at the specified position.
     * Use this method in your MCreator mod's rendering procedures.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return The fluid level (0-8), or 0 if no fluid
     */
    public static int getFluidLevel(int x, int y, int z) {
        // Skip if not initialized
        if (fluidRegistry == null) {
            return 0;
        }
        
        // Create block position
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        
        // Get fluid data
        FluidLevelData fluidData = fluidRegistry.getFluidData(pos);
        
        // Return level
        return fluidData != null ? fluidData.getLevel() : 0;
    }
    
    /**
     * Checks if a block is a fluid source.
     * Use this method in your MCreator mod's block behavior procedures.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return True if the block is a fluid source
     */
    public static boolean isFluidSource(int x, int y, int z) {
        // Skip if not initialized
        if (fluidRegistry == null) {
            return false;
        }
        
        // Create block position
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        
        // Get fluid data
        FluidLevelData fluidData = fluidRegistry.getFluidData(pos);
        
        // Return source status
        return fluidData != null && fluidData.isSource();
    }
    
    /**
     * Gets the flow direction at the specified position.
     * Use this method in your MCreator mod's rendering procedures.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return The flow direction as a string (NORTH, EAST, SOUTH, WEST, UP, DOWN, NONE)
     */
    public static String getFlowDirection(int x, int y, int z) {
        // Skip if not initialized
        if (fluidRegistry == null) {
            return "NONE";
        }
        
        // Create block position
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        
        // Get fluid data
        FluidLevelData fluidData = fluidRegistry.getFluidData(pos);
        
        // Return direction
        return fluidData != null ? fluidData.getMomentum().name() : "NONE";
    }
    
    /**
     * Gets the fluid simulator instance.
     * 
     * @return The fluid simulator
     */
    public static FluidSimulator getFluidSimulator() {
        return fluidSimulator;
    }
    
    /**
     * Gets the fluid registry instance.
     * 
     * @return The fluid registry
     */
    public static FluidRegistry getFluidRegistry() {
        return fluidRegistry;
    }
    
    /**
     * Gets the configuration instance.
     * 
     * @return The configuration
     */
    public static FluidPhysicsConfig getConfig() {
        return config;
    }
}
