package dev.deadorwounded;

import java.security.SecureRandom;

/** Port of the guess-evaluation rules from the original console game. */
class GameEngine {

    private static final SecureRandom RANDOM = new SecureRandom();

    private GameEngine() {
    }

    static String generateSecret() {
        StringBuilder secret = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            secret.append(RANDOM.nextInt(10));
        }
        return secret.toString();
    }

    static boolean isValidGuess(String guess) {
        return guess != null && guess.matches("\\d{4}");
    }

    record Result(int dead, int wounded) {
        boolean isWin() {
            return dead == 4;
        }
    }

    static Result evaluate(String guess, String secret) {
        int dead = 0;
        int wounded = 0;

        for (int i = 0; i < 4; i++) {
            char guessChar = guess.charAt(i);
            if (guessChar == secret.charAt(i)) {
                dead++;
            } else if (secret.indexOf(guessChar) >= 0) {
                wounded++;
            }
        }

        return new Result(dead, wounded);
    }
}
