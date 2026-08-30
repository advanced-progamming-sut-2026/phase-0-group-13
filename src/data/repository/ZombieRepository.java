package data.repository;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.enums.ZombieType;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;


public class ZombieRepository implements ReadOnlyRepository<Object> {

  private final List<ZombieTemplate> zombies = new ArrayList<>();
  private final Map<String, ZombieTemplate> armorDefsByAlias = new HashMap<>();

  public ZombieRepository(List<ZombieTemplate> rawEntries) {
    if (rawEntries == null) {
      return;
    }
    for (ZombieTemplate t : rawEntries) {
      if (t == null) {
        continue;
      }
      if (t.isArmorDefinition()) {
        armorDefsByAlias.put(t.getName(), t);
      } else {
        zombies.add(t);
      }
    }
  }

  public List<ZombieTemplate> getAll() {
    return zombies;
  }

  /**
   * فهرست دانشنامه: یک کارت به‌ازای هر نوع زامبی.
   *
   * <p>{@link #getAll()} is the raw sheet list, which is not what an almanac wants. It carries
   * scenery filed alongside the zombies (the arcade cabinet is a GridItem), several unused sheets
   * that fall through to the basic walker, and four upstream variants of each Zomboss -- so the
   * almanac showed "Basic" four times and Dr. Zomboss nine. One entry per {@link ZombieType},
   * first sheet in file order winning, which is the same one {@code Season.templateFor} spawns.
   */
  public List<ZombieTemplate> getAlmanacEntries() {
    List<ZombieTemplate> entries = new ArrayList<>();
    Set<ZombieType> seen = EnumSet.noneOf(ZombieType.class);
    for (ZombieTemplate template : zombies) {
      String alias = template.getName();
      if (alias == null || !alias.startsWith(ZOMBIE_SHEET_PREFIX)) {
        continue;
      }
      if (seen.add(ZombieTypeResolver.resolve(template))) {
        entries.add(template);
      }
    }
    return entries;
  }

  /** GridItem* entries share the file with the zombies but are scenery, not zombies. */
  private static final String ZOMBIE_SHEET_PREFIX = "Zombie";

  public ZombieTemplate find(String name) {
    if (name == null) {
      return null;
    }
    for (ZombieTemplate t : zombies) {
      if (t.getName() != null && t.getName().equalsIgnoreCase(name)) {
        return t;
      }
    }
    return null;
  }

  public List<Integer> resolveArmorHp(ZombieTemplate zombie) {
    List<Integer> result = new ArrayList<>();
    if (zombie == null) {
      return result;
    }
    for (String alias : zombie.getArmorRefAliases()) {
      ZombieTemplate armorDef = armorDefsByAlias.get(alias);
      if (armorDef != null && armorDef.objdata != null && armorDef.objdata.armorBaseHealth != null) {
        result.add(armorDef.objdata.armorBaseHealth);
      }
    }
    return result;
  }

  public List<model.enums.ArmorType> resolveArmorTypes(ZombieTemplate zombie) {
    List<model.enums.ArmorType> result = new ArrayList<>();
    if (zombie == null) {
      return result;
    }
    for (String alias : zombie.getArmorRefAliases()) {
      ZombieTemplate armorDef = armorDefsByAlias.get(alias);
      String rawType = (armorDef != null && armorDef.objdata != null) ? armorDef.objdata.armorType : null;
      result.add(model.enums.ArmorType.fromRawName(rawType));
    }
    return result;
  }
}