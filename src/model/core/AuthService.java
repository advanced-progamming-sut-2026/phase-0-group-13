package model.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import model.Result;

public class AuthService {
  // داوش زدم تو گوش امتیازی ها
  // بزنم به تخته مامانم یه مهندس تحویل دنیا داده
  // ( تست نشده کامل )
  public static String hashPassword(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception ex) {
      throw new RuntimeException("Error hashing password", ex);
    }
  }

  public static Result checkPassword(String password) {
    if (password == null || password.length() < 8) {
      return new Result(false, "error: weak password. length must be at least 8 characters.", null);
    }

    boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
    String specialChars = "][}{+=)(*&^%$#!?><";

    for (char c : password.toCharArray()) {
      if (Character.isUpperCase(c)) hasUpper = true;
      else if (Character.isLowerCase(c)) hasLower = true;
      else if (Character.isDigit(c)) hasDigit = true;
      else if (specialChars.indexOf(c) != -1) hasSpecial = true;
    }

    if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
      return new Result(
          false,
          "error: weak password. must contain uppercase, lowercase, numbers, and special"
              + " characters.",
          null);
    }

    return new Result(true, "Password is valid", null);
  }

  public static Result checkEmail(String email) {
    if (email == null || email.isEmpty()) {
      return new Result(false, "error: email is empty", null);
    }
    String invalidChars = "\\/|][{}+=)(*&^%$#!?><";
    for (char c : email.toCharArray()) {
      if (invalidChars.indexOf(c) != -1) {
        return new Result(false, "error: email contains invalid symbols", null);
      }
    }

    String[] parts = email.split("@", -1);
    if (parts.length != 2) {
      return new Result(false, "error: email must contain exactly one @ symbol", null);
    }

    String username = parts[0];
    String domain = parts[1];
    if (username.isEmpty()) {
      return new Result(false, "error: email username part cannot be empty", null);
    }
    if (username.contains("..")) {
      return new Result(
          false, "error: dot (.) cannot appear twice consecutively in email username", null);
    }
    if (!Pattern.matches("^[a-zA-Z0-9]([a-zA-Z0-9_\\-\\.]*[a-zA-Z0-9])?$", username)) {
      return new Result(
          false,
          "error: email username must start/end with a letter/number and contain only valid"
              + " characters",
          null);
    }
    if (!domain.contains(".")) {
      return new Result(false, "error: email domain must contain at least one dot (.)", null);
    }
    String[] domainParts = domain.split("\\.");
    String extension = domainParts[domainParts.length - 1];
    if (extension.length() < 2) {
      return new Result(false, "error: email domain extension must be at least 2 characters", null);
    }
    if (!Pattern.matches(
        "^[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*$",
        domain)) {
      return new Result(
          false,
          "error: email domain contains invalid characters or starts/ends with invalid characters",
          null);
    }
    return new Result(true, "Email is valid", null);
  }
}
