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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Testing and debugging utilities for the fluid physics mod.
 * Provides tools for testing, profiling, and debugging the fluid simulation.
 */
@Mod.EventBusSubscriber
public class TestingUtils {
    // Debug mode flag
    private static boolean debugMode = false;
    
    // Profiling data
    private static final Map<String, Long> timingData = new HashMap<>();
    private static final Map<String, Integer> countData = new HashMap<>();
    
    // Log file
    private static PrintWriter logWriter;
    
    /**
     * Initializes the testing utilities.
     * 
     * @param enableDebug Whether to enable debug mode
     */
    public static void initialize(boolean enableDebug) {
        debugMode = enableDebug;
        
        if (debugMode) {
            try {
                // Create log file with timestamp
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                logWriter = new PrintWriter(new FileWriter("fluidphysics_debug_" + timestamp + ".log"));
                
                log("FluidPhysics Debug Mode Enabled");
                log("Java Version: " + System.getProperty("java.version"));
                log("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                log("Available Processors: " + Runtime.getRuntime().availableProcessors());
                log("Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");
            } catch (IOException e) {
                System.err.println("[FluidPhysics] Error creating debug log: " + e.getMessage());
            }
        }
    }
    
    /**
     * Shuts down the testing utilities.
     */
    public static void shutdown() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
    
    /**
     * Logs a message to the debug log.
     * 
     * @param message The message to log
     */
    public static void log(String message) {
        if (debugMode && logWriter != null) {
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            logWriter.println("[" + timestamp + "] " + message);
            logWriter.flush();
        }
    }
    
    /**
     * Starts timing a section of code.
     * 
     * @param section The section name
     */
    public static void startTiming(String section) {
        if (debugMode) {
            timingData.put(section, System.nanoTime());
        }
    }
    
    /**
     * Ends timing a section of code and logs the result.
     * 
     * @param section The section name
     */
    public static void endTiming(String section) {
        if (debugMode && timingData.containsKey(section)) {
            long startTime = timingData.get(section);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000; // Convert to ms
            
            // Update count
            int count = countData.getOrDefault(section, 0) + 1;
            countData.put(section, count);
            
            // Log timing
            log("Timing [" + section + "]: " + duration + "ms (call #" + count + ")");
        }
    }
    
    /**
     * Runs a test case for the fluid simulation.
     * 
     * @param world The Minecraft world
     * @param testName The test name
     * @param testCase The test case to run
     */
    public static void runTest(Level world, String testName, Runnable testCase) {
        if (!debugMode) {
            return;
        }
        
        log("Starting test: " + testName);
        startTiming(testName);
        
        try {
            testCase.run();
            log("Test completed successfully: " + testName);
        } catch (Exception e) {
            log("Test failed: " + testName);
            log("Error: " + e.getMessage());
            e.printStackTrace(logWriter);
        }
        
        endTiming(testName);
    }
    
    /**
     * Tests the fluid simulation with a simple scenario.
     * 
     * @param world The Minecraft world
     * @param simulator The fluid simulator
     * @param registry The fluid registry
     */
    public static void testSimpleScenario(Level world, FluidSimulator simulator, FluidRegistry registry) {
        runTest(world, "SimpleScenario", () -> {
            // Create a simple water column
            BlockPos basePos = new BlockPos(0, 64, 0);
            
            // Add water at the top
            FluidLevelData sourceData = new FluidLevelData();
            sourceData.setLevel(FluidLevelData.MAX_LEVEL);
            sourceData.setSource(true);
            registry.setFluidData(basePos.above(5), sourceData);
            
            // Process several ticks
            for (int i = 0; i < 10; i++) {
                log("Processing tick " + i);
                simulator.processTick(world);
                
                // Log the state after each tick
                for (int y = 0; y <= 5; y++) {
                    BlockPos pos = basePos.above(y);
                    FluidLevelData data = registry.getFluidData(pos);
                    log("  Block at y=" + pos.getY() + ": " + 
                        (data == null ? "null" : "level=" + data.getLevel() + ", source=" + data.isSource()));
                }
            }
        });
    }
    
    /**
     * Tests the fluid simulation with a pressure scenario.
     * 
     * @param world The Minecraft world
     * @param simulator The fluid simulator
     * @param registry The fluid registry
     */
    public static void testPressureScenario(Level world, FluidSimulator simulator, FluidRegistry registry) {
        runTest(world, "PressureScenario", () -> {
            // Create a U-shaped water container
            BlockPos basePos = new BlockPos(10, 64, 10);
            
            // Add water on one side
            FluidLevelData sourceData = new FluidLevelData();
            sourceData.setLevel(FluidLevelData.MAX_LEVEL);
            sourceData.setSource(true);
            
            // Left column (full)
            for (int y = 0; y < 5; y++) {
                registry.setFluidData(basePos.above(y).west(2), sourceData);
            }
            
            // Bottom connector
            registry.setFluidData(basePos.west(1), sourceData);
            registry.setFluidData(basePos, sourceData);
            registry.setFluidData(basePos.east(1), sourceData);
            
            // Right column (empty)
            for (int y = 1; y < 5; y++) {
                registry.setFluidData(basePos.above(y).east(2), new FluidLevelData());
            }
            
            // Process several ticks
            for (int i = 0; i < 20; i++) {
                log("Processing tick " + i);
                simulator.processTick(world);
                
                // Log the state of both columns
                log("  Left column:");
                for (int y = 0; y < 5; y++) {
                    BlockPos pos = basePos.above(y).west(2);
                    FluidLevelData data = registry.getFluidData(pos);
                    log("    Block at y=" + pos.getY() + ": " + 
                        (data == null ? "null" : "level=" + data.getLevel() + ", source=" + data.isSource()));
                }
                
                log("  Right column:");
                for (int y = 0; y < 5; y++) {
                    BlockPos pos = basePos.above(y).east(2);
                    FluidLevelData data = registry.getFluidData(pos);
                    log("    Block at y=" + pos.getY() + ": " + 
                        (data == null ? "null" : "level=" + data.getLevel() + ", source=" + data.isSource()));
                }
            }
        });
    }
    
    /**
     * Handles world tick events for testing.
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
        
        // Skip if debug mode is disabled
        if (!debugMode) {
            return;
        }
        
        // Run tests at specific times
        if (event.level.getGameTime() == 100) {
            testSimpleScenario(event.level, FluidPhysicsMod.getFluidSimulator(), FluidPhysicsMod.getFluidRegistry());
        } else if (event.level.getGameTime() == 200) {
            testPressureScenario(event.level, FluidPhysicsMod.getFluidSimulator(), FluidPhysicsMod.getFluidRegistry());
        }
    }
}
