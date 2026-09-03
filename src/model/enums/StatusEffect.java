package model.enums;

public enum StatusEffect {
  FROZEN,
  /**
   * Held in place by a blow rather than by ice -- Kernel-pult's butter. Stops a zombie exactly as
   * FROZEN does, and is a separate effect only so the view can draw it as a stun instead of
   * freezing something the player never froze.
   */
  STUNNED,
  CHILLED,
  POISONED,
  HYPNOTIZED,
  BURNED,
  OCTOPUS_BIND
}
