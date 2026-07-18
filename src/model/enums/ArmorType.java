package model.enums;

public enum ArmorType {
  CONE,
  BUCKET,
  HELMET,
  SHOULDER_ARMOR,
  BLOCK,
  NEWSPAPER,
  BARREL,
  PIANO;
  
  public static ArmorType fromRawName(String rawArmorType) {
    if (rawArmorType == null) {
      return null;
    }
    switch (rawArmorType.trim().toLowerCase()) {
      case "cone":
        return CONE;
      case "bucket":
        return BUCKET;
      case "crown":
      case "helmet":
        return HELMET;
      case "shoulderarmor":
      case "shoulder_armor":
        return SHOULDER_ARMOR;
      case "brick":
      case "block":
        return BLOCK;
      case "newspaper":
        return NEWSPAPER;
      case "barrel":
        return BARREL;
      case "piano":
        return PIANO;
      default:
        return null;
    }
  }
}