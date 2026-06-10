package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ShopMenuCommands {
    List("shop\\s+list"),
    Daily("shop\\s+daily"),
    Buy("shop\\s+buy\\s+-i\\s+(?<itemId>\\S+)\\s+-n\\s+(?<count>\\S+)(\\s+-t\\s+(?<plantType>.+))?");

    private final String pattern;

    ShopMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
