package model.game.minigame.arcade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.enums.MatchRole;
import org.junit.jupiter.api.Test;

/**
 * Couch play routes the mouse to {@link MatchRole#PLANTS} and the keyboard to
 * {@link MatchRole#ZOMBIES} through the same IZombieMatch.apply the networked game uses, so the
 * rules cannot drift between the two modes. These pin the behaviour that routing depends on:
 * a role may only make its own kind of move, both roles can act on the same match without one
 * blocking the other, and the match is decided the same way whoever is driving it.
 *
 * <p>The seed is fixed so the starting board is the same every run.
 */
class CouchPlayRoutingTest {

  private static final long SEED = 4242L;

  private static IZombieMatch match() {
    return new IZombieMatch(1, SEED);
  }

  private static String someZombie(IZombieMatch match) {
    return match.getEngine().availableZombieTypes().get(0).name;
  }

  private static String somePlant() {
    return IZombieEngine.availablePlantTypes().get(0).name;
  }

  @Test
  void theMouseSideCannotPlaceZombiesAndTheKeyboardSideCannotPlacePlants() {
    IZombieMatch match = match();
    String zombie = someZombie(match);
    String plant = somePlant();

    String plantsPlacingAZombie =
        match.apply(MatchRole.PLANTS, IZombieAction.placeZombie(zombie, 0, IZombieEngine.COLS - 1));
    assertNotNull(plantsPlacingAZombie, "the plant player must not be able to deploy zombies");
    assertTrue(plantsPlacingAZombie.startsWith("error:"), plantsPlacingAZombie);

    String zombiesPlacingAPlant =
        match.apply(MatchRole.ZOMBIES, IZombieAction.placePlant(plant, 0, 0));
    assertNotNull(zombiesPlacingAPlant, "the zombie player must not be able to plant");
    assertTrue(zombiesPlacingAPlant.startsWith("error:"), zombiesPlacingAPlant);
  }

  @Test
  void bothSidesCanActOnTheSameMatchInTheSameTick() {
    IZombieMatch match = match();
    // Enough sun for either side to afford its cheapest option.
    for (int i = 0; i < 200; i++) {
      match.tick();
    }
    String zombie = cheapestZombie(match);
    String plant = cheapestPlant();

    String zombieMove = match.apply(MatchRole.ZOMBIES,
        IZombieAction.placeZombie(zombie, 2, IZombieEngine.COLS - 1));
    String plantMove = match.apply(MatchRole.PLANTS, IZombieAction.placePlant(plant, 4, 1));

    assertNull(zombieMove, "the zombie player was refused: " + zombieMove);
    assertNull(plantMove, "the plant player was refused: " + plantMove);

    IZombieMatch.Snapshot after = match.snapshot();
    assertTrue(after.zombies().stream().anyMatch(z -> z.row() == 2),
        "the keyboard placement did not reach the board");
    assertTrue(after.plants().stream().anyMatch(p -> p.row() == 4 && p.col() == 1),
        "the mouse placement did not reach the board");
  }

  @Test
  void aFinishedMatchRefusesBothSides() {
    IZombieMatch match = match();
    while (!match.isFinished()) {
      match.tick();
    }
    assertNotNull(match.winner());
    assertNotNull(match.apply(MatchRole.PLANTS, IZombieAction.placePlant(somePlant(), 0, 0)));
    assertNotNull(match.apply(MatchRole.ZOMBIES,
        IZombieAction.placeZombie(someZombie(match), 0, IZombieEngine.COLS - 1)));
  }

  @Test
  void aLocalMatchIsDecidedTheSameWayAServerOneIs() {
    // Left alone, the defender holds out and the clock runs down to the plant player's win. Same
    // rule the server's MatchService relies on, reached here with no server in the picture.
    IZombieMatch match = match();
    while (!match.isFinished()) {
      match.tick();
    }
    assertEquals(MatchRole.PLANTS, match.winner());
    assertTrue(match.getTick() <= IZombieMatch.SURVIVAL_TICKS);
    assertEquals(0, IZombieMatch.scoreFor(match.snapshot(), MatchRole.ZOMBIES),
        "an attacker who took no brains scores nothing");
    assertTrue(IZombieMatch.scoreFor(match.snapshot(), MatchRole.PLANTS) > 0,
        "the defender is paid for the time they held");
  }

  private static String cheapestZombie(IZombieMatch match) {
    IZombieEngine.ZombieSpec best = match.getEngine().availableZombieTypes().get(0);
    for (IZombieEngine.ZombieSpec spec : match.getEngine().availableZombieTypes()) {
      if (spec.cost < best.cost) {
        best = spec;
      }
    }
    return best.name;
  }

  private static String cheapestPlant() {
    IZombieEngine.PlantSpec best = IZombieEngine.availablePlantTypes().get(0);
    for (IZombieEngine.PlantSpec spec : IZombieEngine.availablePlantTypes()) {
      if (spec.cost < best.cost) {
        best = spec;
      }
    }
    return best.name;
  }
}
