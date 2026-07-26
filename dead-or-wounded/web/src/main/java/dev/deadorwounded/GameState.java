package dev.deadorwounded;

import java.io.Serializable;

class GameState implements Serializable {

    private final String secret;
    private int attempts;
    private boolean won;

    GameState(String secret) {
        this.secret = secret;
        this.attempts = 0;
        this.won = false;
    }

    String getSecret() {
        return secret;
    }

    int getAttempts() {
        return attempts;
    }

    void incrementAttempts() {
        attempts++;
    }

    boolean isWon() {
        return won;
    }

    void markWon() {
        won = true;
    }
}
