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

  /**
   * دسته‌های اضافه‌شده برای اثرهای لِوِلی که قبلا فقط داخل rawEffects ذخیره می‌شدند و هیچ‌وقت
   * اعمال نمی‌شدند ("Atk Speed +10%"، "Sun +50"، "Pierce +1"، ...).
   */
  private final int attackSpeedPercent;
  private final int sunDelta;
  private final int pierceDelta;
  private final int rangeDelta;
  private final int durationDeltaSeconds;
  private final int freezeTimeDeltaSeconds;
  private final int lifespanDeltaSeconds;
  private final int targetsDelta;

  private PlantLevel(int hpDelta, int damageDelta, int costDelta, int cooldownDeltaSeconds,
                     int actionIntervalDeltaSeconds, List<String> rawEffects) {
    this(hpDelta, damageDelta, costDelta, cooldownDeltaSeconds, actionIntervalDeltaSeconds,
            rawEffects, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  private PlantLevel(int hpDelta, int damageDelta, int costDelta, int cooldownDeltaSeconds,
                     int actionIntervalDeltaSeconds, List<String> rawEffects,
                     int attackSpeedPercent, int sunDelta, int pierceDelta, int rangeDelta,
                     int durationDeltaSeconds, int freezeTimeDeltaSeconds,
                     int lifespanDeltaSeconds, int targetsDelta) {
    this.hpDelta = hpDelta;
    this.damageDelta = damageDelta;
    this.costDelta = costDelta;
    this.cooldownDeltaSeconds = cooldownDeltaSeconds;
    this.actionIntervalDeltaSeconds = actionIntervalDeltaSeconds;
    this.rawEffects = rawEffects;
    this.attackSpeedPercent = attackSpeedPercent;
    this.sunDelta = sunDelta;
    this.pierceDelta = pierceDelta;
    this.rangeDelta = rangeDelta;
    this.durationDeltaSeconds = durationDeltaSeconds;
    this.freezeTimeDeltaSeconds = freezeTimeDeltaSeconds;
    this.lifespanDeltaSeconds = lifespanDeltaSeconds;
    this.targetsDelta = targetsDelta;
  }

  public static PlantLevel none() {
    return new PlantLevel(0, 0, 0, 0, 0, new ArrayList<>());
  }

  public static PlantLevel parse(String raw) {
    if (raw == null || raw.trim().isEmpty()) return none();

    String text = raw.trim();
    String lower = text.toLowerCase();
    Matcher numberMatcher = NUMBER_PATTERN.matcher(text);
    Integer value = null;

    if (numberMatcher.find()) {
      try {
        value = Integer.parseInt(numberMatcher.group(1).replace(" ", ""));
      } catch (NumberFormatException ignored) {
        value = null;
      }
    }

    List<String> effects = new ArrayList<>();
    effects.add(text);

    if (value == null) return new PlantLevel(0, 0, 0, 0, 0, effects);
    if (lower.contains("hp")) return new PlantLevel(value, 0, 0, 0, 0, effects);
    if (lower.contains("dmg") || lower.contains("damage")) return new PlantLevel(0, value, 0, 0, 0, effects);
    if (lower.contains("cost")) return new PlantLevel(0, 0, value, 0, 0, effects);
    if (lower.contains("cooldown") || lower.contains("recharge")) {
      return new PlantLevel(0, 0, 0, value, 0, effects);
    }
    if (lower.contains("prod. time") || lower.contains("grow time") || lower.contains("regen")
            || lower.contains("arm time") || lower.contains("charge time") || lower.contains("digest")
            || lower.contains("eat time")) {
      return new PlantLevel(0, 0, 0, 0, value, effects);
    }

    // اثرهایی که تا حالا فقط متن بودند و روی گیم‌پلی سوار نمی‌شدند
    if (lower.contains("atk speed")) {
      return withExtra(effects, value, 0, 0, 0, 0, 0, 0, 0);
    }
    if (lower.contains("sun")) {
      return withExtra(effects, 0, value, 0, 0, 0, 0, 0, 0);
    }
    if (lower.contains("pierce")) {
      return withExtra(effects, 0, 0, value, 0, 0, 0, 0, 0);
    }
    if (lower.contains("range") || lower.contains("radius") || lower.contains("max size")) {
      return withExtra(effects, 0, 0, 0, value, 0, 0, 0, 0);
    }
    if (lower.contains("duration")) {
      return withExtra(effects, 0, 0, 0, 0, value, 0, 0, 0);
    }
    if (lower.contains("freeze time") || lower.contains("chill time")) {
      return withExtra(effects, 0, 0, 0, 0, 0, value, 0, 0);
    }
    if (lower.contains("lifespan")) {
      return withExtra(effects, 0, 0, 0, 0, 0, 0, value, 0);
    }
    if (lower.contains("targets") || lower.contains("bounces")) {
      return withExtra(effects, 0, 0, 0, 0, 0, 0, 0, value);
    }

    return new PlantLevel(0, 0, 0, 0, 0, effects);
  }

  private static PlantLevel withExtra(List<String> effects, int attackSpeedPercent, int sunDelta,
                                      int pierceDelta, int rangeDelta, int durationDeltaSeconds,
                                      int freezeTimeDeltaSeconds, int lifespanDeltaSeconds,
                                      int targetsDelta) {
    return new PlantLevel(0, 0, 0, 0, 0, effects, attackSpeedPercent, sunDelta, pierceDelta,
            rangeDelta, durationDeltaSeconds, freezeTimeDeltaSeconds, lifespanDeltaSeconds,
            targetsDelta);
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
            combined,
            this.attackSpeedPercent + other.attackSpeedPercent,
            this.sunDelta + other.sunDelta,
            this.pierceDelta + other.pierceDelta,
            this.rangeDelta + other.rangeDelta,
            this.durationDeltaSeconds + other.durationDeltaSeconds,
            this.freezeTimeDeltaSeconds + other.freezeTimeDeltaSeconds,
            this.lifespanDeltaSeconds + other.lifespanDeltaSeconds,
            this.targetsDelta + other.targetsDelta
    );
  }

  public static PlantLevel cumulative(PlantTemplate template, int targetLevel) {
    PlantLevel result = none();
    if (template == null || targetLevel <= 1) return result;

    if (targetLevel >= 2) result = result.add(parse(template.lvl2));
    if (targetLevel >= 3) result = result.add(parse(template.lvl3));
    if (targetLevel >= 4) result = result.add(parse(template.lvl4));
    return result;
  }

  public int getHpDelta() { return hpDelta; }
  public int getDamageDelta() { return damageDelta; }
  public int getCostDelta() { return costDelta; }
  public int getCooldownDeltaSeconds() { return cooldownDeltaSeconds; }
  public int getActionIntervalDeltaSeconds() { return actionIntervalDeltaSeconds; }
  public List<String> getRawEffects() { return rawEffects; }
  public int getAttackSpeedPercent() { return attackSpeedPercent; }
  public int getSunDelta() { return sunDelta; }
  public int getPierceDelta() { return pierceDelta; }
  public int getRangeDelta() { return rangeDelta; }
  public int getDurationDeltaSeconds() { return durationDeltaSeconds; }
  public int getFreezeTimeDeltaSeconds() { return freezeTimeDeltaSeconds; }
  public int getLifespanDeltaSeconds() { return lifespanDeltaSeconds; }
  public int getTargetsDelta() { return targetsDelta; }
}