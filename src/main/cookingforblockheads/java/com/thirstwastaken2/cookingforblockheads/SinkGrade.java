package com.thirstwastaken2.cookingforblockheads;

/**
 * The grade of the water in a Cooking for Blockheads Sink's tank, kept on its block entity by
 * {@code SinkBlockEntityMixin} and saved with it. Only meaningful while the Sink is a tank
 * ({@code sinkRequiresWater}) and holds water; see {@link SinkWater}.
 */
public interface SinkGrade {
    /** The grade as {@link SinkWater#encode} writes it, or {@link SinkWater#UNKNOWN}. */
    int thirst$grade();

    void thirst$setGrade(int grade);
}
