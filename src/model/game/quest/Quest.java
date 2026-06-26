package model.game.quest;

public class Quest {
  public String title;
  private String category;
  private String condition;
  private String rewardType;
  private String priority;
  private String variable;

  private final Double progressOfQuest;

  public Quest() {
    this.progressOfQuest = 0.0;
  }

  public String getTitle() {
    return title;
  }

  public String getCategory() {
    return category;
  }

  public String getCondition() {
    return condition;
  }

  public String getRewardType() {
    return rewardType;
  }

  public String getPriority() {
    return priority;
  }

  public String getVariable() {
    return variable;
  }

  public void start() {}

  public void finish() {}
}
