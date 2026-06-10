package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ProfileMenuCommands implements Command {
    ChangeUserName("menu\\s+profile\\s+change-username\\s+-u\\s+(?<username>.+)"),
    ChangeNickName("menu\\s+profile\\s+change-nickname\\s+-u\\s+(?<nickname>.+)"),
    ChangeEmail("menu\\s+profile\\s+change-email\\s+-e\\s+(?<email>.+)"),
    ChangePassword("menu\\s+profile\\s+change-password\\s+-p\\s+(?<newPassword>\\S+)\\s+-o\\s+(?<oldPassword>.+)"),
    ShowInfo("menu\\s+profile\\s+show-info");

    private final String pattern;

    ProfileMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
