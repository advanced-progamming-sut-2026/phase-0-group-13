package network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Who may take an invite, and what the random queue does. */
class MatchmakingServiceTest {

  @Test
  void onlyTheAddresseeCanAcceptAnInvite() {
    MatchmakingService matchmaking = new MatchmakingService();
    MatchmakingService.Invite invite = matchmaking.invite("alice", "bob");

    assertNull(matchmaking.consumeInvite(invite.id(), "mallory"),
        "an invite id is not a ticket into somebody else's match");
    assertNull(matchmaking.consumeInvite(invite.id(), null));

    // The refused attempts must not have eaten it: bob can still accept his own invite.
    MatchmakingService.Invite accepted = matchmaking.consumeInvite(invite.id(), "bob");
    assertNotNull(accepted);
    assertEquals("alice", accepted.from());
    assertEquals("bob", accepted.to());
  }

  @Test
  void anInviteCanOnlyBeAcceptedOnce() {
    MatchmakingService matchmaking = new MatchmakingService();
    MatchmakingService.Invite invite = matchmaking.invite("alice", "bob");

    assertNotNull(matchmaking.consumeInvite(invite.id(), "bob"));
    assertNull(matchmaking.consumeInvite(invite.id(), "bob"));
  }

  @Test
  void theRecipientCheckIgnoresCase() {
    MatchmakingService matchmaking = new MatchmakingService();
    MatchmakingService.Invite invite = matchmaking.invite("alice", "Bob");

    assertNotNull(matchmaking.consumeInvite(invite.id(), "bOB"));
  }

  @Test
  void anUnknownInviteIsRefused() {
    assertNull(new MatchmakingService().consumeInvite("not-an-invite", "bob"));
    assertNull(new MatchmakingService().consumeInvite(null, "bob"));
  }

  @Test
  void theQueuePairsTheSecondPlayerWithTheFirst() {
    MatchmakingService matchmaking = new MatchmakingService();

    assertNull(matchmaking.enqueue("alice"), "first in waits");
    assertTrue(matchmaking.isQueued("alice"));
    assertEquals("alice", matchmaking.enqueue("bob"));
    assertEquals(0, matchmaking.queueSize());
  }

  @Test
  void queueingTwiceDoesNotPairAPlayerWithThemselves() {
    MatchmakingService matchmaking = new MatchmakingService();
    matchmaking.enqueue("alice");

    assertNull(matchmaking.enqueue("alice"));
    assertEquals(1, matchmaking.queueSize());
  }

  @Test
  void cancellingLeavesTheQueueAndDropsTheInvites() {
    MatchmakingService matchmaking = new MatchmakingService();
    matchmaking.enqueue("alice");
    MatchmakingService.Invite invite = matchmaking.invite("alice", "bob");

    matchmaking.cancel("alice");

    assertEquals(0, matchmaking.queueSize());
    assertNull(matchmaking.consumeInvite(invite.id(), "bob"));
  }
}
