package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import data.persistence.UserManager;
import model.account.User;


public final class CurrencyHud extends Table {

  private static final String NO_USER = "-";

  private final Label coins;
  private final Label diamonds;

  public CurrencyHud(Skin skin) {
    this.coins = new Label(NO_USER, skin, UiSkinProvider.LABEL_MEDIUM);
    this.diamonds = new Label(NO_USER, skin, UiSkinProvider.LABEL_MEDIUM);

    add(new Image(skin.getDrawable(UiSkinProvider.COIN_ICON))).size(34f).padRight(6f);
    add(coins).padRight(22f);
    add(new Image(skin.getDrawable(UiSkinProvider.GEM_ICON))).size(34f).padRight(6f);
    add(diamonds);

    refresh();
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    refresh();
  }

  private void refresh() {
    User user = UserManager.getInstance().getCurrentUser();
    coins.setText(user == null ? NO_USER : String.valueOf(user.getCoins()));
    diamonds.setText(user == null ? NO_USER : String.valueOf(user.getDiamonds()));
  }
}
