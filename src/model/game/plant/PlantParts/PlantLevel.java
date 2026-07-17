package model.game.plant.PlantParts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlantLevel {
  private static final Pattern NUMBER_PATTERN = Pattern.compile("([+-]\\s?\\d+)");

  private final int hpDelta;
  private final int damageDelta;
  private final int costDelta;
  private final int cooldownDeltaSeconds;
  private final int actionIntervalDeltaSeconds;
  private final List<String> rawEffects;

  private PlantLevel(
      int hpDelta,
      int damageDelta,
      int costDelta,
      int cooldownDeltaSeconds,
      int actionIntervalDeltaSeconds,
      List<String> rawEffects) {
    this.hpDelta = hpDelta;
    this.damageDelta = damageDelta;
    this.costDelta = costDelta;
    this.cooldownDeltaSeconds = cooldownDeltaSeconds;
    this.actionIntervalDeltaSeconds = actionIntervalDeltaSeconds;
    this.rawEffects = rawEffects;
  }

  public static PlantLevel none() {
    return new PlantLevel(0, 0, 0, 0, 0, new ArrayList<>());
  }

  public static PlantLevel parse(String raw) {
    if (raw == null || raw.trim().isEmpty()) {
      return none();
    }

    String text = raw.trim();
    String lower = text.toLowerCase();

    Matcher numberMatcher = NUMBER_PATTERN.matcher(text);
    Integer value = null;
    if (numberMatcher.find()) {
      try {
        value = Integer.parseInt(numberMatcher.group(1).replace(" ", ""));
      } catch (NumberFormatException e) {
        value = null;
      }
    }

    List<String> effects = new ArrayList<>();
    effects.add(text);

    if (value == null) {
      return new PlantLevel(0, 0, 0, 0, 0, effects);
    }
    if (lower.contains("hp")) {
      return new PlantLevel(value, 0, 0, 0, 0, effects);
    }
    if (lower.contains("dmg") || lower.contains("damage")) {
      return new PlantLevel(0, value, 0, 0, 0, effects);
    }
    if (lower.contains("cost")) {
      return new PlantLevel(0, 0, value, 0, 0, effects);
    }
    if (lower.contains("cooldown") || lower.contains("recharge")) {
      return new PlantLevel(0, 0, 0, value, 0, effects);
    }
    if (lower.contains("prod. time")
        || lower.contains("grow time")
        || lower.contains("regen")
        || lower.contains("arm time")
        || lower.contains("charge time")
        || lower.contains("digest")) {
      return new PlantLevel(0, 0, 0, 0, value, effects);
    }

    return new PlantLevel(0, 0, 0, 0, 0, effects);
  }

  public PlantLevel add(PlantLevel other) {
    if (other == null) return this;
    List<String> combined = new ArrayList<>(this.rawEffects);
    combined.addAll(other.rawEffects);
    return new PlantLevel(
        this.hpDelta + other.hpDelta,
        this.damageDelta + other.damageDelta,
        this.costDelta + other.costDelta,
        this.cooldownDeltaSeconds + other.cooldownDeltaSeconds,
        this.actionIntervalDeltaSeconds + other.actionIntervalDeltaSeconds,
        combined);
  }

  public static PlantLevel cumulative(PlantTemplate template, int targetLevel) {
    PlantLevel result = none();
    if (template == null || targetLevel <= 1) {
      return result;
    }
    if (targetLevel >= 2) {
      result = result.add(parse(template.lvl2));
    }
    if (targetLevel >= 3) {
      result = result.add(parse(template.lvl3));
    }
    if (targetLevel >= 4) {
      result = result.add(parse(template.lvl4));
    }
    return result;
  }

  public int getHpDelta() {
    return hpDelta;
  }

  public int getDamageDelta() {
    return damageDelta;
  }

  public int getCostDelta() {
    return costDelta;
  }

  public int getCooldownDeltaSeconds() {
    return cooldownDeltaSeconds;
  }

  public int getActionIntervalDeltaSeconds() {
    return actionIntervalDeltaSeconds;
  }

  public List<String> getRawEffects() {
    return rawEffects;
  }
}