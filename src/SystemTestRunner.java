import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class SystemTestRunner {

    private static final String COMMANDS_FILE = "e2e_test_commands";
    private static final String OUTPUT_LOG_FILE = "e2e_output_log.txt";

    public static void main(String[] args) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        try (FileInputStream fis = new FileInputStream(new File(COMMANDS_FILE));
             PrintStream logStream = new PrintStream(new FileOutputStream(OUTPUT_LOG_FILE), true)) {

            System.setIn(fis);
            System.setOut(logStream);
            System.setErr(logStream);

            logStream.println("=== E2E Test Run Started ===");
            logStream.println("Command script: " + COMMANDS_FILE);
            logStream.println();

            try {
                Main.main(new String[0]);
                logStream.println();
                logStream.println("=== Application returned normally (all input consumed or exit reached) ===");
            } catch (Throwable t) {
                logStream.println();
                logStream.println("=== UNCAUGHT EXCEPTION DURING E2E RUN ===");
                logStream.println(t.toString());
                for (StackTraceElement el : t.getStackTrace()) {
                    logStream.println("    at " + el);
                }
            }

        } catch (IOException e) {
            System.setOut(originalOut);
            System.err.println("Failed to set up E2E test I/O redirection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            System.setErr(originalErr);
            System.out.println("E2E test run complete. See " + OUTPUT_LOG_FILE + " for full output.");
        }
    }
}