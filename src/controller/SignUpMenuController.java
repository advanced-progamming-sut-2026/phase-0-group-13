package controller;

import data.persistence.UserManager;
import java.util.regex.Matcher;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.SignUpMenuCommands;
import model.enums.Menu;
import model.enums.SecurityQuestion;

public class SignUpMenuController implements BaseController {
  @Override
  public void initController() {
    // System.out.println("Entered SignUp Menu");
  }

  @Override
  public void handleinput(String input) {
    Matcher matcher;

    if ((matcher = SignUpMenuCommands.Register.getMatcher(input)) != null) {
      handleRegister(matcher);
    } else if ((matcher = SignUpMenuCommands.PickQuestion.getMatcher(input)) != null) {
      handlePickQuestion(matcher);
    } else if ((matcher = MenuCommands.EnterMenu.getMatcher(input)) != null) {
      String targetMenu = matcher.group("menuName").trim().toLowerCase();
      handleEnterMenu(targetMenu);
    } else if ((matcher = MenuCommands.ShowCurrentMenu.getMatcher(input)) != null) {
      System.out.println("Sign Up Menu"); // اصلاح نام منو
    } else if ((matcher = MenuCommands.ExitMenu.getMatcher(input)) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private void handleRegister(Matcher matcher) {
    String username = matcher.group("username");
    String password = matcher.group("password");
    String passwordConfirm = matcher.group("passwordConfirm");
    String nickname = matcher.group("nickname");
    String email = matcher.group("email");
    String gender = matcher.group("gender");

    if (!password.equals(passwordConfirm)) {
      System.out.println("error: passwords do not match");
      return;
    }

    try {
      UserManager.getInstance().registerUser(username, password, nickname, email, gender);
      System.out.println("User data syntax is valid. Proceeding to pick a security question...");
      System.out.println(SecurityQuestion.listAll());
      System.out.println("pick question -q <question_number> -a <answer> -c <answer_confirm>");

    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void handlePickQuestion(Matcher matcher) {
    String qNumber = matcher.group("questionNumber");

    if (SecurityQuestion.fromNumber(qNumber) == null) {
      System.out.println("error: invalid question number");
      System.out.println(SecurityQuestion.listAll());
      return;
    }

    String answer = matcher.group("answer").replace("\"", "").trim();
    String answerConfirm = matcher.group("answerConfirm").replace("\"", "").trim();

    if (!answer.equals(answerConfirm)) {
      System.out.println("error: answers do not match");
      return;
    }

    try {
      UserManager.getInstance().setSecurityQuestionForLatestUser(qNumber, answer);
      System.out.println("Security question picked successfully.");
      System.out.println("Changed to sign in menu.");
      App.setCurrentMenu(Menu.SignInMenu);

    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void handleEnterMenu(String targetMenu) {
    if (!targetMenu.equals("signin menu")) {
      System.out.println("invalid menu!!!");
      return;
    }
    System.out.println("Changed to sign in menu.");
    App.setCurrentMenu(Menu.SignInMenu);
  }

  @Override
  public void exit() {
    System.out.println("Exiting application...");
    App.shouldExit = true;
  }
}