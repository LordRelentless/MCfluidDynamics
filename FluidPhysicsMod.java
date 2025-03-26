package com.fluidphysics.minecraft;

import com.fluidphysics.core.FluidPhysicsConfig;
import com.fluidphysics.core.FluidRegistry;
import com.fluidphysics.core.FluidSimulator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Main mod class for Fluid Physics.
 * Handles mod initialization and registration.
 */
@Mod(FluidPhysicsMod.MOD_ID)
public class FluidPhysicsMod {
    // Mod ID
    public static final String MOD_ID = "fluidphysics";
    
    // Registry objects
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MOD_ID);
    
    // Block registry objects
    public static final RegistryObject<Block> WATER_PHYSICS_BLOCK = BLOCKS.register("water_physics_block",
        () -> new FluidPhysicsBlock(Block.Properties.of().noCollission().strength(100.0F).noDrops(), 
                                   FluidRegistry.getInstance(ModConfig.getInstance().getDefaultConfig())));
    
    // Item registry objects
    public static final RegistryObject<Item> WATER_PHYSICS_ITEM = ITEMS.register("water_physics_block",
        () -> new BlockItem(WATER_PHYSICS_BLOCK.get(), new Item.Properties()));
    
    // Block entity registry objects
    public static final RegistryObject<BlockEntityType<FluidBlockEntity>> FLUID_BLOCK_ENTITY = BLOCK_ENTITIES.register(
        "fluid_block_entity",
        () -> BlockEntityType.Builder.of(
            (pos, state) -> new FluidBlockEntity(FLUID_BLOCK_ENTITY.get(), pos, state),
            WATER_PHYSICS_BLOCK.get()
        ).build(null)
    );
    
    // Configuration
    private static FluidPhysicsConfig config;
    
    // Simulator and registry
    private static FluidSimulator fluidSimulator;
    private static FluidRegistry fluidRegistry;
    
    /**
     * Constructor for the mod class.
     */
    public FluidPhysicsMod() {
        // Get the mod event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register deferred registers
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        
        // Register event listeners
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);
        
        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfig.getSpec());
        
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * Common setup event handler.
     * 
     * @param event The setup event
     */
    private void setup(final FMLCommonSetupEvent event) {
        // Initialize configuration
        config = ModConfig.getInstance().getDefaultConfig();
        
        // Initialize simulator and registry
        fluidRegistry = FluidRegistry.getInstance(config);
        fluidSimulator = new FluidSimulator(config);
        
        // Initialize event handler
        FluidPhysicsHandler.initialize(config);
        
        // Register vanilla fluid overrides
        registerFluidOverrides();
    }
    
    /**
     * Client setup event handler.
     * 
     * @param event The client setup event
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        // Register renderers
        event.enqueueWork(() -> {
            // Register block entity renderer
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                FLUID_BLOCK_ENTITY.get(),
                (context) -> new FluidRenderHandler(context, fluidRegistry)
            );
        });
    }
    
    /**
     * Registers overrides for vanilla fluids.
     */
    private void registerFluidOverrides() {
        // Override water
        Fluids.WATER.getFluidType().setCustomBlock(WATER_PHYSICS_BLOCK.get());
        
        // Override flowing water
        Fluids.FLOWING_WATER.getFluidType().setCustomBlock(WATER_PHYSICS_BLOCK.get());
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
