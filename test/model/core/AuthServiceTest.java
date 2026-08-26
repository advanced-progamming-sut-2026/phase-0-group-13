package model.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The account validators: pure functions, no file or network I/O, so nothing here can touch a
 * real save file. {@link AuthService#checkUsername} etc. only check shape -- server-side
 * uniqueness is Phase 3's job and is deliberately not exercised here.
 */
class AuthServiceTest {

  // ---- password -----------------------------------------------------------------------------

  @Test
  void aPasswordShorterThanEightCharsIsRejectedEvenIfOtherwiseStrong() {
    Result result = AuthService.checkPassword("Ab1!ab1");
    assertFalse(result.success());
  }

  @Test
  void aPasswordMissingAnyOfTheFourClassesIsRejected() {
    assertFalse(AuthService.checkPassword("alllowercase1!").success(), "no uppercase");
    assertFalse(AuthService.checkPassword("ALLUPPERCASE1!").success(), "no lowercase");
    assertFalse(AuthService.checkPassword("NoDigitsHere!!").success(), "no digit");
    assertFalse(AuthService.checkPassword("NoSpecial1234").success(), "no special character");
  }

  @Test
  void aPasswordWithAllFourClassesAndEnoughLengthPasses() {
    // '-' is not in AuthService's own special-character set (it's a plain allowed character
    // elsewhere, e.g. usernames), so the special character here has to be one that counts, '!'.
    assertTrue(AuthService.checkPassword("Strong!Pass1").success());
  }

  @Test
  void hashPasswordIsDeterministicAndNeverStoresThePlainText() {
    String hash = AuthService.hashPassword("Strong-Pass1");
    assertEquals(hash, AuthService.hashPassword("Strong-Pass1"), "same input, same hash");
    assertEquals(64, hash.length(), "SHA-256 as hex is 64 characters");
    assertFalse(hash.contains("Strong-Pass1"), "the plain password must not leak into the hash");
  }

  @Test
  void hashPasswordDiffersForDifferentInputs() {
    assertFalse(AuthService.hashPassword("Strong-Pass1")
        .equals(AuthService.hashPassword("Strong-Pass2")));
  }

  // ---- username -------------------------------------------------------------------------------

  @Test
  void anEmptyOrNullUsernameIsRejected() {
    assertFalse(AuthService.checkUsername("").success());
    assertFalse(AuthService.checkUsername(null).success());
  }

  @ParameterizedTest
  @ValueSource(strings = {"has space", "has@symbol", "has.dot", "has_underscore", "emoji😀"})
  void aUsernameWithAnythingOtherThanLettersDigitsOrHyphenIsRejected(String username) {
    assertFalse(AuthService.checkUsername(username).success(), username);
  }

  @Test
  void aUsernameOfLettersDigitsAndHyphensIsAccepted() {
    assertTrue(AuthService.checkUsername("player-One-42").success());
  }

  // ---- nickname -------------------------------------------------------------------------------

  @Test
  void aNicknameShorterThanThreeOrLongerThanThirtyIsRejected() {
    assertFalse(AuthService.checkNickname("ab").success(), "too short");
    assertFalse(AuthService.checkNickname("a".repeat(31)).success(), "too long");
  }

  @Test
  void aNicknameInRangeIsAccepted() {
    assertTrue(AuthService.checkNickname("abc").success(), "lower bound");
    assertTrue(AuthService.checkNickname("a".repeat(30)).success(), "upper bound");
  }

  // ---- gender ---------------------------------------------------------------------------------

  @Test
  void genderAcceptsMaleOrFemaleCaseInsensitively() {
    assertTrue(AuthService.checkGender("Male").success());
    assertTrue(AuthService.checkGender("FEMALE").success());
  }

  @Test
  void genderRejectsAnythingElse() {
    assertFalse(AuthService.checkGender("other").success());
    assertFalse(AuthService.checkGender(null).success());
  }

  // ---- email ----------------------------------------------------------------------------------

  @Test
  void aWellFormedEmailIsAccepted() {
    assertTrue(AuthService.checkEmail("player.one@example.com").success());
  }

  @Test
  void anEmailWithoutOrWithTwoAtSymbolsIsRejected() {
    assertFalse(AuthService.checkEmail("no-at-symbol.com").success());
    assertFalse(AuthService.checkEmail("two@at@symbols.com").success());
  }

  @Test
  void anEmailWithConsecutiveDotsInTheLocalPartIsRejected() {
    assertFalse(AuthService.checkEmail("player..one@example.com").success());
  }

  @Test
  void anEmailWithoutADotInTheDomainIsRejected() {
    assertFalse(AuthService.checkEmail("player@examplecom").success());
  }

  @Test
  void anEmailWithATooShortTopLevelDomainIsRejected() {
    assertFalse(AuthService.checkEmail("player@example.c").success());
  }
}
