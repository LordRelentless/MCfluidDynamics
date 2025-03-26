package com.fluidphysics.minecraft;

import com.fluidphysics.core.FluidPhysicsConfig;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Configuration handler for the Fluid Physics mod.
 * Manages loading and saving configuration from/to file.
 */
public class ModConfig {
    // Singleton instance
    private static ModConfig instance;
    
    // Config builder
    private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private final ForgeConfigSpec spec;
    
    // Config values
    private final ForgeConfigSpec.IntValue simulationPrecision;
    private final ForgeConfigSpec.DoubleValue flowRate;
    private final ForgeConfigSpec.BooleanValue enablePressure;
    private final ForgeConfigSpec.BooleanValue enableMomentum;
    private final ForgeConfigSpec.BooleanValue enableFiniteFluids;
    private final ForgeConfigSpec.IntValue updateFrequency;
    private final ForgeConfigSpec.IntValue activeRange;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ModConfig() {
        // Create config categories
        builder.comment("Fluid Physics Configuration").push("general");
        
        // Add config options
        simulationPrecision = builder
            .comment("Simulation precision (higher = more realistic but more intensive)")
            .defineInRange("simulationPrecision", 2, 1, 4);
        
        flowRate = builder
            .comment("Flow rate multiplier")
            .defineInRange("flowRate", 1.0, 0.1, 5.0);
        
        enablePressure = builder
            .comment("Enable pressure-based flow")
            .define("enablePressure", true);
        
        enableMomentum = builder
            .comment("Enable momentum-based flow")
            .define("enableMomentum", true);
        
        enableFiniteFluids = builder
            .comment("Enable finite fluids (disable for infinite water sources)")
            .define("enableFiniteFluids", true);
        
        updateFrequency = builder
            .comment("How often the simulation runs (in ticks)")
            .defineInRange("updateFrequency", 1, 1, 20);
        
        activeRange = builder
            .comment("Maximum distance for active fluid simulation")
            .defineInRange("activeRange", 64, 16, 256);
        
        // End category
        builder.pop();
        
        // Build the config spec
        spec = builder.build();
    }
    
    /**
     * Gets the singleton instance, creating it if necessary.
     * 
     * @return The singleton instance
     */
    public static synchronized ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }
    
    /**
     * Gets the config spec.
     * 
     * @return The config spec
     */
    public static ForgeConfigSpec getSpec() {
        return getInstance().spec;
    }
    
    /**
     * Creates a FluidPhysicsConfig from the current config values.
     * 
     * @return The fluid physics configuration
     */
    public FluidPhysicsConfig getDefaultConfig() {
        return new FluidPhysicsConfig(
            simulationPrecision.get(),
            flowRate.get().floatValue(),
            enablePressure.get(),
            enableMomentum.get(),
            enableFiniteFluids.get(),
            updateFrequency.get(),
            activeRange.get()
        );
    }
    
    /**
     * Registers the config with Forge.
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, getSpec());
    }
}
