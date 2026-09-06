package view.gdx;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * macOS will not let GLFW run on any thread but the process's first, so the JVM has to be started
 * with {@code -XstartOnFirstThread}.
 *
 * <p>Nobody handed a jar knows that, and without the flag the game dies on its first line with a
 * stack trace about {@code EventLoop.check}. So when the flag is missing, this starts the JVM
 * again with it and hands the game over to that process.
 */
final class StartOnFirstThread {

  /** The JVM sets this itself, on macOS, when it was started with the flag. */
  private static final String JVM_MARKER = "JAVA_STARTED_ON_FIRST_THREAD_";
  /** Our own, in case a JVM ever stops setting the one above: without it this would fork forever. */
  private static final String OWN_MARKER = "PVZ_RESTARTED_ON_FIRST_THREAD";

  private StartOnFirstThread() {}

  /** Hands over to a new JVM and never returns, or returns for the game to start here. */
  static void handOverIfNeeded(String mainClass, String[] args) {
    if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")
        || "1".equals(System.getenv(JVM_MARKER + ProcessHandle.current().pid()))
        || "1".equals(System.getenv(OWN_MARKER))) {
      return;
    }
    try {
      ProcessBuilder restarted = new ProcessBuilder(commandFor(mainClass, args)).inheritIO();
      restarted.environment().put(OWN_MARKER, "1");
      System.exit(restarted.start().waitFor());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      System.out.println("could not start on the first thread (" + e.getMessage() + ").");
      System.out.println("run it with: java -XstartOnFirstThread -jar <this jar>");
    }
  }

  private static List<String> commandFor(String mainClass, String[] args) {
    List<String> command = new ArrayList<>();
    command.add(ProcessHandle.current().info().command().orElse(
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"));
    for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      if (!argument.startsWith("-XstartOnFirstThread")) {
        command.add(argument);
      }
    }
    command.add("-XstartOnFirstThread");
    // Started from the packed jar, hand the jar back rather than a classpath: only a -jar launch
    // reads the manifest, and that is what turns off the native-access warnings.
    String classpath = System.getProperty("java.class.path", "");
    if (classpath.endsWith(".jar") && !classpath.contains(File.pathSeparator)) {
      command.add("-jar");
      command.add(classpath);
    } else {
      command.add("-cp");
      command.add(classpath);
      command.add(mainClass);
    }
    command.addAll(List.of(args));
    return command;
  }
}
