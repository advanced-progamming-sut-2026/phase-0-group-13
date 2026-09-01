package view.gdx.render;

import com.badlogic.gdx.graphics.g2d.Batch;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import view.gdx.animation.EntityAnimation;

/**
 * The pieces that come off a zombie: its head, an arm, and the armour it was wearing.
 *
 * <p>Every rig already carries this artwork -- PopCap authored {@code particle_head} and {@code
 * particle_arm} as gibs, and each armour in its damage states -- and no clip the lawn asks for
 * ever draws any of it, so a zombie died in one piece and its bucket vanished with a puff. What
 * was missing was somewhere for a piece to live once it is no longer part of the body, which is
 * all this is: a short list of things falling under their own weight.
 *
 * <p>Positions are kept in lanes and columns rather than pixels, the same as {@link HitEffects},
 * so a piece in flight lands in the right place when the window is resized under it.
 */
final class DetachedParts {

  /** How long a piece is kept, counting the time it spends lying on the ground fading out. */
  static final float LIFE_SECONDS = 1.5f;

  private static final float FADE_SECONDS = 0.45f;

  /** Downwards pull, in lanes a second squared. Tuned by eye against the death clips. */
  private static final float GRAVITY_LANES = 13f;

  private static final float MIN_RISE = 2.3f;
  private static final float MAX_RISE = 3.3f;
  private static final float MIN_DRIFT = 0.35f;
  private static final float MAX_DRIFT = 1.15f;
  private static final float MIN_SPIN = 3.5f;
  private static final float MAX_SPIN = 9f;

  private final List<Piece> pieces = new ArrayList<>();
  private final Random random = new Random();

  /** One thing in the air, drawn from the rig it came off. */
  static final class Piece {

    private final EntityAnimation rig;
    private final String[] partNames;
    private final int row;
    private final float heightLanes;
    private final boolean flip;
    private final float spin;

    private double column;
    private float lift;
    private double drift;
    private float rise;
    private float angle;
    private float age;
    private boolean resting;

    private Piece(EntityAnimation rig, String[] partNames, double column, int row, float lift,
        float heightLanes, boolean flip, double drift, float rise, float spin) {
      this.rig = rig;
      this.partNames = partNames;
      this.column = column;
      this.row = row;
      this.lift = lift;
      this.heightLanes = heightLanes;
      this.flip = flip;
      this.drift = drift;
      this.rise = rise;
      this.spin = spin;
    }

    int row() {
      return row;
    }

    double column() {
      return column;
    }

    /** How far above the row's foot line the piece is now, in lanes. */
    float lift() {
      return lift;
    }

    float heightLanes() {
      return heightLanes;
    }

    /** Solid until its last moments, so a piece does not fade while it is still visibly moving. */
    float alpha() {
      float left = LIFE_SECONDS - age;
      return left >= FADE_SECONDS ? 1f : Math.max(0f, left / FADE_SECONDS);
    }

    boolean draw(Batch batch, float x, float y, float height) {
      return rig.drawLoosePart(batch, x, y, height, flip, angle, partNames);
    }

    private void advance(float delta) {
      age += delta;
      if (resting) {
        return;
      }
      rise -= GRAVITY_LANES * delta;
      lift += rise * delta;
      column += drift * delta;
      angle += spin * delta;
      if (lift <= 0f) {
        // Stops where it lands rather than bouncing: a bouncing head reads as a prop, and the
        // piece only has a few tenths of a second left to fade out anyway.
        lift = 0f;
        resting = true;
      }
    }
  }

  /**
   * Throws one piece off a zombie.
   *
   * @param lift how high up the piece starts, in lanes above the row's foot line
   * @param heightLanes how tall to draw it, as a share of a lane
   * @param flip true for a hypnotised zombie, which faces the other way
   * @param backwards true to send it back the way the zombie came, which is where a head goes
   * @param partNames the part to look for, most wanted first
   */
  void spawn(EntityAnimation rig, double column, int row, float lift, float heightLanes,
      boolean flip, boolean backwards, String... partNames) {
    if (rig == null || !rig.hasPart(partNames)) {
      return;
    }
    double drift = between(MIN_DRIFT, MAX_DRIFT) * (backwards ? 1 : -1);
    pieces.add(new Piece(rig, partNames, column, row, lift, heightLanes, flip,
        drift, between(MIN_RISE, MAX_RISE), between(MIN_SPIN, MAX_SPIN) * plusOrMinus()));
  }

  void advance(float delta) {
    for (int i = pieces.size() - 1; i >= 0; i--) {
      Piece piece = pieces.get(i);
      piece.advance(delta);
      if (piece.age >= LIFE_SECONDS) {
        pieces.remove(i);
      }
    }
  }

  List<Piece> all() {
    return pieces;
  }

  void clear() {
    pieces.clear();
  }

  private float between(float low, float high) {
    return low + random.nextFloat() * (high - low);
  }

  private int plusOrMinus() {
    return random.nextBoolean() ? 1 : -1;
  }
}
