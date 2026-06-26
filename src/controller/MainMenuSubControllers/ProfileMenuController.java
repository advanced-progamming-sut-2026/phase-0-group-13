package controller.MainMenuSubControllers;

import controller.BaseController;
import data.persistence.UserManager;
import java.util.regex.Matcher;
import model.account.User;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.ProfileMenuCommands;
import model.enums.Menu;

public class ProfileMenuController implements BaseController {

  @Override
  public void initController() {}

  @Override
  public void handleinput(String input) {
    Matcher matcher;

    if ((matcher = ProfileMenuCommands.ChangeUserName.getMatcher(input)) != null) {
      handleChangeUsername(matcher.group("username").trim());
    } else if ((matcher = ProfileMenuCommands.ChangeNickName.getMatcher(input)) != null) {
      handleChangeNickname(matcher.group("nickname").trim());
    } else if ((matcher = ProfileMenuCommands.ChangeEmail.getMatcher(input)) != null) {
      handleChangeEmail(matcher.group("email").trim());
    } else if ((matcher = ProfileMenuCommands.ChangePassword.getMatcher(input)) != null) {
      handleChangePassword(matcher.group("newPassword"), matcher.group("oldPassword").trim());
    } else if (ProfileMenuCommands.ShowInfo.getMatcher(input) != null) {
      handleShowInfo();
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(input) != null) {
      System.out.println("Profile Menu");
    } else if (MenuCommands.ExitMenu.getMatcher(input) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private void handleChangeUsername(String username) {
    try {
      UserManager.getInstance().changeUsername(username);
      System.out.println("Username changed to " + username + ".");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void handleChangeNickname(String nickname) {
    try {
      UserManager.getInstance().changeNickname(nickname);
      System.out.println("Nickname changed to " + nickname + ".");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void handleChangeEmail(String email) {
    try {
      UserManager.getInstance().changeEmail(email);
      System.out.println("Email changed to " + email + ".");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void handleChangePassword(String newPassword, String oldPassword) {
    try {
      UserManager.getInstance().changePassword(newPassword, oldPassword);
      System.out.println("Password changed successfully.");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void handleShowInfo() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      System.out.println("error: no user is currently logged in");
      return;
    }

    int clearedLevels =
        Math.max(
            0,
            (user.getProgress().getMaxClearedStage() - 1) * 4
                + user.getProgress().getMaxClearedLevel());

    System.out.println("Username: " + user.getUsername());
    System.out.println("Nickname: " + user.getNickname());
    System.out.println("Games played: " + user.getRecentGames().size());
    System.out.println("Coins: " + user.getCoins());
    System.out.println("Diamonds: " + user.getDiamonds());
    System.out.println("Levels cleared: " + clearedLevels);
    System.out.println("Highest myopoint: 0");
  }

  @Override
  public void exit() {
    System.out.println("Changed to Main Menu.");
    App.setCurrentMenu(Menu.MainMenu);
  }
}
