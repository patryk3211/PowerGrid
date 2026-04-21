package org.patryk3211.powergrid.electricity.sim.solver;

/**
 * Multi-tick hooks, called only when multi ticks are > 1
 */
public interface IMultiHooks {
    /**
     * Called before any solving is started, once per tick (regardless of multi-ticks)
     */
    default void prepare(int multiTicks) { }

    /**
     * Called after a single small tick solve
     */
    default void postMicroTick() { }
}
