package view.gdx.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import view.gdx.animation.AnimationLibrary;
import view.gdx.animation.EntityAnimation;

/**
 * A Scene2D actor that plays one entity's idle animation.
 *
 * <p>The menus had no way to show a rig: {@link EntityAnimation} draws straight to a Batch at world
 * coordinates, which is what the lawn renderers want, while a menu needs something that sits in a
 * Table cell and takes its size from the layout. This wraps the one in the other, scaling the clip
 * to whatever cell it lands in and keeping it centred.
 *
 * <p>Falls back to drawing nothing when the entity has no rig, so a caller can stack it over a
 * static portrait and get the portrait for the handful that have no animation.
 */
public final class RigActor extends Actor {

  private final EntityAnimation animation;
  private final String clip;
  private float time;

  private RigActor(EntityAnimation animation, String clip) {
    this.animation = animation;
    this.clip = clip;
  }

  /**
   * An actor for this plant or zombie's idle loop, or null when it has no rig.
   *
   * @param kind {@link AnimationLibrary#PLANTS} or {@link AnimationLibrary#ZOMBIES}
   */
  public static RigActor idle(AnimationLibrary animations, String kind, String entityName) {
    if (animations == null || entityName == null) {
      return null;
    }
    EntityAnimation animation = animations.find(kind, entityName);
    if (animation == null) {
      return null;
    }
    // Same order the lawn asks in, so a menu shows the pose the board would.
    String clip = animation.pickClip("idle", "walk", "attack");
    if (clip == null || animation.height(clip) <= 0f) {
      return null;
    }
    return new RigActor(animation, clip);
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    time += delta;
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    float artHeight = animation.height(clip);
    float artWidth = animation.width(clip);
    if (artHeight <= 0f || artWidth <= 0f || getWidth() <= 0f || getHeight() <= 0f) {
      return;
    }
    // Fit inside the cell on both axes, so a wide rig is not clipped by a tall slot.
    float scale = Math.min(getHeight() / artHeight, getWidth() / artWidth);
    batch.setColor(getColor().r, getColor().g, getColor().b, getColor().a * parentAlpha);
    animation.draw(batch, clip, time,
        getX() + getWidth() / 2f,
        getY() + (getHeight() - artHeight * scale) / 2f,
        scale, false);
    batch.setColor(1f, 1f, 1f, 1f);
  }
}
