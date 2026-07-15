package data.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.game.zombie.ZombieParts.ZombieTemplate;


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
}