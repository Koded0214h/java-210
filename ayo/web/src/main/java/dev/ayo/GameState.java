package dev.ayo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Board state for one Ayo (Oware/Ayoayo) game, held per HTTP session. */
class GameState implements Serializable {

    static final int PITS_PER_SIDE = 6;
    static final int PIT_COUNT = PITS_PER_SIDE * 2;
    static final int SEEDS_PER_PIT = 4;

    // pits[0..5]  = Player A's pits (A1..A6)
    // pits[6..11] = Player B's pits (B1..B6)
    // Sowing moves in increasing index order, wrapping 11 -> 0.
    private final int[] pits = new int[PIT_COUNT];
    private final int[] stores = new int[2];
    private int currentPlayer = 0;
    private boolean gameOver = false;

    GameState() {
        reset();
    }

    void reset() {
        for (int i = 0; i < PIT_COUNT; i++) {
            pits[i] = SEEDS_PER_PIT;
        }
        stores[0] = 0;
        stores[1] = 0;
        currentPlayer = 0;
        gameOver = false;
    }

    int[] getPits() {
        return pits;
    }

    int[] getStores() {
        return stores;
    }

    int getCurrentPlayer() {
        return currentPlayer;
    }

    boolean isGameOver() {
        return gameOver;
    }

    boolean hasSeeds(int player) {
        int start = player == 0 ? 0 : PITS_PER_SIDE;
        for (int i = start; i < start + PITS_PER_SIDE; i++) {
            if (pits[i] > 0) {
                return true;
            }
        }
        return false;
    }

    boolean ownsPit(int player, int pitIndex) {
        int start = player == 0 ? 0 : PITS_PER_SIDE;
        return pitIndex >= start && pitIndex < start + PITS_PER_SIDE;
    }

    /** Applies a move: sow from pitIndex, resolve captures, and advance/end the game. Returns the outcome. */
    MoveOutcome applyMove(int pitIndex) {
        int lastPit = sow(pitIndex);
        List<Integer> capturedPits = resolveCaptures(lastPit);

        currentPlayer = 1 - currentPlayer;

        if (!hasSeeds(currentPlayer)) {
            collectRemainingSeeds();
            gameOver = true;
        }

        return new MoveOutcome(lastPit, capturedPits, gameOver, winner());
    }

    private int sow(int pitIndex) {
        int seeds = pits[pitIndex];
        pits[pitIndex] = 0;

        int pos = pitIndex;
        int lastPos = pitIndex;
        while (seeds > 0) {
            pos = (pos + 1) % PIT_COUNT;
            if (pos == pitIndex) {
                pos = (pos + 1) % PIT_COUNT;
            }
            pits[pos]++;
            lastPos = pos;
            seeds--;
        }
        return lastPos;
    }

    private List<Integer> resolveCaptures(int lastPit) {
        List<Integer> captured = new ArrayList<>();
        int opponent = 1 - currentPlayer;
        int pos = lastPit;

        while (ownsPit(opponent, pos) && (pits[pos] == 2 || pits[pos] == 3)) {
            stores[currentPlayer] += pits[pos];
            pits[pos] = 0;
            captured.add(pos);
            pos = (pos - 1 + PIT_COUNT) % PIT_COUNT;
        }
        return captured;
    }

    private void collectRemainingSeeds() {
        for (int player = 0; player < 2; player++) {
            int start = player == 0 ? 0 : PITS_PER_SIDE;
            for (int i = start; i < start + PITS_PER_SIDE; i++) {
                stores[player] += pits[i];
                pits[i] = 0;
            }
        }
    }

    Integer winner() {
        if (!gameOver) {
            return null;
        }
        if (stores[0] > stores[1]) {
            return 0;
        }
        if (stores[1] > stores[0]) {
            return 1;
        }
        return -1; // tie
    }

    record MoveOutcome(int lastPit, List<Integer> capturedPits, boolean gameOver, Integer winner) {
    }
}
