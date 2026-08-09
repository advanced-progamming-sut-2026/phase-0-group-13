package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import data.persistence.UserManager;
import model.account.User;


/**
 * Coin and diamond readout, shown in every graphical menu and in the match HUD.
 *
 * <p>Reads the logged-in User straight out of UserManager on every act(), so it always agrees with
 * the terminal version. There is no second copy of the balances anywhere in the graphical layer,
 * which also means a cheat or a reward shows up here on the next frame without anyone telling it.
 *
 * <p>Draws dashes when nobody is logged in, since the login and sign up screens carry the same
 * header.
 */
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

    // Also here, or the first frame of every screen shows dashes before the first act().
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
