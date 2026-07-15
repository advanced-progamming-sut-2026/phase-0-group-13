package model.enums;

public enum SecurityQuestion {
  Q1(1, "What is the name of your first pet?"),
  Q2(2, "What is your mother's maiden name?"),
  Q3(3, "What was the name of your first school?"),
  Q4(4, "What is your favorite plant?"),
  Q5(5, "In what city were you born?");

  private final int number;
  private final String text;

  SecurityQuestion(int number, String text) {
    this.number = number;
    this.text = text;
  }

  public int getNumber() {
    return number;
  }

  public String getText() {
    return text;
  }

  public static SecurityQuestion fromNumber(String numberStr) {
    if (numberStr == null) return null;
    try {
      int n = Integer.parseInt(numberStr.trim());
      for (SecurityQuestion q : values()) {
        if (q.number == n) return q;
      }
    } catch (NumberFormatException ignored) {
    }
    return null;
  }

  public static String listAll() {
    StringBuilder sb = new StringBuilder("Security questions:\n");
    for (SecurityQuestion q : values()) {
      sb.append(q.number).append(". ").append(q.text).append("\n");
    }
    return sb.toString().stripTrailing();
  }
}