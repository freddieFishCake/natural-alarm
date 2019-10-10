package net.htb.naturalalarm;

import org.immutables.value.Value;

/**
 *
 * @author Jonathan Lister
 */
@Value.Immutable
public interface LedControl {

    @Value.Default
    default int getStart() {
        return 0;
    }
    @Value.Default
    default int getEnd() {
        return 0;
    }
    @Value.Default
    default float getStep() {
        return 1.0f;
    }

    @Value.Derived
    default boolean isDimming() {
        return getStart() > getEnd();
    }    
    @Value.Derived
    default boolean isBrighteinng() {
        return getStart() < getEnd();
    }    
    @Value.Derived
    default boolean isStatic() {
        return getStart() == getEnd();
    }    
    
    @Value.Check
    default void validate() {
        if (getStep() < 0.0f) {
            throw new IllegalStateException("Step must be > 0.0");
        }
    }
}
