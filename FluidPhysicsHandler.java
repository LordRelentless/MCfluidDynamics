package com.fluidphysics.minecraft;

import com.fluidphysics.core.FluidLevelData;
import com.fluidphysics.core.FluidPhysicsConfig;
import com.fluidphysics.core.FluidRegistry;
import com.fluidphysics.core.FluidSimulator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for Minecraft events.
 * Hooks into block placement, destruction, and world tick events.
 * Triggers simulation updates at appropriate times.
 */
@Mod.EventBusSubscriber
public class FluidPhysicsHandler {
    // The fluid simulator
    private static FluidSimulator fluidSimulator;
    
    // The fluid registry
    private static FluidRegistry fluidRegistry;
    
    // Tick counter for update frequency
    private static int tickCounter = 0;
    
    /**
     * Initializes the fluid physics handler.
     * 
     * @param config The fluid physics configuration
     */
    public static void initialize(FluidPhysicsConfig config) {
        fluidSimulator = new FluidSimulator(config);
        fluidRegistry = FluidRegistry.getInstance(config);
    }
    
    /**
     * Handles world tick events.
     * 
     * @param event The tick event
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        // Only process on server side and at the end of the tick
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        
        // Get the configuration from the simulator
        FluidPhysicsConfig config = fluidSimulator.getConfig();
        
        // Increment tick counter
        tickCounter++;
        
        // Only process every N ticks based on update frequency
        if (tickCounter % config.updateFrequency != 0) {
            return;
        }
        
        // Process the fluid simulation
        fluidSimulator.processTick(event.level);
    }
    
    /**
     * Handles block placement events.
     * 
     * @param event The block place event
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // Only process on server side
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        Level world = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        
        // If this is a fluid block, register it
        if (!state.getFluidState().isEmpty()) {
            fluidRegistry.registerFluidBlock(pos, state);
        }
        
        // Check adjacent blocks for fluids that might be affected
        checkAdjacentBlocks(world, pos);
    }
    
    /**
     * Handles block break events.
     * 
     * @param event The block break event
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Only process on server side
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        Level world = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        
        // Unregister this block
        fluidRegistry.unregisterFluidBlock(pos);
        
        // Check adjacent blocks for fluids that might be affected
        checkAdjacentBlocks(world, pos);
    }
    
    /**
     * Handles chunk load events.
     * 
     * @param event The chunk load event
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Only process on server side
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        Level world = (Level) event.getLevel();
        int chunkX = event.getChunk().getPos().x;
        int chunkZ = event.getChunk().getPos().z;
        
        // Register all fluid blocks in this chunk
        fluidRegistry.onChunkLoad(world, chunkX, chunkZ);
    }
    
    /**
     * Handles chunk unload events.
     * 
     * @param event The chunk unload event
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        // Only process on server side
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        int chunkX = event.getChunk().getPos().x;
        int chunkZ = event.getChunk().getPos().z;
        
        // Unregister all fluid blocks in this chunk
        fluidRegistry.onChunkUnload(chunkX, chunkZ);
    }
    
    /**
     * Checks adjacent blocks for fluids that might be affected by a block change.
     * 
     * @param world The Minecraft world
     * @param pos The position of the changed block
     */
    private static void checkAdjacentBlocks(Level world, BlockPos pos) {
        // Check all six adjacent blocks
        checkBlock(world, pos.north());
        checkBlock(world, pos.east());
        checkBlock(world, pos.south());
        checkBlock(world, pos.west());
        checkBlock(world, pos.above());
        checkBlock(world, pos.below());
    }
    
    /**
     * Checks a block for fluid and registers it if found.
     * 
     * @param world The Minecraft world
     * @param pos The position to check
     */
    private static void checkBlock(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        
        // If this is a fluid block, register it
        if (!state.getFluidState().isEmpty()) {
            fluidRegistry.registerFluidBlock(pos, state);
            fluidRegistry.markActive(pos);
        }
    }
}
