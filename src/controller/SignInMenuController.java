package controller;

import data.persistence.UserManager;
import java.util.regex.Matcher;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.SignInMenuCommands;
import model.enums.Menu;

public class SignInMenuController implements BaseController {

  @Override
  public void initController() {
    // System.out.println("Entered SignIn Menu.");
  }

  @Override
  public void handleinput(String input) {
    Matcher matcher;

    if ((matcher = SignInMenuCommands.Login.getMatcher(input)) != null) {
      handleLogin(matcher.group("username"), matcher.group("password"), matcher.group("stay"));
    } else if ((matcher = SignInMenuCommands.ForgetPassword.getMatcher(input)) != null) {
      handleForgetPassword(matcher.group("username"), matcher.group("email"));
    } else if ((matcher = SignInMenuCommands.Answer.getMatcher(input)) != null) {
      handleAnswer(matcher.group("answer"));
    } else if (input.startsWith("set new password ")) {
      String newPassword = input.replace("set new password ", "").trim();
      handleResetPassword(newPassword);
    } else if ((matcher = MenuCommands.ShowCurrentMenu.getMatcher(input)) != null) {
      System.out.println("Sign In Menu"); // اصلاح نام منو
    } else if ((matcher = MenuCommands.ExitMenu.getMatcher(input)) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private void handleLogin(String username, String password, String stay) {
    boolean stayLoggedIn = stay != null && !stay.trim().isEmpty();

    try {
      UserManager.getInstance().loginUser(username, password, stayLoggedIn);
      System.out.println("Login successful!");

      model.core.App.setCurrentMenu(model.enums.Menu.MainMenu);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void handleForgetPassword(String username, String email) {
    try {
      String question = UserManager.getInstance().initiatePasswordRecovery(username, email);
      System.out.println(question);
      System.out.println("Forget password initiated for " + username);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
  private void handleAnswer(String answer) {
    try {
      UserManager.getInstance().verifyRecoveryAnswer(answer);
      System.out.println("Checking security answer...");
      System.out.println("Please enter your new password using: 'set new password <password>'");
    } catch (Exception e) {
      System.out.println(e.getMessage());
      App.setCurrentMenu(Menu.SignInMenu);
    }
  }

  private void handleResetPassword(String newPassword) {
    try {
      UserManager.getInstance().resetPasswordAfterRecovery(newPassword);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  @Override
  public void exit() {
    System.out.println("Changed to SignUp Menu.");
    App.setCurrentMenu(Menu.SignUpMenu);
  }
}