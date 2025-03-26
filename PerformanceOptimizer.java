package com.fluidphysics.minecraft;

import com.fluidphysics.core.FluidRegistry;
import com.fluidphysics.core.FluidSimulator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Performance monitoring and optimization for the fluid physics system.
 * Tracks performance metrics and adjusts simulation parameters accordingly.
 */
@Mod.EventBusSubscriber
public class PerformanceOptimizer {
    // Performance metrics
    private static long lastProcessTime = 0;
    private static int activeBlockCount = 0;
    private static int processedBlockCount = 0;
    private static int skippedBlockCount = 0;
    
    // Moving average of processing time (in ms)
    private static final int SAMPLE_SIZE = 20;
    private static final long[] processTimes = new long[SAMPLE_SIZE];
    private static int sampleIndex = 0;
    
    // Performance thresholds
    private static final long HIGH_LOAD_THRESHOLD = 50; // ms
    private static final long LOW_LOAD_THRESHOLD = 10; // ms
    
    /**
     * Starts performance monitoring for a simulation tick.
     */
    public static void startMonitoring() {
        lastProcessTime = System.currentTimeMillis();
        activeBlockCount = FluidPhysicsMod.getFluidRegistry().getActiveBlocks().size();
        processedBlockCount = 0;
        skippedBlockCount = 0;
    }
    
    /**
     * Ends performance monitoring for a simulation tick.
     */
    public static void endMonitoring() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastProcessTime;
        
        // Update moving average
        processTimes[sampleIndex] = elapsedTime;
        sampleIndex = (sampleIndex + 1) % SAMPLE_SIZE;
        
        // Log performance metrics
        if (FluidPhysicsMod.getConfig().enablePerformanceLogging) {
            System.out.println("[FluidPhysics] Performance: " + elapsedTime + "ms, " + 
                              activeBlockCount + " active blocks, " + 
                              processedBlockCount + " processed, " + 
                              skippedBlockCount + " skipped");
        }
        
        // Adjust simulation parameters if needed
        adjustSimulationParameters();
    }
    
    /**
     * Records a processed block.
     */
    public static void recordProcessedBlock() {
        processedBlockCount++;
    }
    
    /**
     * Records a skipped block.
     */
    public static void recordSkippedBlock() {
        skippedBlockCount++;
    }
    
    /**
     * Adjusts simulation parameters based on performance metrics.
     */
    private static void adjustSimulationParameters() {
        // Calculate average processing time
        long totalTime = 0;
        for (long time : processTimes) {
            totalTime += time;
        }
        long averageTime = totalTime / SAMPLE_SIZE;
        
        // Get current config
        FluidPhysicsConfig config = FluidPhysicsMod.getConfig();
        
        // If performance is poor, reduce simulation quality
        if (averageTime > HIGH_LOAD_THRESHOLD) {
            // Increase update frequency (less frequent updates)
            if (config.updateFrequency < 5) {
                ModConfig.getInstance().setUpdateFrequency(config.updateFrequency + 1);
            }
            
            // Reduce simulation precision
            if (config.simulationPrecision > 1) {
                ModConfig.getInstance().setSimulationPrecision(config.simulationPrecision - 1);
            }
            
            // Reduce active range
            if (config.activeRange > 32) {
                ModConfig.getInstance().setActiveRange(config.activeRange - 16);
            }
            
            // Log adjustment
            if (config.enablePerformanceLogging) {
                System.out.println("[FluidPhysics] Performance optimization: Reducing simulation quality");
            }
        }
        // If performance is good, increase simulation quality
        else if (averageTime < LOW_LOAD_THRESHOLD) {
            // Decrease update frequency (more frequent updates)
            if (config.updateFrequency > 1) {
                ModConfig.getInstance().setUpdateFrequency(config.updateFrequency - 1);
            }
            
            // Increase simulation precision
            if (config.simulationPrecision < 4) {
                ModConfig.getInstance().setSimulationPrecision(config.simulationPrecision + 1);
            }
            
            // Increase active range
            if (config.activeRange < 128) {
                ModConfig.getInstance().setActiveRange(config.activeRange + 16);
            }
            
            // Log adjustment
            if (config.enablePerformanceLogging) {
                System.out.println("[FluidPhysics] Performance optimization: Increasing simulation quality");
            }
        }
    }
    
    /**
     * Handles world tick events for performance monitoring.
     * 
     * @param event The tick event
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        // Only process on server side and at the end of the tick
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        
        // Get the configuration
        FluidPhysicsConfig config = FluidPhysicsMod.getConfig();
        
        // Only monitor every N ticks
        if (event.level.getGameTime() % 100 == 0) {
            // Check for large fluid bodies
            checkForLargeFluidBodies(event.level);
        }
    }
    
    /**
     * Checks for large fluid bodies that might cause performance issues.
     * 
     * @param world The Minecraft world
     */
    private static void checkForLargeFluidBodies(Level world) {
        FluidRegistry registry = FluidPhysicsMod.getFluidRegistry();
        int activeBlocks = registry.getActiveBlocks().size();
        
        // If there are too many active blocks, optimize by chunking
        if (activeBlocks > 1000) {
            // Group active blocks by chunk
            java.util.Map<Long, java.util.List<BlockPos>> blocksByChunk = new java.util.HashMap<>();
            
            for (BlockPos pos : registry.getActiveBlocks()) {
                long chunkPos = ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
                blocksByChunk.computeIfAbsent(chunkPos, k -> new java.util.ArrayList<>()).add(pos);
            }
            
            // Find chunks with too many active blocks
            for (java.util.Map.Entry<Long, java.util.List<BlockPos>> entry : blocksByChunk.entrySet()) {
                if (entry.getValue().size() > 100) {
                    // This chunk has too many active blocks, optimize it
                    optimizeChunk(world, entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    /**
     * Optimizes a chunk with too many active fluid blocks.
     * 
     * @param world The Minecraft world
     * @param chunkPos The chunk position
     * @param blocks The active blocks in this chunk
     */
    private static void optimizeChunk(Level world, long chunkPos, java.util.List<BlockPos> blocks) {
        // Get the fluid simulator
        FluidSimulator simulator = FluidPhysicsMod.getFluidSimulator();
        
        // Mark some blocks as inactive to reduce processing
        int toDeactivate = blocks.size() / 2;
        
        for (int i = 0; i < toDeactivate; i++) {
            BlockPos pos = blocks.get(i);
            FluidPhysicsMod.getFluidRegistry().markInactive(pos);
        }
        
        // Log optimization
        if (FluidPhysicsMod.getConfig().enablePerformanceLogging) {
            System.out.println("[FluidPhysics] Performance optimization: Deactivated " + 
                              toDeactivate + " blocks in chunk " + 
                              (chunkPos >> 32) + ", " + (chunkPos & 0xFFFFFFFFL));
        }
    }
}
