package view.gdx.animation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The rigs carry parts they do not mean to show, and the match for them has to be narrow.
 *
 * <p>"item" as a plain substring also catches the Grape Shot's whiteMelonBody, which is the plant
 * itself; hiding that leaves a Grape Shot with no grapes. The suffix is anchored instead.
 */
class StrayPartTest {

  private static boolean hidden(String part) {
    return EntityAnimation.isStrayPart(part);
  }

  @Test
  void anEmptyItemSlotIsHidden() {
    assertTrue(hidden("Magnet_Item"), "Magnet-shroom's item slot draws as a purple slab");
  }

  @Test
  void aPartThatMerelySpellsItemInsideAWordIsKept() {
    assertFalse(hidden("whiteMelonBody"), "this is the Grape Shot's own body");
    assertFalse(hidden("item_holder_visible"), "only a trailing _Item is a slot");
  }

  @Test
  void theArtistsLeftoverFolderIsHidden() {
    assertTrue(hidden("Duplicate Items Folder/sunshroom_eye_highlight copy 3"));
    assertTrue(hidden("Duplicate Items Folder/bounding_box copy"));
  }

  @Test
  void theOrdinaryHiddenPartsStillAre() {
    assertTrue(hidden("butter"));
    assertTrue(hidden("ink"));
    assertTrue(hidden("ground_swatch_01"));
    assertFalse(hidden("zombie_piano_torso"));
  }
}
