package view.gdx.animation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.NumberUtils;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * One entity's animation, baked from its PAM and drawn out of its own atlas.
 *
 * <p>A PAM is a tree: the main timeline attaches sub-sprites, those attach more sub-sprites, and
 * only the leaves are images. Walking that tree every frame would be wasteful, so the constructor
 * plays the whole timeline once and records each frame as a flat list of quads in PAM space. Draw
 * time is then a loop over one frame's quads.
 *
 * <p>Clip boundaries come from the PAM, but how fast a clip plays comes from the manifest: the
 * frame span is divided by the duration listed in {@code assets/animations}. Nothing about a
 * particular plant or zombie is known here, which is what makes the same class serve idle, walk,
 * eat and everything the later phases add.
 *
 * <p>Parts whose name marks them optional -- armour, butter, ink, the ground swatch -- are hidden
 * unless a visibility map turns them on, the same default the art was authored with.
 */
public final class EntityAnimation {

  /** Frames per second to fall back on when a clip is not in the manifest. */
  private static final float DEFAULT_FPS = 30f;

  private final Frame[] frames;
  private final Part[] parts;
  private final Part root;
  private final Map<String, Clip> clips = new LinkedHashMap<>();
  private final float[] vertices = new float[20];
  private final float[] topBox = new float[4];
  private final Color scratch = new Color();
  private boolean[] defaultVisible;

  EntityAnimation(PamFile pam, TextureAtlas atlas, Map<String, Float> durations) {
    Bake bake = new Bake(pam, atlas);
    this.frames = bake.frames;
    this.parts = bake.parts;
    this.root = bake.root;
    float fileFps = pam.mainSprite != null && pam.mainSprite.frameRate > 0
        ? pam.mainSprite.frameRate
        : Math.max(1f, pam.frameRate);
    for (Map.Entry<String, int[]> range : bake.ranges.entrySet()) {
      int span = range.getValue()[1] - range.getValue()[0] + 1;
      Float listed = durations.get(range.getKey());
      float fps = listed != null && listed > 0f ? span / listed : fileFps;
      clips.put(range.getKey(), new Clip(range.getValue()[0], range.getValue()[1],
          fps > 0f ? fps : DEFAULT_FPS));
    }
  }

  boolean isUsable() {
    return frames.length > 0 && !clips.isEmpty();
  }

  /**
   * The first of these clips this entity actually has, or null if it has none of them.
   *
   * <p>Names are not consistent across rigs: Peashooter's idle is "idle", Puff-shroom's is
   * "idle_stage1" and Doom-shroom's is "stage1_idle". An exact name wins, then the shortest one
   * that starts with it, then the shortest one that merely contains it, so a caller asking for
   * "idle" gets the plain loop when there is one and the plant's own variant when there is not.
   */
  public String pickClip(String... preferred) {
    for (String clip : preferred) {
      if (clips.containsKey(clip)) {
        return clip;
      }
    }
    for (String clip : preferred) {
      String prefixed = nearest(clip, true);
      if (prefixed != null) {
        return prefixed;
      }
    }
    for (String clip : preferred) {
      String contained = nearest(clip, false);
      if (contained != null) {
        return contained;
      }
    }
    return null;
  }

  private String nearest(String wanted, boolean atStart) {
    String best = null;
    for (String name : clips.keySet()) {
      boolean matches = atStart ? name.startsWith(wanted) : name.contains(wanted);
      if (matches && (best == null || name.length() < best.length())) {
        best = name;
      }
    }
    return best;
  }

  /**
   * Every part name in this rig, for a caller that wants to build a visibility map.
   *
   * <p>Armour, butter and ink are authored as parts of the body rig rather than as separate
   * entities, and {@link Part#isOptional} hides them by default. A caller that knows a zombie is
   * still wearing its cone needs the rig's own name for that cone to switch it back on, and the
   * names differ between rigs, so they are read off the rig instead of being listed anywhere.
   */
  public Set<String> partNames() {
    Set<String> names = new LinkedHashSet<>();
    for (Part part : parts) {
      if (part.name != null && !part.name.isEmpty()) {
        names.add(part.name);
      }
    }
    return names;
  }

  /**
   * How long one pass of a clip takes, in seconds, or 0 if this rig has no such clip.
   *
   * <p>For a caller that wants to hold a one-shot pose for exactly as long as it runs -- an attack,
   * say -- rather than guessing a duration that is the same for every rig. The number is the clip's
   * own frame span over its own frame rate, which is what {@link #draw} steps through.
   */
  public float duration(String clip) {
    Clip found = clips.get(clip);
    if (found == null || found.fps <= 0f) {
      return 0f;
    }
    return (found.end - found.start + 1) / found.fps;
  }

  /** Height of the drawn art in PAM units, for sizing the sprite against a lane. */
  public float height(String clip) {
    Bounds bounds = boundsOf(clip);
    return bounds == null ? 0f : bounds.maxY - bounds.minY;
  }

  public float width(String clip) {
    Bounds bounds = boundsOf(clip);
    return bounds == null ? 0f : bounds.maxX - bounds.minX;
  }

  public void draw(Batch batch, String clip, float time, float x, float y, float scale,
      boolean flip) {
    draw(batch, clip, time, x, y, scale, flip, null);
  }

  /**
   * Draws one frame of a clip standing on {@code (x, y)}.
   *
   * @param x where the middle of the art goes
   * @param y where the bottom of the art goes
   * @param flip true to face the other way, for a hypnotised zombie
   * @param visibility parts to force on or off by name, or null for the authored defaults
   */
  public void draw(Batch batch, String clip, float time, float x, float y, float scale,
      boolean flip, Map<String, Boolean> visibility) {
    Clip found = clips.get(clip);
    Bounds bounds = boundsOf(clip);
    if (found == null || bounds == null) {
      return;
    }
    int span = found.end - found.start + 1;
    int step = (int) (Math.max(0f, time) * found.fps);
    Frame frame = frames[found.start + step % span];

    boolean[] visible = visibleParts(visibility);
    // PAM space has y going down from the top left, libGDX has it going up, hence the -scale.
    float sx = flip ? -scale : scale;
    float anchorX = (bounds.minX + bounds.maxX) / 2f;
    float originX = x - sx * anchorX;
    float originY = y + scale * bounds.maxY;

    Color batchColor = batch.getColor();
    boolean plain = batchColor.r == 1f && batchColor.g == 1f && batchColor.b == 1f
        && batchColor.a == 1f;
    for (int i = 0; i < frame.count; i++) {
      Part part = parts[frame.partIds[i]];
      if (!visible[part.id] || part.texture == null) {
        continue;
      }
      float packed = plain ? frame.colors[i] : tint(frame.colors[i], batchColor);
      int corner = i * 8;
      float u = part.u;
      float v = part.v;
      float u2 = part.u2;
      float v2 = part.v2;
      set(vertices, Batch.X1, originX + sx * frame.corners[corner],
          originY - scale * frame.corners[corner + 1], packed, u, v);
      set(vertices, Batch.X2, originX + sx * frame.corners[corner + 2],
          originY - scale * frame.corners[corner + 3], packed, u, v2);
      set(vertices, Batch.X3, originX + sx * frame.corners[corner + 4],
          originY - scale * frame.corners[corner + 5], packed, u2, v2);
      set(vertices, Batch.X4, originX + sx * frame.corners[corner + 6],
          originY - scale * frame.corners[corner + 7], packed, u2, v);
      batch.draw(part.texture, vertices, 0, 20);
    }
  }

  /**
   * Where the topmost part of the current frame sits on screen, as
   * {@code {centreX, centreY, width, height}}, or null if the clip has no drawable frame.
   *
   * <p>On a walker rig the highest part is the head, so this is what a caller overlays something
   * onto. Uses the same transform as {@link #draw}, so it tracks the head through the walk cycle
   * instead of pinning it to the sprite box. The array is reused between calls.
   */
  public float[] topPartBox(String clip, float time, float x, float y, float scale, boolean flip) {
    Clip found = clips.get(clip);
    Bounds bounds = boundsOf(clip);
    if (found == null || bounds == null) {
      return null;
    }
    int span = found.end - found.start + 1;
    int step = (int) (Math.max(0f, time) * found.fps);
    Frame frame = frames[found.start + step % span];

    boolean[] visible = visibleParts(null);
    float sx = flip ? -scale : scale;
    float originX = x - sx * ((bounds.minX + bounds.maxX) / 2f);
    float originY = y + scale * bounds.maxY;

    float highest = -Float.MAX_VALUE;
    boolean any = false;
    for (int i = 0; i < frame.count; i++) {
      Part part = parts[frame.partIds[i]];
      if (!visible[part.id] || part.texture == null) {
        continue;
      }
      int corner = i * 8;
      float minX = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      for (int c = 0; c < 8; c += 2) {
        float wx = originX + sx * frame.corners[corner + c];
        float wy = originY - scale * frame.corners[corner + c + 1];
        minX = Math.min(minX, wx);
        maxX = Math.max(maxX, wx);
        minY = Math.min(minY, wy);
        maxY = Math.max(maxY, wy);
      }
      if (maxY > highest) {
        highest = maxY;
        any = true;
        topBox[0] = (minX + maxX) / 2f;
        topBox[1] = (minY + maxY) / 2f;
        topBox[2] = maxX - minX;
        topBox[3] = maxY - minY;
      }
    }
    return any ? topBox : null;
  }

  private static void set(float[] vertices, int offset, float x, float y, float color, float u,
      float v) {
    vertices[offset] = x;
    vertices[offset + 1] = y;
    vertices[offset + 2] = color;
    vertices[offset + 3] = u;
    vertices[offset + 4] = v;
  }

  private float tint(float packed, Color batchColor) {
    int bits = NumberUtils.floatToIntColor(packed);
    scratch.set((bits & 0xff) / 255f * batchColor.r, ((bits >>> 8) & 0xff) / 255f * batchColor.g,
        ((bits >>> 16) & 0xff) / 255f * batchColor.b, ((bits >>> 24) & 0xff) / 255f * batchColor.a);
    return scratch.toFloatBits();
  }

  /** Walks the part tree so hiding a parent hides everything hanging off it. */
  private boolean[] visibleParts(Map<String, Boolean> visibility) {
    if (visibility == null && defaultVisible != null) {
      return defaultVisible;
    }
    boolean[] visible = new boolean[parts.length];
    List<Part> queue = new ArrayList<>();
    queue.add(root);
    for (int i = 0; i < queue.size(); i++) {
      Part part = queue.get(i);
      Boolean forced = visibility == null || part.name == null ? null : visibility.get(part.name);
      if (Boolean.FALSE.equals(forced) || (forced == null && part.optional)) {
        continue;
      }
      visible[part.id] = true;
      queue.addAll(part.children);
    }
    if (visibility == null) {
      defaultVisible = visible;
    }
    return visible;
  }

  private Bounds boundsOf(String clip) {
    Clip found = clips.get(clip);
    if (found == null) {
      return null;
    }
    if (found.bounds == null) {
      found.bounds = measure(found);
    }
    return found.bounds;
  }

  /** Only the parts that are actually drawn count, or a hidden shadow would offset every plant. */
  private Bounds measure(Clip clip) {
    boolean[] visible = visibleParts(null);
    Bounds bounds = new Bounds();
    for (int f = clip.start; f <= clip.end && f < frames.length; f++) {
      Frame frame = frames[f];
      for (int i = 0; i < frame.count; i++) {
        Part part = parts[frame.partIds[i]];
        if (!visible[part.id] || part.texture == null) {
          continue;
        }
        for (int corner = i * 8; corner < i * 8 + 8; corner += 2) {
          bounds.add(frame.corners[corner], frame.corners[corner + 1]);
        }
      }
    }
    return bounds.isEmpty() ? null : bounds;
  }

  private static final class Clip {
    final int start;
    final int end;
    final float fps;
    Bounds bounds;

    Clip(int start, int end, float fps) {
      this.start = start;
      this.end = end;
      this.fps = fps;
    }
  }

  private static final class Bounds {
    float minX = Float.MAX_VALUE;
    float minY = Float.MAX_VALUE;
    float maxX = -Float.MAX_VALUE;
    float maxY = -Float.MAX_VALUE;

    void add(float x, float y) {
      minX = Math.min(minX, x);
      minY = Math.min(minY, y);
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
    }

    boolean isEmpty() {
      return minX > maxX || minY > maxY;
    }
  }

  /** A node of the PAM tree, held flat. Its region never changes once the timeline is baked. */
  private static final class Part {
    final int id;
    final String name;
    final Part parent;
    final boolean optional;
    final List<Part> children = new ArrayList<>();
    String leaf;
    Texture texture;
    float u;
    float v;
    float u2;
    float v2;

    Part(int id, String name, Part parent) {
      this.id = id;
      this.name = name;
      this.parent = parent;
      this.optional = isOptional(name);
      if (parent != null) {
        parent.children.add(this);
      }
    }

    private static boolean isOptional(String name) {
      return name != null && (name.equals("butter") || name.equals("ink")
          || name.contains("armor") || name.contains("custom")
          || name.startsWith("ground_swatch") || name.contains("arm_outer_upper_bone"));
    }
  }

  /** One frame of the main timeline, flattened to quads. */
  private static final class Frame {
    final int count;
    final float[] corners;
    final float[] colors;
    final int[] partIds;

    Frame(int count, float[] corners, float[] colors, int[] partIds) {
      this.count = count;
      this.corners = corners;
      this.colors = colors;
      this.partIds = partIds;
    }
  }

  /** Plays the PAM timeline once and records what every frame draws. */
  private static final class Bake {

    private final PamFile pam;
    private final TextureAtlas atlas;
    private final List<Part> built = new ArrayList<>();
    private final Map<String, int[]> ranges = new LinkedHashMap<>();
    private final Map<PamFile.Image, float[]> sizes = new IdentityHashMap<>();

    private Part root;
    private Part[] parts;
    private Frame[] frames;

    Bake(PamFile pam, TextureAtlas atlas) {
      this.pam = pam;
      this.atlas = atlas;
      PamFile.Sprite main = pam.mainSprite;
      String rootName = main != null && main.name != null && !main.name.isEmpty()
          ? main.name
          : "root";
      this.root = part(rootName, null);
      readRanges(main);
      this.frames = run(main);
      this.parts = built.toArray(new Part[0]);
      bindRegions();
    }

    private void readRanges(PamFile.Sprite main) {
      if (main == null) {
        return;
      }
      String open = null;
      int openedAt = 0;
      for (int i = 0; i < main.frames.size(); i++) {
        String clip = main.frames.get(i).clip;
        if (clip == null || clip.isEmpty()) {
          continue;
        }
        if (open != null) {
          ranges.putIfAbsent(open, new int[] {openedAt, i - 1});
        }
        open = clip;
        openedAt = i;
      }
      if (open != null) {
        ranges.putIfAbsent(open, new int[] {openedAt, main.frames.size() - 1});
      }
    }

    private Frame[] run(PamFile.Sprite main) {
      if (main == null || main.frames.isEmpty()) {
        return new Frame[0];
      }
      Timeline timeline = new Timeline(main);
      Frame[] out = new Frame[main.frames.size()];
      for (int i = 0; i < out.length; i++) {
        apply(timeline, main.frames.get(i));
        advance(timeline);
        out[i] = flatten(timeline);
      }
      return out;
    }

    private void apply(Timeline timeline, PamFile.Frame frame) {
      for (Integer index : frame.removes) {
        timeline.slots.remove(index);
      }
      for (PamFile.Add add : frame.appends) {
        timeline.slots.put(add.index, attach(add));
      }
      for (PamFile.Move move : frame.changes) {
        Slot slot = timeline.slots.get(move.index);
        if (slot == null) {
          continue;
        }
        slot.transform = matrix(move.transform);
        if (move.color != null) {
          slot.color = new Color(move.color[0], move.color[1], move.color[2], move.color[3]);
        }
        if (slot.child != null && move.animFrameNum != 0) {
          slot.child.at = slot.child.wrap(move.animFrameNum);
          slot.child.stopped = false;
        }
      }
    }

    private Slot attach(PamFile.Add add) {
      Slot slot = new Slot();
      slot.timescale = add.timescale <= 0f ? 1f : add.timescale;
      slot.name = add.name;
      if (add.sprite) {
        if (add.resource >= 0 && add.resource < pam.sprites.size()) {
          PamFile.Sprite sprite = pam.sprites.get(add.resource);
          slot.child = new Timeline(sprite);
          if (!sprite.frames.isEmpty()) {
            apply(slot.child, sprite.frames.get(slot.child.wrap(slot.child.at)));
          }
          if (slot.name == null) {
            slot.name = sprite.name;
          }
        }
      } else if (add.resource >= 0 && add.resource < pam.images.size()) {
        slot.image = pam.images.get(add.resource);
        if (slot.name == null) {
          slot.name = slot.image.leaf;
        }
      }
      return slot;
    }

    private void advance(Timeline timeline) {
      for (Slot slot : timeline.slots.values()) {
        if (slot.child == null) {
          continue;
        }
        slot.pending += Math.max(0f, slot.timescale);
        int guard = 0;
        while (slot.pending >= 1f && guard++ < 1024) {
          slot.pending -= 1f;
          step(slot.child);
        }
      }
    }

    private void step(Timeline timeline) {
      if (timeline.sprite.frames.isEmpty() || timeline.stopped) {
        return;
      }
      PamFile.Frame frame = timeline.sprite.frames.get(timeline.wrap(timeline.at));
      apply(timeline, frame);
      advance(timeline);
      if (frame.stop) {
        timeline.stopped = true;
      } else {
        timeline.at = timeline.next();
      }
    }

    private Frame flatten(Timeline timeline) {
      List<float[]> corners = new ArrayList<>();
      List<Float> colors = new ArrayList<>();
      List<Integer> ids = new ArrayList<>();
      collect(timeline, IDENTITY, Color.WHITE, root, corners, colors, ids);
      int count = ids.size();
      float[] flatCorners = new float[count * 8];
      float[] flatColors = new float[count];
      int[] flatIds = new int[count];
      for (int i = 0; i < count; i++) {
        System.arraycopy(corners.get(i), 0, flatCorners, i * 8, 8);
        flatColors[i] = colors.get(i);
        flatIds[i] = ids.get(i);
      }
      return new Frame(count, flatCorners, flatColors, flatIds);
    }

    private void collect(Timeline timeline, float[] parent, Color inherited, Part owner,
        List<float[]> corners, List<Float> colors, List<Integer> ids) {
      for (Slot slot : timeline.slots.values()) {
        float[] world = multiply(parent, slot.transform);
        Color color = slot.color != null ? slot.color : inherited;
        Part part = part(slot.name, owner);
        if (slot.child != null) {
          collect(slot.child, world, color, part, corners, colors, ids);
        } else if (slot.image != null) {
          if (part.leaf == null) {
            part.leaf = slot.image.leaf;
          }
          corners.add(quad(multiply(world, slot.image.transform), sizeOf(slot.image)));
          colors.add(color.toFloatBits());
          ids.add(part.id);
        }
      }
    }

    /** Older PAMs leave the part size out; the atlas region it points at is the same thing. */
    private float[] sizeOf(PamFile.Image image) {
      float[] cached = sizes.get(image);
      if (cached != null) {
        return cached;
      }
      float w = image.size != null && image.size[0] > 0 ? image.size[0] : 0f;
      float h = image.size != null && image.size[1] > 0 ? image.size[1] : 0f;
      if (w <= 0f || h <= 0f) {
        TextureRegion region = atlas.findRegion(image.leaf);
        w = region == null ? 1f : region.getRegionWidth();
        h = region == null ? 1f : region.getRegionHeight();
      }
      cached = new float[] {w, h};
      sizes.put(image, cached);
      return cached;
    }

    private static float[] quad(float[] m, float[] size) {
      float w = size[0];
      float h = size[1];
      return new float[] {
          m[0] * 0f + m[2] * 0f + m[4], m[1] * 0f + m[3] * 0f + m[5],
          m[0] * 0f + m[2] * h + m[4], m[1] * 0f + m[3] * h + m[5],
          m[0] * w + m[2] * h + m[4], m[1] * w + m[3] * h + m[5],
          m[0] * w + m[2] * 0f + m[4], m[1] * w + m[3] * 0f + m[5]};
    }

    private Part part(String name, Part parent) {
      String key = name == null ? "" : name;
      for (Part existing : built) {
        if (existing.parent == parent && existing.name.equals(key)) {
          return existing;
        }
      }
      Part created = new Part(built.size(), key, parent);
      built.add(created);
      return created;
    }

    private void bindRegions() {
      for (Part part : parts) {
        if (part.leaf == null) {
          continue;
        }
        TextureRegion region = atlas.findRegion(part.leaf);
        if (region == null) {
          continue;
        }
        part.texture = region.getTexture();
        part.u = region.getU();
        part.v = region.getV();
        part.u2 = region.getU2();
        part.v2 = region.getV2();
      }
    }
  }

  private static final float[] IDENTITY = {1f, 0f, 0f, 1f, 0f, 0f};

  /** Affine transforms packed as {a, b, c, d, tx, ty}, the order the PAM stores them in. */
  private static float[] matrix(float[] packed) {
    if (packed == null || packed.length == 0) {
      return IDENTITY.clone();
    }
    if (packed.length >= 6) {
      return packed.clone();
    }
    if (packed.length == 3) {
      float cos = (float) Math.cos(packed[0]);
      float sin = (float) Math.sin(packed[0]);
      return new float[] {cos, sin, -sin, cos, packed[1], packed[2]};
    }
    return new float[] {1f, 0f, 0f, 1f, packed[0], packed[1]};
  }

  private static float[] multiply(float[] left, float[] right) {
    return new float[] {
        left[0] * right[0] + left[2] * right[1],
        left[1] * right[0] + left[3] * right[1],
        left[0] * right[2] + left[2] * right[3],
        left[1] * right[2] + left[3] * right[3],
        left[0] * right[4] + left[2] * right[5] + left[4],
        left[1] * right[4] + left[3] * right[5] + left[5]};
  }

  /** Playback position of one sprite while the timeline is being baked. */
  private static final class Timeline {
    final PamFile.Sprite sprite;
    final TreeMap<Integer, Slot> slots = new TreeMap<>();
    int at;
    boolean stopped;

    Timeline(PamFile.Sprite sprite) {
      this.sprite = sprite;
    }

    int wrap(int frame) {
      return frame < 0 || frame > last() ? 0 : frame;
    }

    int next() {
      return at + 1 > last() ? 0 : at + 1;
    }

    private int last() {
      return sprite.frames.isEmpty() ? 0 : sprite.frames.size() - 1;
    }
  }

  /** One attached image or sub-sprite, at whatever transform the last move gave it. */
  private static final class Slot {
    String name;
    PamFile.Image image;
    Timeline child;
    float[] transform = IDENTITY.clone();
    Color color;
    float timescale = 1f;
    float pending;
  }
}
