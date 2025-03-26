package com.fluidphysics.core;

/**
 * Represents the direction of fluid flow momentum.
 * Used to track the direction of fluid movement for momentum calculations.
 */
public enum FlowDirection {
    NONE,       // No flow
    NORTH,      // Flowing north
    EAST,       // Flowing east
    SOUTH,      // Flowing south
    WEST,       // Flowing west
    UP,         // Flowing up (for special cases like pumps)
    DOWN;       // Flowing down (gravity)
    
    /**
     * Gets the opposite flow direction.
     * 
     * @return The opposite direction
     */
    public FlowDirection getOpposite() {
        switch (this) {
            case NORTH: return SOUTH;
            case EAST: return WEST;
            case SOUTH: return NORTH;
            case WEST: return EAST;
            case UP: return DOWN;
            case DOWN: return UP;
            default: return NONE;
        }
    }
    
    /**
     * Checks if this direction is horizontal.
     * 
     * @return True if this is a horizontal direction
     */
    public boolean isHorizontal() {
        return this == NORTH || this == EAST || this == SOUTH || this == WEST;
    }
    
    /**
     * Checks if this direction is vertical.
     * 
     * @return True if this is a vertical direction
     */
    public boolean isVertical() {
        return this == UP || this == DOWN;
    }
    
    /**
     * Gets the X offset for this direction.
     * 
     * @return The X offset (-1, 0, or 1)
     */
    public int getXOffset() {
        return this == EAST ? 1 : (this == WEST ? -1 : 0);
    }
    
    /**
     * Gets the Y offset for this direction.
     * 
     * @return The Y offset (-1, 0, or 1)
     */
    public int getYOffset() {
        return this == UP ? 1 : (this == DOWN ? -1 : 0);
    }
    
    /**
     * Gets the Z offset for this direction.
     * 
     * @return The Z offset (-1, 0, or 1)
     */
    public int getZOffset() {
        return this == SOUTH ? 1 : (this == NORTH ? -1 : 0);
    }
}
