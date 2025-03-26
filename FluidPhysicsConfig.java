package com.fluidphysics.core;

/**
 * Configuration class for the fluid physics system.
 * Contains settings that control the behavior and performance of the simulation.
 */
public class FluidPhysicsConfig {
    // Simulation precision (higher = more realistic but more intensive)
    public final int simulationPrecision;
    
    // Flow rate multiplier
    public final float flowRate;
    
    // Enable/disable specific features
    public final boolean enablePressure;
    public final boolean enableMomentum;
    public final boolean enableFiniteFluids;
    
    // Performance settings
    public final int updateFrequency;
    public final int activeRange;
    
    /**
     * Creates a new configuration with default values.
     */
    public FluidPhysicsConfig() {
        this.simulationPrecision = 2;
        this.flowRate = 1.0f;
        this.enablePressure = true;
        this.enableMomentum = true;
        this.enableFiniteFluids = true;
        this.updateFrequency = 1;
        this.activeRange = 64;
    }
    
    /**
     * Creates a new configuration with the specified values.
     * 
     * @param simulationPrecision The simulation precision
     * @param flowRate The flow rate multiplier
     * @param enablePressure Whether to enable pressure-based flow
     * @param enableMomentum Whether to enable momentum-based flow
     * @param enableFiniteFluids Whether to enable finite fluids
     * @param updateFrequency How often the simulation runs (in ticks)
     * @param activeRange Maximum distance for active fluid simulation
     */
    public FluidPhysicsConfig(int simulationPrecision, float flowRate, 
                             boolean enablePressure, boolean enableMomentum, 
                             boolean enableFiniteFluids, int updateFrequency, 
                             int activeRange) {
        this.simulationPrecision = simulationPrecision;
        this.flowRate = flowRate;
        this.enablePressure = enablePressure;
        this.enableMomentum = enableMomentum;
        this.enableFiniteFluids = enableFiniteFluids;
        this.updateFrequency = updateFrequency;
        this.activeRange = activeRange;
    }
    
    /**
     * Creates a high-performance configuration with reduced realism.
     * 
     * @return A high-performance configuration
     */
    public static FluidPhysicsConfig highPerformance() {
        return new FluidPhysicsConfig(
            1,      // Low precision
            1.5f,   // Faster flow rate
            true,   // Enable pressure
            false,  // Disable momentum
            true,   // Enable finite fluids
            2,      // Update every 2 ticks
            32      // Smaller active range
        );
    }
    
    /**
     * Creates a high-realism configuration with reduced performance.
     * 
     * @return A high-realism configuration
     */
    public static FluidPhysicsConfig highRealism() {
        return new FluidPhysicsConfig(
            4,      // High precision
            0.8f,   // Slower, more realistic flow rate
            true,   // Enable pressure
            true,   // Enable momentum
            true,   // Enable finite fluids
            1,      // Update every tick
            128     // Larger active range
        );
    }
    
    /**
     * Creates a balanced configuration with good realism and performance.
     * 
     * @return A balanced configuration
     */
    public static FluidPhysicsConfig balanced() {
        return new FluidPhysicsConfig(
            2,      // Medium precision
            1.0f,   // Standard flow rate
            true,   // Enable pressure
            true,   // Enable momentum
            true,   // Enable finite fluids
            1,      // Update every tick
            64      // Standard active range
        );
    }
}
