package pvz.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.textures.TextureBank;

/**
 * A minimal playground to test the libpvz rendering.
 */
public class Demo extends ApplicationAdapter {
    private TextureBank textures;
    private PamPlayer player;
    private SpriteBatch batch;
    private OrthographicCamera camera;

    private String targetPam;
    // Clips are the names of little animation snippets in each PAM file. For example, a zombie
    // typically has an idle, a walk, an eating, and a dying clip.
    private List<String> clips;
    // ClipRefs are basically handles to the internal animation cache. By default, you would
    // give the PamPlayer a PAM filepath, a clip name, and a timestamp to draw a frame. But
    // this requires string hashing to find the corresponding animation for that PAM filepath
    // (which usually is a long string) every single frame. Instead, you can use refs to do the
    // hashing once (e.g., at load time), and then use that handle in O(1) time.
    private List<ClipRef> clipRefs = new ArrayList<>();
    
    // Visibility maps: enables you to show/hide parts. Some parts are hidden by default (for convenience),
    // and you must make them visible manually (like armor or butter).
    private Map<String, Boolean> visibilityMap = new HashMap<>();
    private Map<String, Boolean> visibilityMap2 = new HashMap<>();
    
    private float stateTime = 0f;
    private int currentClipIndex = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera(1280, 720);
        camera.position.set(0, 0, 0);
        camera.update();

        String rootPath = System.getProperty("pvz.assets");
        if (rootPath == null || rootPath.isEmpty()) {
            System.err.println("ERROR: You must specify the assets directory.");
            System.err.println("Pass -Dpvz.assets=/path/to/assets to Gradle, or set the PVZ_ASSETS environment variable.");
            System.exit(1);
        }


        // 1. Create the TextureBank and PamPlayer.
        FileHandle assetsFolder = Gdx.files.absolute(rootPath);
        textures = new TextureBank("768", assetsFolder);
        player = new PamPlayer(textures, assetsFolder);


        targetPam = System.getProperty("pvz.pam", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM");

        // 2. Get the clip labels of the PAM (the animation segments).
        clips = player.clips(targetPam);
        System.out.println(clips);

        // 3. (Optional, for optimization) Get the clip refs of the animation, so that instead of 
        //    hashing the path and clip strings each frame, we directly reference the animation.
        for (String clip : clips) {
            clipRefs.add(player.getClip(targetPam, clip));
        }

        // 3-1. (Optional) Create visibility maps to show or hide zombie parts.
        visibilityMap.put("_zombie_egypt_armor2_states", true);
        visibilityMap.put("zombie_armor_bucket_norm", true);

        visibilityMap2.put("butter", true);
    }

    @Override
    public void render() {
        stateTime += Gdx.graphics.getDeltaTime();
    
        ScreenUtils.clear(Color.DARK_GRAY);

        // 4. Do not forget to update the TextureBank! It is required by the internal asset manager it uses.
        textures.update();
        
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 5. Render the animation as you wish!
        player.draw(batch, clipRefs.get(currentClipIndex), stateTime, -200, 0, true);
        player.draw(batch, targetPam, clips.get(currentClipIndex), stateTime, 200, 0, true);

        // 6. Hide or show parts with visibility maps!
        player.draw(batch, clipRefs.get(currentClipIndex), stateTime, 0, -200, true, visibilityMap);
        player.draw(batch, clipRefs.get(currentClipIndex), stateTime, 0, 200, true, visibilityMap2);

        batch.end();

        if(stateTime >= clipRefs.get(currentClipIndex).duration) {
            stateTime = 0;
            currentClipIndex = (currentClipIndex + 1) % clips.size();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        textures.dispose();
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("libpvz Demo");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new Demo(), config);
    }
}
