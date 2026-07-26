package dev.ayo;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/game")
class GameController {

    private static final String SESSION_KEY = "ayoGameState";

    record MoveRequest(int pit) {
    }

    record StateResponse(
            int[] pits,
            int[] stores,
            int currentPlayer,
            boolean gameOver,
            Integer winner,
            Integer lastPit,
            List<Integer> capturedPits
    ) {
    }

    @PostMapping("/new")
    StateResponse newGame(HttpSession session) {
        GameState state = new GameState();
        session.setAttribute(SESSION_KEY, state);
        return toResponse(state, null, List.of());
    }

    @PostMapping("/move")
    StateResponse move(@RequestBody MoveRequest request, HttpSession session) {
        GameState state = activeGame(session);

        if (state.isGameOver()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is already over.");
        }

        int pit = request.pit();
        if (pit < 0 || pit >= GameState.PIT_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pit must be between 0 and 11.");
        }
        if (!state.ownsPit(state.getCurrentPlayer(), pit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That pit doesn't belong to the current player.");
        }
        if (state.getPits()[pit] == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That pit is empty.");
        }

        GameState.MoveOutcome outcome = state.applyMove(pit);
        return toResponse(state, outcome.lastPit(), outcome.capturedPits());
    }

    private GameState activeGame(HttpSession session) {
        GameState state = (GameState) session.getAttribute(SESSION_KEY);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No active game. Start a new game first.");
        }
        return state;
    }

    private StateResponse toResponse(GameState state, Integer lastPit, List<Integer> capturedPits) {
        return new StateResponse(
                state.getPits().clone(),
                state.getStores().clone(),
                state.getCurrentPlayer(),
                state.isGameOver(),
                state.winner(),
                lastPit,
                capturedPits
        );
    }
}
