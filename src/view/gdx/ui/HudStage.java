package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import model.core.GameManager;
import model.game.plant.PlantParts.PlantTemplate;


/**
 * Scene2D layer for the in-match UI: sun counter, seed bar, plant food, wave banner, pause button.
 *
 * <p>Has its own ScreenViewport rather than sharing the world one. The world is letterboxed to a
 * fixed size so the lawn keeps its shape, but the HUD wants one unit per real pixel so the text
 * stays sharp at any window size.
 *
 * <p>What is filled in so far is the part the menus share: the coin and diamond readout, the debug
 * cheats and a way back out. It uses the same CurrencyHud and DebugPanel the menus do, so the
 * balances shown during a match are the same ones shown outside it. The match-specific widgets are
 * still to come.
 *
 * <p>build() does nothing while there's no skin, see UiSkinProvider.
 */
public final class HudStage implements Disposable {

  private final Stage stage;
  private Label statusLabel;
  private Label sunLabel;
  private Table seedBar;
  private final List<SeedCard> seedCards = new ArrayList<>();

  public HudStage() {
    this.stage = new Stage(new ScreenViewport());
  }

  /** The stage itself, for putting in an InputMultiplexer. */
  public Stage getStage() {
    return stage;
  }

  /**
   * Fills in the HUD. Does nothing without a skin, so a screen can just call it.
   *
   * @param onExit what the Menu button does, or null to leave it out
   */
  public void build(UiSkinProvider skinProvider, Runnable onExit) {
    if (!skinProvider.isAvailable()) {
      return;
    }
    Skin skin = skinProvider.get();

    // TODO seed bar, plant food slots and the pause button. The style names are all listed in
    // docs/phase2/pvz-skin-field-guide.html, e.g. ImageButton "ingame_pause".
    Table root = new Table();
    root.setFillParent(true);
    root.top().pad(16f);
    stage.addActor(root);

    root.add(new CurrencyHud(skin)).left().expandX();
    if (onExit != null) {
      root.add(exitButton(skin, onExit)).right();
    }
    root.row();

    if (DebugPanel.isEnabled()) {
      root.add(new DebugPanel(skin, message -> Toast.show(stage, skin, message)))
          .colspan(onExit != null ? 2 : 1)
          .right()
          .padTop(12f);
      root.row();
    }

    sunLabel = new Label("Sun: 0", skin, UiSkinProvider.LABEL_MEDIUM);
    statusLabel = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    root.add(sunLabel).left().padTop(10f);
    root.add(statusLabel).right().expandX().padTop(10f);
    root.row();

    // Docked in the top table for now, same as everything else here; moving it to the
    // bottom of the screen the way the real game does is a Phase-2 polish pass, not
    // needed just to have a working seed bar.
    seedBar = new Table();
    root.add(seedBar).colspan(2).left().padTop(14f);
  }

  private TextButton exitButton(Skin skin, Runnable onExit) {
    TextButton button = new TextButton("Menu", skin, UiSkinProvider.BUTTON_BROWN);
    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            onExit.run();
          }
        });
    return button;
  }

  /**
   * Fills the seed bar with one card per plant in the deck. Safe to call again (e.g. if the
   * deck can somehow change mid-match); it rebuilds from scratch each time.
   *
   * @param onPick called with the plant's template name when its card is clicked
   */
  public void buildSeedBar(Skin skin, List<PlantTemplate> templates, Consumer<String> onPick) {
    if (seedBar == null || skin == null || templates == null) {
      return;
    }
    seedBar.clearChildren();
    seedCards.clear();
    for (PlantTemplate template : templates) {
      TextButton button = new TextButton(
          template.name + "\n" + template.cost, skin, UiSkinProvider.BUTTON_BROWN);
      button.addListener(
          new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              onPick.accept(template.name);
            }
          });
      seedBar.add(button).size(96f, 64f).padRight(6f);
      seedCards.add(new SeedCard(template, button));
    }
  }

  /**
   * Refreshes each seed card: highlights the one currently selected for placing, and dims
   * (via alpha, not a skin-specific disabled style) any still on cooldown. Cheap enough to call
   * every frame -- it does no allocation beyond the per-card cooldown lookup.
   */
  public void updateSeeds(GameManager match, String selectedType, ToIntFunction<String> levelLookup) {
    if (match == null || seedCards.isEmpty()) {
      return;
    }
    for (SeedCard card : seedCards) {
      boolean selected = card.template.name.equalsIgnoreCase(selectedType);
      Color base = selected ? SELECTED_TINT : Color.WHITE;

      int level = levelLookup == null ? 1 : Math.max(1, levelLookup.applyAsInt(card.template.name));
      int recharge = adjustedRechargeSeconds(card.template, level);
      boolean onCooldown = match.ticksUntilPlantReady(card.template.name, recharge) > 0;
      card.button.setColor(base.r, base.g, base.b, onCooldown ? 0.5f : 1f);
    }
  }

  private static int adjustedRechargeSeconds(PlantTemplate template, int level) {
    model.game.plant.PlantParts.PlantLevel levelStats =
        model.game.plant.PlantParts.PlantLevel.cumulative(template, level);
    return Math.max(0, template.recharge + levelStats.getCooldownDeltaSeconds());
  }

  /** Free-text status line -- wave, plant food count, what's currently selected. */
  public void setStatus(String text) {
    if (statusLabel != null) {
      statusLabel.setText(text == null ? "" : text);
    }
  }

  public void setSun(int amount) {
    if (sunLabel != null) {
      sunLabel.setText("Sun: " + amount);
    }
  }

  private static final Color SELECTED_TINT = new Color(0.65f, 1f, 0.65f, 1f);

  /** One seed-bar card: the plant it represents and the button showing it. */
  private static final class SeedCard {
    private final PlantTemplate template;
    private final TextButton button;

    private SeedCard(PlantTemplate template, TextButton button) {
      this.template = template;
      this.button = button;
    }
  }

  public void act(float delta) {
    stage.act(delta);
  }

  public void draw() {
    stage.getViewport().apply();
    stage.draw();
  }

  public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
  }

  @Override
  public void dispose() {
    stage.dispose();
  }
}
