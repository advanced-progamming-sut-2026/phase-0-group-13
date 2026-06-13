package model.quest;

public class Quest {
    private String questName;
    private String questDescription;
    private Double ProgressOfQuest ;

    public Quest(String questName, String questDescription) {
        this.questName = questName;
        this.questDescription = questDescription;
        this.ProgressOfQuest = 0.0;
    }

    public Quest(String questName) {
        this.questName = questName;
    }

    public void start() {
    }

    public void finish() {
    }
}
