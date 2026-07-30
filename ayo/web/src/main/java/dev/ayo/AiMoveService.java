package dev.ayo;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Picks Player B's move in "auto mode" by asking Gemini to reason over the
 * current board. Falls back to the first legal pit if the model call fails
 * or returns something that isn't actually a legal move, so a flaky network
 * call can never break the game.
 */
@Component
class AiMoveService {

    private static final Logger log = LoggerFactory.getLogger(AiMoveService.class);
    private static final int AI_PLAYER = 1;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "pit", Map.of("type", "STRING", "description", "The pit label to sow from, e.g. B3"),
                    "reason", Map.of("type", "STRING", "description", "One short sentence explaining the choice")
            ),
            "required", List.of("pit", "reason")
    );

    private final GeminiClient geminiClient;

    AiMoveService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    record AiMove(int pitIndex, String reason) {
    }

    AiMove chooseMove(GameState state) {
        List<Integer> validPits = state.validPitsFor(AI_PLAYER);
        if (validPits.isEmpty()) {
            throw new IllegalStateException("No legal moves for the AI player.");
        }

        try {
            String prompt = buildPrompt(state, validPits);
            JsonNode json = geminiClient.generateJson(prompt, RESPONSE_SCHEMA);
            String label = json.path("pit").asText("");
            String reason = json.path("reason").asText("");
            Integer pitIndex = labelToIndex(label);

            if (pitIndex != null && validPits.contains(pitIndex)) {
                return new AiMove(pitIndex, reason.isBlank() ? "Gemini's pick." : reason);
            }
        } catch (Exception e) {
            log.warn("Gemini move selection failed, falling back to first legal pit: {}", e.toString());
        }

        return new AiMove(validPits.get(0), "Fallback move (AI call unavailable).");
    }

    private String buildPrompt(GameState state, List<Integer> validPits) {
        int[] pits = state.getPits();
        int[] stores = state.getStores();
        StringBuilder sb = new StringBuilder();
        sb.append("You are playing Ayo (Ayoayo/Oware), a sowing-and-capture board game, as Player B.\n");
        sb.append("Board (pit label = seed count):\n");
        for (int i = 0; i < GameState.PITS_PER_SIDE; i++) {
            sb.append("A").append(i + 1).append("=").append(pits[i]).append(" ");
        }
        sb.append("\n");
        for (int i = GameState.PITS_PER_SIDE; i < GameState.PIT_COUNT; i++) {
            sb.append("B").append(i - GameState.PITS_PER_SIDE + 1).append("=").append(pits[i]).append(" ");
        }
        sb.append("\nStores: A=").append(stores[0]).append(" B=").append(stores[1]).append("\n");
        sb.append("Sowing goes in increasing pit order A1->A6->B1->B6->A1, skipping the origin pit on a full lap.\n");
        sb.append("You capture an opponent pit if your last sown seed lands there and it then holds exactly 2 or 3 seeds; captures chain backward through consecutive such pits.\n");
        sb.append("Your legal pits this turn: ").append(validPits.stream().map(this::labelFor).reduce((a, b) -> a + ", " + b).orElse("")).append("\n");
        sb.append("Choose the pit that best sets up a capture or protects your pits from being captured next turn.");
        return sb.toString();
    }

    private String labelFor(int index) {
        return index < GameState.PITS_PER_SIDE ? "A" + (index + 1) : "B" + (index - GameState.PITS_PER_SIDE + 1);
    }

    private Integer labelToIndex(String label) {
        if (label == null || label.length() < 2) {
            return null;
        }
        char side = Character.toUpperCase(label.charAt(0));
        try {
            int n = Integer.parseInt(label.substring(1).trim());
            if (n < 1 || n > GameState.PITS_PER_SIDE) {
                return null;
            }
            if (side == 'A') {
                return n - 1;
            }
            if (side == 'B') {
                return GameState.PITS_PER_SIDE + n - 1;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
