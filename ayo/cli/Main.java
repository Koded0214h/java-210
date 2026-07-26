import java.util.Scanner;

public class Main {

    static final int PITS_PER_SIDE = 6;
    static final int PIT_COUNT = PITS_PER_SIDE * 2;
    static final int SEEDS_PER_PIT = 4;

    // pits[0..5]  = Player A's pits (A1..A6)
    // pits[6..11] = Player B's pits (B1..B6)
    // Sowing moves in increasing index order, wrapping 11 -> 0.
    static int[] pits = new int[PIT_COUNT];
    static int[] stores = new int[2]; // stores[0] = Player A, stores[1] = Player B
    static int currentPlayer = 0; // 0 = Player A, 1 = Player B

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        resetBoard();

        System.out.println("=== AYO ===");
        System.out.println("Capture an opponent's pit by making it hold 2 or 3 seeds.\n");

        while (true) {
            if (!hasSeeds(currentPlayer)) {
                collectRemainingSeeds();
                break;
            }

            printBoard();
            int pit = askForPit(scanner);
            int lastPit = sow(pit);
            resolveCaptures(lastPit);
            currentPlayer = 1 - currentPlayer;
        }

        printBoard();
        announceWinner();
        scanner.close();
    }

    static void resetBoard() {
        for (int i = 0; i < PIT_COUNT; i++) {
            pits[i] = SEEDS_PER_PIT;
        }
        stores[0] = 0;
        stores[1] = 0;
        currentPlayer = 0;
    }

    static boolean hasSeeds(int player) {
        int start = player == 0 ? 0 : PITS_PER_SIDE;
        for (int i = start; i < start + PITS_PER_SIDE; i++) {
            if (pits[i] > 0) {
                return true;
            }
        }
        return false;
    }

    static boolean ownsPit(int player, int pitIndex) {
        int start = player == 0 ? 0 : PITS_PER_SIDE;
        return pitIndex >= start && pitIndex < start + PITS_PER_SIDE;
    }

    static int askForPit(Scanner scanner) {
        while (true) {
            System.out.printf("Player %s, choose a pit (1-6): ", currentPlayer == 0 ? "A" : "B");
            String input = scanner.nextLine().trim();

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Enter a number between 1 and 6.\n");
                continue;
            }

            if (choice < 1 || choice > PITS_PER_SIDE) {
                System.out.println("Enter a number between 1 and 6.\n");
                continue;
            }

            int pitIndex = (currentPlayer == 0 ? 0 : PITS_PER_SIDE) + (choice - 1);
            if (pits[pitIndex] == 0) {
                System.out.println("That pit is empty. Choose another.\n");
                continue;
            }

            return pitIndex;
        }
    }

    /** Sows the seeds from pitIndex counter-clockwise, skipping the origin pit. Returns the index of the last sown pit. */
    static int sow(int pitIndex) {
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

    static void resolveCaptures(int lastPit) {
        int opponent = 1 - currentPlayer;
        int pos = lastPit;

        while (ownsPit(opponent, pos) && (pits[pos] == 2 || pits[pos] == 3)) {
            stores[currentPlayer] += pits[pos];
            pits[pos] = 0;
            pos = (pos - 1 + PIT_COUNT) % PIT_COUNT;
        }
    }

    static void collectRemainingSeeds() {
        for (int player = 0; player < 2; player++) {
            int start = player == 0 ? 0 : PITS_PER_SIDE;
            for (int i = start; i < start + PITS_PER_SIDE; i++) {
                stores[player] += pits[i];
                pits[i] = 0;
            }
        }
    }

    static void printBoard() {
        System.out.println();
        System.out.print("        ");
        for (int i = PIT_COUNT - 1; i >= PITS_PER_SIDE; i--) {
            System.out.printf("%3d", pits[i]);
        }
        System.out.println("   <- Player B (B6..B1)");

        System.out.printf("Store B [%2d]%" + (PITS_PER_SIDE * 3 - 9) + "s[%2d] Store A%n", stores[1], "", stores[0]);

        System.out.print("        ");
        for (int i = 0; i < PITS_PER_SIDE; i++) {
            System.out.printf("%3d", pits[i]);
        }
        System.out.println("   <- Player A (A1..A6)");
        System.out.println();
    }

    static void announceWinner() {
        System.out.println("Game Over!");
        System.out.println("Player A store: " + stores[0]);
        System.out.println("Player B store: " + stores[1]);

        if (stores[0] > stores[1]) {
            System.out.println("Player A wins!");
        } else if (stores[1] > stores[0]) {
            System.out.println("Player B wins!");
        } else {
            System.out.println("It's a tie!");
        }
    }
}
