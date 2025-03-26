package com.fluidphysics.core;

/**
 * Handles the mathematical calculations for fluid flow.
 * Implements pressure, gravity, and momentum effects.
 */
public class FlowCalculator {
    // Configuration
    private final FluidPhysicsConfig config;
    
    /**
     * Creates a new FlowCalculator with the specified configuration.
     * 
     * @param config The fluid physics configuration
     */
    public FlowCalculator(FluidPhysicsConfig config) {
        this.config = config;
    }
    
    /**
     * Calculates the amount of fluid that should flow due to gravity.
     * 
     * @param sourceData The source fluid data
     * @param targetData The target fluid data
     * @return The amount of fluid that should flow
     */
    public int calculateGravityFlow(FluidLevelData sourceData, FluidLevelData targetData) {
        // If source is empty, no flow
        if (sourceData.isEmpty()) {
            return 0;
        }
        
        // If target is full or has a different fluid type, no flow
        if (targetData.isFull() || 
            (!targetData.isEmpty() && targetData.getFluidType() != sourceData.getFluidType())) {
            return 0;
        }
        
        // Calculate base flow amount based on source level
        int baseFlow = Math.max(1, sourceData.getLevel() / 2);
        
        // Apply flow rate multiplier
        float adjustedFlow = baseFlow * config.flowRate;
        
        // Apply simulation precision
        adjustedFlow = adjustedFlow * (config.simulationPrecision / 2.0f);
        
        // Calculate maximum flow (how much the target can accept)
        int maxFlow = FluidLevelData.MAX_LEVEL - targetData.getLevel();
        
        // Return the minimum of calculated flow and maximum flow
        return Math.min((int)adjustedFlow, maxFlow);
    }
    
    /**
     * Calculates the amount of fluid that should flow due to pressure differences.
     * 
     * @param sourceData The source fluid data
     * @param targetData The target fluid data
     * @param hasMomentum Whether the source has momentum in this direction
     * @return The amount of fluid that should flow
     */
    public int calculatePressureFlow(FluidLevelData sourceData, FluidLevelData targetData, boolean hasMomentum) {
        // If source is empty or has only 1 level, no horizontal flow
        if (sourceData.isEmpty() || sourceData.getLevel() <= 1) {
            return 0;
        }
        
        // If target is full or has a different fluid type, no flow
        if (targetData.isFull() || 
            (!targetData.isEmpty() && targetData.getFluidType() != sourceData.getFluidType())) {
            return 0;
        }
        
        // Calculate pressure difference
        int pressureDiff = sourceData.getLevel() - targetData.getLevel();
        
        // If no pressure difference or negative pressure, no flow
        if (pressureDiff <= 0) {
            return 0;
        }
        
        // Calculate base flow amount based on pressure difference
        int baseFlow = Math.max(1, pressureDiff / 2);
        
        // Apply momentum bonus if applicable
        if (hasMomentum && config.enableMomentum) {
            baseFlow += 1;
        }
        
        // Apply flow rate multiplier
        float adjustedFlow = baseFlow * config.flowRate;
        
        // Apply simulation precision
        adjustedFlow = adjustedFlow * (config.simulationPrecision / 2.0f);
        
        // Calculate maximum flow (how much the target can accept)
        int maxFlow = Math.min(
            FluidLevelData.MAX_LEVEL - targetData.getLevel(),  // Target capacity
            sourceData.getLevel() - 1                          // Source can't go below 1
        );
        
        // Return the minimum of calculated flow and maximum flow
        return Math.min((int)adjustedFlow, maxFlow);
    }
    
    /**
     * Calculates the amount of fluid that should flow due to momentum.
     * 
     * @param sourceData The source fluid data
     * @param targetData The target fluid data
     * @return The amount of fluid that should flow
     */
    public int calculateMomentumFlow(FluidLevelData sourceData, FluidLevelData targetData) {
        // If momentum is disabled, no flow
        if (!config.enableMomentum) {
            return 0;
        }
        
        // If source is empty or has only 1 level, no flow
        if (sourceData.isEmpty() || sourceData.getLevel() <= 1) {
            return 0;
        }
        
        // If target is full or has a different fluid type, no flow
        if (targetData.isFull() || 
            (!targetData.isEmpty() && targetData.getFluidType() != sourceData.getFluidType())) {
            return 0;
        }
        
        // Calculate base flow amount (momentum causes small flow)
        int baseFlow = 1;
        
        // Apply flow rate multiplier
        float adjustedFlow = baseFlow * config.flowRate;
        
        // Calculate maximum flow (how much the target can accept)
        int maxFlow = Math.min(
            FluidLevelData.MAX_LEVEL - targetData.getLevel(),  // Target capacity
            sourceData.getLevel() - 1                          // Source can't go below 1
        );
        
        // Return the minimum of calculated flow and maximum flow
        return Math.min((int)adjustedFlow, maxFlow);
    }
    
    /**
     * Calculates the pressure at a specific fluid level.
     * 
     * @param level The fluid level
     * @return The pressure value
     */
    public float calculatePressure(int level) {
        // Simple linear pressure model
        return level / (float)FluidLevelData.MAX_LEVEL;
    }
}
