package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SignInMenuCommands implements Command {
    Login("login\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)(?<stay>\\s+-stay-logged-in)?"),    ForgetPassword("forget\\s+password\\s+-u\\s+(?<username>\\S+)\\s+-e\\s+(?<email>.+)"),
    Answer("answer\\s+-a\\s+(?<answer>.+)");

    private final String pattern;

    SignInMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
