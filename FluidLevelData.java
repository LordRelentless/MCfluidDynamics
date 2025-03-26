package com.fluidphysics.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidType;

/**
 * Stores and manages fluid level data for a single block.
 * This is the core data structure for the fluid physics system.
 */
public class FluidLevelData {
    // Constants
    public static final int MAX_LEVEL = 8; // Full block
    public static final int MIN_LEVEL = 0; // Empty block
    
    // Properties
    private FluidType fluidType;
    private int level; // 0-8, where 8 is a full block
    private FlowDirection momentum;
    private boolean isSource;
    private long lastUpdateTime;
    
    /**
     * Creates a new FluidLevelData instance with default values.
     */
    public FluidLevelData() {
        this.fluidType = null;
        this.level = 0;
        this.momentum = FlowDirection.NONE;
        this.isSource = false;
        this.lastUpdateTime = 0;
    }
    
    /**
     * Creates a new FluidLevelData instance with the specified fluid type and level.
     * 
     * @param fluidType The type of fluid
     * @param level The fluid level (0-8)
     * @param isSource Whether this is a source block
     */
    public FluidLevelData(FluidType fluidType, int level, boolean isSource) {
        this.fluidType = fluidType;
        this.level = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        this.momentum = FlowDirection.NONE;
        this.isSource = isSource;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Creates a FluidLevelData instance from a Minecraft FluidState.
     * 
     * @param state The Minecraft fluid state
     * @return A new FluidLevelData instance
     */
    public static FluidLevelData fromFluidState(FluidState state) {
        if (state.isEmpty()) {
            return new FluidLevelData();
        }
        
        FluidType fluidType = state.getType().getFluidType();
        boolean isSource = state.isSource();
        int level = isSource ? MAX_LEVEL : state.getAmount();
        
        return new FluidLevelData(fluidType, level, isSource);
    }
    
    /**
     * Gets the fluid type.
     * 
     * @return The fluid type
     */
    public FluidType getFluidType() {
        return fluidType;
    }
    
    /**
     * Sets the fluid type.
     * 
     * @param fluidType The new fluid type
     */
    public void setFluidType(FluidType fluidType) {
        this.fluidType = fluidType;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the fluid level.
     * 
     * @return The fluid level (0-8)
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Sets the fluid level, clamped to the valid range.
     * 
     * @param level The new fluid level
     */
    public void setLevel(int level) {
        this.level = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        this.lastUpdateTime = System.currentTimeMillis();
        
        // If level is 0, clear the fluid type
        if (this.level == 0) {
            this.fluidType = null;
            this.isSource = false;
        }
    }
    
    /**
     * Adds fluid to this block, up to the maximum level.
     * 
     * @param amount The amount to add
     * @param type The fluid type to add
     * @return The amount that couldn't be added (overflow)
     */
    public int addFluid(int amount, FluidType type) {
        // If this block is empty or has the same fluid type
        if (isEmpty() || this.fluidType == type) {
            // Set the fluid type if empty
            if (isEmpty()) {
                this.fluidType = type;
            }
            
            int newLevel = this.level + amount;
            int overflow = Math.max(0, newLevel - MAX_LEVEL);
            this.level = Math.min(MAX_LEVEL, newLevel);
            this.lastUpdateTime = System.currentTimeMillis();
            
            // If we reach max level, mark as source
            if (this.level == MAX_LEVEL) {
                this.isSource = true;
            }
            
            return overflow;
        }
        
        // Can't mix fluid types
        return amount;
    }
    
    /**
     * Removes fluid from this block.
     * 
     * @param amount The amount to remove
     * @return The actual amount removed
     */
    public int removeFluid(int amount) {
        int actualAmount = Math.min(this.level, amount);
        this.level -= actualAmount;
        this.lastUpdateTime = System.currentTimeMillis();
        
        // If level is 0, clear the fluid type
        if (this.level == 0) {
            this.fluidType = null;
            this.isSource = false;
        } else if (this.level < MAX_LEVEL) {
            // If level is less than max, it's no longer a source
            this.isSource = false;
        }
        
        return actualAmount;
    }
    
    /**
     * Gets the flow momentum direction.
     * 
     * @return The flow direction
     */
    public FlowDirection getMomentum() {
        return momentum;
    }
    
    /**
     * Sets the flow momentum direction.
     * 
     * @param momentum The new flow direction
     */
    public void setMomentum(FlowDirection momentum) {
        this.momentum = momentum;
    }
    
    /**
     * Checks if this block is a fluid source.
     * 
     * @return True if this is a source block
     */
    public boolean isSource() {
        return isSource;
    }
    
    /**
     * Sets whether this block is a fluid source.
     * 
     * @param isSource True if this is a source block
     */
    public void setSource(boolean isSource) {
        this.isSource = isSource;
    }
    
    /**
     * Gets the last update time.
     * 
     * @return The last update time in milliseconds
     */
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    /**
     * Updates the last update time to the current time.
     */
    public void updateTime() {
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Checks if this block is empty (no fluid).
     * 
     * @return True if this block is empty
     */
    public boolean isEmpty() {
        return this.level == 0 || this.fluidType == null;
    }
    
    /**
     * Checks if this block is full (maximum level).
     * 
     * @return True if this block is full
     */
    public boolean isFull() {
        return this.level == MAX_LEVEL;
    }
    
    /**
     * Serializes this fluid level data to an NBT tag.
     * 
     * @return The NBT tag
     */
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        
        // Only serialize if not empty
        if (!isEmpty()) {
            tag.putString("FluidType", fluidType.getRegistryName().toString());
            tag.putInt("Level", level);
            tag.putInt("Momentum", momentum.ordinal());
            tag.putBoolean("IsSource", isSource);
            tag.putLong("LastUpdateTime", lastUpdateTime);
        }
        
        return tag;
    }
    
    /**
     * Deserializes fluid level data from an NBT tag.
     * 
     * @param tag The NBT tag
     * @return The deserialized fluid level data
     */
    public static FluidLevelData deserialize(CompoundTag tag) {
        if (tag.isEmpty()) {
            return new FluidLevelData();
        }
        
        FluidLevelData data = new FluidLevelData();
        
        // Get fluid type from registry name
        String fluidTypeName = tag.getString("FluidType");
        if (!fluidTypeName.isEmpty()) {
            // This would need to be adapted to the actual fluid registry system
            // For now, we'll just use water as a placeholder
            data.fluidType = Fluids.WATER.getFluidType();
        }
        
        data.level = tag.getInt("Level");
        data.momentum = FlowDirection.values()[tag.getInt("Momentum")];
        data.isSource = tag.getBoolean("IsSource");
        data.lastUpdateTime = tag.getLong("LastUpdateTime");
        
        return data;
    }
}
