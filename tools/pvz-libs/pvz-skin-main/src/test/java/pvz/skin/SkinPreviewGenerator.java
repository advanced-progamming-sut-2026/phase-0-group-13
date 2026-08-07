package pvz.skin;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class SkinPreviewGenerator extends ApplicationAdapter {
    private SpriteBatch batch;
    private Skin skin;
    private FileHandle outputDir;

    @Override
    public void create() {
        batch = new SpriteBatch();
        skin = PvzSkin.get();
        outputDir = Gdx.files.local("previews");
        outputDir.mkdirs();

        System.out.println("Generating skin component previews into: " + outputDir.file().getAbsolutePath());

        generateFontPreviews();
        generateTextButtonPreviews();
        generateImageButtonPreviews();
        generateLabelPreviews();
        generateProgressBarPreviews();
        generateCheckBoxPreviews();
        generateSliderPreviews();
        generateTenPatchPreviews();
        generateBorderedTablePreviews();

        System.out.println("Preview generation complete!");
        Gdx.app.exit();
    }

    private void generateFontPreviews() {
        String[] fontNames = {
            "ASHLEYSCRIPTMTSTD", "AVENIRNEXTLTPRO-DEMICN", "BRIANNETOD",
            "FBUSV8C5EI_1", "FBUSV8C5EI_1_outline", "FBUSV8C5EI_2",
            "FBUSV8C5EI_2_outline", "FBUSV8C6EI_3", "HOUSE_OF_TERROR", "PICO12"
        };

        for (String name : fontNames) {
            try {
                BitmapFont font = skin.getFont(name);
                if (font != null) {
                    renderFontToPNG(font, name, outputDir.child("font_" + name + ".png"));
                }
            } catch (Exception e) {
                System.err.println("Skipping font " + name + ": " + e.getMessage());
            }
        }
    }

    private void generateTextButtonPreviews() {
        String[] styles = {"default", "brown", "purple", "green", "green_small"};
        for (String style : styles) {
            try {
                TextButton button = new TextButton("Sample Button", skin, style);
                renderActorToPNG(button, (int) Math.max(button.getPrefWidth(), 160), (int) Math.max(button.getPrefHeight(), 60), outputDir.child("textbutton_" + style + ".png"));
            } catch (Exception e) {
                System.err.println("Skipping TextButton style " + style + ": " + e.getMessage());
            }
        }
    }

    private void generateImageButtonPreviews() {
        String[] styles = {
            "default", "ingame_pause", "ingame_2x", "generic_close_circle", "generic_close",
            "ingame_shovel", "hud_zg", "hud_quests", "hud_minigames", "settings",
            "plantfood", "next", "previous", "almanac"
        };

        for (String style : styles) {
            try {
                ImageButton button = new ImageButton(skin, style);
                renderActorToPNG(button, (int) Math.max(button.getPrefWidth(), 64), (int) Math.max(button.getPrefHeight(), 64), outputDir.child("imagebutton_" + style + ".png"));
            } catch (Exception e) {
                System.err.println("Skipping ImageButton style " + style + ": " + e.getMessage());
            }
        }
    }

    private void generateLabelPreviews() {
        String[] styles = {
            "default", "big", "secondary", "bundle_reward_multiplier", "medium",
            "promo_ribbon", "big_outline", "medium_outline"
        };

        for (String style : styles) {
            try {
                Label label = new Label("PvZ Label Preview", skin, style);
                renderActorToPNG(label, (int) Math.max(label.getPrefWidth() + 20, 200), (int) Math.max(label.getPrefHeight() + 20, 60), outputDir.child("label_" + style + ".png"));
            } catch (Exception e) {
                System.err.println("Skipping Label style " + style + ": " + e.getMessage());
            }
        }
    }

    private void generateProgressBarPreviews() {
        String[] styles = {"default-horizontal", "xp_fuschia", "xp_green", "xp_teal", "xp_yellow", "ingame_progress"};
        for (String style : styles) {
            try {
                ProgressBar pb = new ProgressBar(0, 100, 1, false, skin, style);
                pb.setValue(60);
                renderActorToPNG(pb, (int) Math.max(pb.getPrefWidth(), 200), (int) Math.max(pb.getPrefHeight(), 40), outputDir.child("progressbar_" + style + ".png"));
            } catch (Exception e) {
                System.err.println("Skipping ProgressBar style " + style + ": " + e.getMessage());
            }
        }
    }

    private void generateCheckBoxPreviews() {
        try {
            CheckBox cb = new CheckBox("Check Box", skin, "default");
            cb.setChecked(true);
            renderActorToPNG(cb, (int) Math.max(cb.getPrefWidth(), 150), (int) Math.max(cb.getPrefHeight(), 40), outputDir.child("checkbox_default.png"));
        } catch (Exception e) {
            System.err.println("Skipping CheckBox: " + e.getMessage());
        }
    }

    private void generateSliderPreviews() {
        try {
            Slider slider = new Slider(0, 100, 1, false, skin, "default-horizontal");
            slider.setValue(50);
            renderActorToPNG(slider, (int) Math.max(slider.getPrefWidth(), 200), (int) Math.max(slider.getPrefHeight(), 40), outputDir.child("slider_default.png"));
        } catch (Exception e) {
            System.err.println("Skipping Slider: " + e.getMessage());
        }
    }

    private void generateTenPatchPreviews() {
        String[] tenPatchNames = {
            "image_ui_generic_xp_progress_bar_10",
            "image_ui_generic_xp_progress_bar_fill_fuschia_10",
            "image_ui_generic_xp_progress_bar_fill_green_10",
            "image_ui_generic_xp_progress_bar_fill_teal_10",
            "image_ui_generic_xp_progress_bar_fill_yellow_10",
            "image_ui_hud_ingame_progress_meter_10",
            "image_ui_hud_ingame_progress_meter_fill_10",
            "image_ui_generic_bluebutton_10",
            "image_ui_generic_bluebutton_down_10",
            "image_ui_generic_brownbutton_10",
            "image_ui_generic_brownbutton_down_10",
            "image_ui_generic_disabledbutton_10",
            "image_ui_generic_disabledbutton_down_10",
            "image_ui_generic_purplebutton_10",
            "image_ui_generic_purplebutton_down_10",
            "image_ui_generic_greenbutton_10",
            "image_ui_generic_greenbutton_down_10",
            "image_ui_generic_greenbuybutton_10",
            "image_ui_generic_greenbuybutton_down_10",
            "image_ui_cards_almanac_plant_card_10",
            "image_ui_dialog_asset_dialogborder_10",
            "image_ui_dialog_asset_inner_bkgd_10",
            "image_ui_almanac_tabs_plants_active_10",
            "image_ui_almanac_general_scrollbar_10",
            "image_ui_almanac_general_scrollbar_bkgd_10",
            "image_ui_mainmenu_name_field_10",
            "image_ui_mainmenu_name_field_hover_10",
            "image_ui_quests_panel_edge_to_edge_ten",
            "image_ui_mainmenu_mm_settings_tab_10",
            "image_ui_if_bundle_reward_multiplier_bg_10",
            "image_ui_almanac_plants_plant_fuelbar_10",
            "image_ui_almanac_general_fuelbar_fill_10",
            "image_ui_if_bundle_reward1_bg_10",
            "image_ui_cards_store_promo_ribbon_10",
            "image_ui_powerups_powerup_cost_10",
            "image_ui_mainmenu_text_entry_field_10",
            "image_ui_generic_guaranteed_bg_10"
        };

        for (String name : tenPatchNames) {
            try {
                Drawable drawable = skin.getDrawable(name);
                if (drawable != null) {
                    Image image = new Image(drawable);
                    int w = (int) Math.max(image.getPrefWidth(), 100);
                    int h = (int) Math.max(image.getPrefHeight(), 50);
                    renderActorToPNG(image, w, h, outputDir.child("tenpatch_" + name + ".png"));
                }
            } catch (Exception e) {
                System.err.println("Skipping TenPatch drawable " + name + ": " + e.getMessage());
            }
        }
    }

    private void generateBorderedTablePreviews() {
        try {
            BorderedTable table = new BorderedTable();
            Label label = new Label("Bordered Table", skin, "medium");
            label.setColor(Color.BLACK);
            table.add(label);
            renderActorToPNG(table, 300, 150, outputDir.child("borderedtable.png"));
        } catch (Exception e) {
            System.err.println("Skipping BorderedTable: " + e.getMessage());
        }
    }

    private static Pixmap flipY(Pixmap src) {
        Pixmap flipped = new Pixmap(src.getWidth(), src.getHeight(), src.getFormat());
        int w = src.getWidth();
        int h = src.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                flipped.drawPixel(x, h - 1 - y, src.getPixel(x, y));
            }
        }
        return flipped;
    }

    private void renderFontToPNG(BitmapFont font, String text, FileHandle outputFile) {
        GlyphLayout layout = new GlyphLayout(font, text);
        int w = (int) Math.max(layout.width + 30, 200);
        int h = (int) Math.max(layout.height + 30, 50);

        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        OrthographicCamera cam = new OrthographicCamera(w, h);
        cam.position.set(w / 2f, h / 2f, 0);
        cam.update();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        font.draw(batch, layout, 15, h / 2f + layout.height / 2f);
        batch.end();

        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, w, h);
        fbo.end();
        fbo.dispose();

        Pixmap flipped = flipY(pixmap);
        PixmapIO.writePNG(outputFile, flipped);
        pixmap.dispose();
        flipped.dispose();
    }

    private void renderActorToPNG(Actor actor, int width, int height, FileHandle outputFile) {
        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        OrthographicCamera cam = new OrthographicCamera(width, height);
        cam.position.set(width / 2f, height / 2f, 0);
        cam.update();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        actor.setSize(width, height);
        actor.setPosition(0, 0);
        actor.draw(batch, 1f);
        batch.end();

        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
        fbo.end();
        fbo.dispose();

        Pixmap flipped = flipY(pixmap);
        PixmapIO.writePNG(outputFile, flipped);
        pixmap.dispose();
        flipped.dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("PvZ Skin Preview Generator");
        config.setWindowedMode(800, 600);
        config.setInitialVisible(false);
        new Lwjgl3Application(new SkinPreviewGenerator(), config);
    }
}
