package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import data.persistence.UserManager;
import model.enums.SecurityQuestion;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


public final class SignUpScreen extends MenuScreen {

  private static final String[] GENDERS = {"male", "female"};

  private TextField username;
  private TextField password;
  private TextField passwordConfirm;
  private TextField nickname;
  private TextField email;
  private SelectBox<String> gender;
  private SelectBox<String> question;
  private TextField answer;
  private TextField answerConfirm;

  public SignUpScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Sign Up";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  protected void buildContent(Table content) {
    Table form = panel();
    username = field(form, "Username", false);
    password = field(form, "Password", true);
    passwordConfirm = field(form, "Confirm Password", true);
    nickname = field(form, "Nickname", false);
    email = field(form, "Email", false);

    gender = select(form, "Gender", GENDERS);
    question = select(form, "Security Question", questionChoices());
    answer = field(form, "Answer", false);
    answerConfirm = field(form, "Confirm Answer", false);

    Table actions = new Table();
    actions.defaults().pad(6f).width(200f);
    actions.add(button("Register", UiSkinProvider.BUTTON_GREEN, this::register));
    actions.add(button("Login", UiSkinProvider.BUTTON_BROWN, () -> go(new LoginScreen(game))));
    actions.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    form.add(actions).colspan(2).padTop(14f).row();

    // Nine rows do not fit a short window, so the form scrolls rather than being clipped.
    ScrollPane scroll = new ScrollPane(form, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    content.add(scroll).grow();
  }

  private SelectBox<String> select(Table form, String label, String[] items) {
    SelectBox<String> box = new SelectBox<>(skin);
    box.setItems(items);
    form.add(new Label(label, skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    form.add(box).width(340f).height(52f).row();
    return box;
  }

  private static String[] questionChoices() {
    SecurityQuestion[] questions = SecurityQuestion.values();
    String[] choices = new String[questions.length];
    for (int i = 0; i < questions.length; i++) {
      choices[i] = questions[i].getNumber() + ". " + questions[i].getText();
    }
    return choices;
  }

  private void register() {
    if (!password.getText().equals(passwordConfirm.getText())) {
      toast("error: passwords do not match");
      return;
    }
    String securityAnswer = answer.getText().trim();
    if (!securityAnswer.equals(answerConfirm.getText().trim())) {
      toast("error: answers do not match");
      return;
    }

    String user = username.getText().trim();
    String pass = password.getText();
    String nick = nickname.getText().trim();
    String mail = email.getText().trim();
    String genderChoice = gender.getSelected();
    String questionNumber = selectedQuestionNumber();
    runAsync(
        () -> {
          UserManager manager = UserManager.getInstance();
          manager.registerUser(user, pass, nick, mail, genderChoice);
          manager.setSecurityQuestionForLatestUser(questionNumber, securityAnswer);
          return null;
        },
        ignored -> go(new LoginScreen(game).withNotice("Account created. You can log in now.")),
        e -> toast(e.getMessage()));
  }

  private String selectedQuestionNumber() {
    int index = Math.max(0, question.getSelectedIndex());
    return String.valueOf(SecurityQuestion.values()[index].getNumber());
  }
}
