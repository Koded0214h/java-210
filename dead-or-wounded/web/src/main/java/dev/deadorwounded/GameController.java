package dev.deadorwounded;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/game")
class GameController {

    private static final String SESSION_KEY = "gameState";
    static final int TIME_LIMIT_SECONDS = 60;

    record NewGameResponse(int timeLimitSeconds) {
    }

    record GuessRequest(String guess) {
    }

    record GuessResponse(int dead, int wounded, boolean won, int attempts) {
    }

    record RevealResponse(String secret, int attempts) {
    }

    @PostMapping("/new")
    NewGameResponse newGame(HttpSession session) {
        session.setAttribute(SESSION_KEY, new GameState(GameEngine.generateSecret()));
        return new NewGameResponse(TIME_LIMIT_SECONDS);
    }

    @PostMapping("/guess")
    GuessResponse guess(@RequestBody GuessRequest request, HttpSession session) {
        GameState state = activeGame(session);

        if (state.isWon()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already won.");
        }

        if (!GameEngine.isValidGuess(request.guess())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter exactly 4 digits.");
        }

        GameEngine.Result result = GameEngine.evaluate(request.guess(), state.getSecret());
        state.incrementAttempts();
        if (result.isWin()) {
            state.markWon();
        }

        return new GuessResponse(result.dead(), result.wounded(), result.isWin(), state.getAttempts());
    }

    @GetMapping("/reveal")
    RevealResponse reveal(HttpSession session) {
        GameState state = activeGame(session);
        return new RevealResponse(state.getSecret(), state.getAttempts());
    }

    private GameState activeGame(HttpSession session) {
        GameState state = (GameState) session.getAttribute(SESSION_KEY);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No active game. Start a new game first.");
        }
        return state;
    }
}
