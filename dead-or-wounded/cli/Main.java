import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static AtomicInteger secondsRemaining = new AtomicInteger(60);
    public static AtomicBoolean isTimeUp = new AtomicBoolean(false);

    public static String askForInput(Scanner scanner) {
        System.out.print("Enter a 4-digit number: ");
        String guess = scanner.nextLine().trim();

        if (guess.length() == 4 && guess.matches("\\d{4}")) {
            return guess;
        } else {
            System.out.println("Invalid input: Enter exactly 4 numbers.");
            return null;
        }
    }

    public static boolean evaluateGuess(String guess, String secret) {
        int dead = 0;
        int wounded = 0;

        for (int i = 0; i < 4; i++) {
            char guessChar = guess.charAt(i);

            if (guessChar == secret.charAt(i)) {
                dead++;
            } else if (secret.contains(String.valueOf(guessChar))) {
                wounded++;
            }
        }

        System.out.println("Result: " + dead + " Dead, " + wounded + " Wounded\n");
        
        // Returns true if all 4 digits match exactly
        return dead == 4;
    }

    public static ScheduledExecutorService scheduledCountdown() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            if (secondsRemaining.decrementAndGet() <= 0) {
                isTimeUp.set(true);
                System.out.println("\n\n[TIME'S UP!] Press Enter to finish.");
                scheduler.shutdown();
            }
        }, 1, 1, TimeUnit.SECONDS);

        return scheduler;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String secret = "4902";

        System.out.println("=== DEAD OR WOUNDED GAME ===");
        System.out.println("You have 60 seconds to guess the 4-digit code!\n");

        // Start background timer thread
        ScheduledExecutorService scheduler = scheduledCountdown();

        while (!isTimeUp.get()) {
            String guess = askForInput(scanner);

            // Check if time expired while blocking for user input
            if (isTimeUp.get()) {
                break;
            }

            if (guess != null) {
                boolean won = evaluateGuess(guess, secret);
                if (won) {
                    System.out.println("🎉 Congratulations! You guessed the code!");
                    scheduler.shutdownNow(); // Cleanly kill background timer thread
                    scanner.close();
                    return;
                }
            }
        }

        System.out.println("\nGame Over! The secret code was: " + secret);
        scheduler.shutdownNow();
        scanner.close();
    }
}