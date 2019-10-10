package net.htb.naturalalarm;

import org.immutables.value.Value;

/**
 *
 * @author Jonathan Lister
 */
@Value.Immutable
public interface ChangeRequest {
    int getSteps();
    int getMillisBetweenSteps();
    @Value.Default
    default LedControl getRed() {
      return ImmutableLedControl.builder().build();
    }
    @Value.Default
    default LedControl getGreen() {
      return ImmutableLedControl.builder().build();
    }
    @Value.Default
    default LedControl getBlue() {
      return ImmutableLedControl.builder().build();
    }
}
