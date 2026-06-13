package model.account;

public class PlayerProfile {
    private String nickname;
    private String gender;
    private Progress progress;
    private Collection collection ;
    public PlayerProfile() {
    }

    public PlayerProfile(String nickname, String gender, Progress progress) {
        this.nickname = nickname;
        this.gender = gender;
        this.progress = progress;
    }

    public void showInfo() {
    }

    public void changeNickname() {
    }

    public void changeEmail() {
    }
}
