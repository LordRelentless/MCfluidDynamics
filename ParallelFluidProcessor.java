package com.fluidphysics.minecraft;

import com.fluidphysics.core.FluidLevelData;
import com.fluidphysics.core.FluidRegistry;
import com.fluidphysics.core.FluidSimulator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Handles multithreaded processing of fluid simulation.
 * Divides work across multiple threads for improved performance.
 */
@Mod.EventBusSubscriber
public class ParallelFluidProcessor {
    // Thread pool for parallel processing
    private static ExecutorService threadPool;
    
    // Number of threads to use
    private static int threadCount = 4;
    
    // Map of active tasks
    private static final Map<Integer, Future<?>> activeTasks = new ConcurrentHashMap<>();
    
    // Performance metrics
    private static long totalProcessingTime = 0;
    private static int processedChunks = 0;
    
    /**
     * Initializes the parallel processor.
     * 
     * @param threads The number of threads to use
     */
    public static void initialize(int threads) {
        threadCount = Math.max(1, Math.min(threads, Runtime.getRuntime().availableProcessors()));
        threadPool = Executors.newFixedThreadPool(threadCount);
        
        System.out.println("[FluidPhysics] Parallel processor initialized with " + threadCount + " threads");
    }
    
    /**
     * Shuts down the parallel processor.
     */
    public static void shutdown() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
    
    /**
     * Processes fluid simulation in parallel.
     * 
     * @param world The Minecraft world
     * @param simulator The fluid simulator
     * @param registry The fluid registry
     */
    public static void processInParallel(Level world, FluidSimulator simulator, FluidRegistry registry) {
        // Skip if disabled
        if (!FluidPhysicsMod.getConfig().enableParallelProcessing) {
            return;
        }
        
        // Start timing
        long startTime = System.currentTimeMillis();
        
        // Group active blocks by chunk
        Map<Integer, Map<BlockPos, FluidLevelData>> blocksByChunk = new HashMap<>();
        
        for (BlockPos pos : registry.getActiveBlocks()) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            int chunkKey = (chunkX << 16) | (chunkZ & 0xFFFF);
            
            blocksByChunk.computeIfAbsent(chunkKey, k -> new HashMap<>())
                .put(pos, registry.getFluidData(pos));
        }
        
        // Process each chunk in parallel
        for (Map.Entry<Integer, Map<BlockPos, FluidLevelData>> entry : blocksByChunk.entrySet()) {
            int chunkKey = entry.getKey();
            Map<BlockPos, FluidLevelData> chunkBlocks = entry.getValue();
            
            // Skip if this chunk is already being processed
            if (activeTasks.containsKey(chunkKey) && !activeTasks.get(chunkKey).isDone()) {
                continue;
            }
            
            // Submit task for this chunk
            Future<?> task = threadPool.submit(() -> {
                try {
                    processChunk(world, simulator, registry, chunkBlocks);
                } catch (Exception e) {
                    System.err.println("[FluidPhysics] Error processing chunk: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            activeTasks.put(chunkKey, task);
        }
        
        // Wait for all tasks to complete
        for (Future<?> task : activeTasks.values()) {
            try {
                if (!task.isDone()) {
                    task.get();
                }
            } catch (Exception e) {
                System.err.println("[FluidPhysics] Error waiting for task: " + e.getMessage());
            }
        }
        
        // Clear completed tasks
        activeTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
        
        // End timing
        long endTime = System.currentTimeMillis();
        totalProcessingTime += (endTime - startTime);
        processedChunks += blocksByChunk.size();
        
        // Log performance metrics
        if (FluidPhysicsMod.getConfig().enablePerformanceLogging && processedChunks >= 10) {
            System.out.println("[FluidPhysics] Parallel processing: " + 
                              (totalProcessingTime / processedChunks) + "ms per chunk average");
            totalProcessingTime = 0;
            processedChunks = 0;
        }
    }
    
    /**
     * Processes a single chunk of fluid blocks.
     * 
     * @param world The Minecraft world
     * @param simulator The fluid simulator
     * @param registry The fluid registry
     * @param chunkBlocks The blocks in this chunk
     */
    private static void processChunk(Level world, FluidSimulator simulator, FluidRegistry registry, 
                                    Map<BlockPos, FluidLevelData> chunkBlocks) {
        // Process each block in this chunk
        for (Map.Entry<BlockPos, FluidLevelData> entry : chunkBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            FluidLevelData fluidData = entry.getValue();
            
            // Skip if this block is no longer active
            if (!registry.isActive(pos)) {
                continue;
            }
            
            // Process this block
            simulator.processFluidBlock(world, pos, fluidData);
            
            // Record processed block
            PerformanceOptimizer.recordProcessedBlock();
        }
    }
    
    /**
     * Handles world tick events for parallel processing.
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
        
        // Skip if parallel processing is disabled
        if (!config.enableParallelProcessing) {
            return;
        }
        
        // Only process every N ticks based on update frequency
        if (event.level.getGameTime() % config.updateFrequency != 0) {
            return;
        }
        
        // Process in parallel
        processInParallel(event.level, FluidPhysicsMod.getFluidSimulator(), FluidPhysicsMod.getFluidRegistry());
    }
}
