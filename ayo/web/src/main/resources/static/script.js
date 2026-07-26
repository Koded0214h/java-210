const startButton = document.getElementById("start");
const turnIndicator = document.getElementById("turn-indicator");
const messageEl = document.getElementById("message");
const rowB = document.getElementById("row-b");
const rowA = document.getElementById("row-a");
const storeAEl = document.getElementById("store-a");
const storeBEl = document.getElementById("store-b");
const gameOverEl = document.getElementById("game-over");
const gameOverMessageEl = document.getElementById("game-over-message");

// row-b displays indices 11..6 (B6..B1), row-a displays indices 0..5 (A1..A6)
const ROW_B_INDICES = [11, 10, 9, 8, 7, 6];
const ROW_A_INDICES = [0, 1, 2, 3, 4, 5];

let state = null;

function pitLabel(index) {
    return index < 6 ? `A${index + 1}` : `B${index - 5}`;
}

function render() {
    if (!state) {
        return;
    }

    storeAEl.textContent = state.stores[0];
    storeBEl.textContent = state.stores[1];

    turnIndicator.textContent = state.gameOver
        ? "Game over"
        : `Player ${state.currentPlayer === 0 ? "A" : "B"}'s turn`;

    rowB.innerHTML = "";
    ROW_B_INDICES.forEach((index) => rowB.appendChild(renderPit(index)));

    rowA.innerHTML = "";
    ROW_A_INDICES.forEach((index) => rowA.appendChild(renderPit(index)));

    if (state.gameOver) {
        gameOverEl.hidden = false;
        if (state.winner === -1) {
            gameOverMessageEl.textContent = `It's a tie! ${state.stores[0]} - ${state.stores[1]}`;
        } else {
            const winnerName = state.winner === 0 ? "Player A" : "Player B";
            gameOverMessageEl.textContent = `${winnerName} wins! ${state.stores[0]} - ${state.stores[1]}`;
        }
    } else {
        gameOverEl.hidden = true;
    }
}

function renderPit(index) {
    const pit = document.createElement("div");
    pit.className = "pit";

    const seeds = state.pits[index];
    const playable = !state.gameOver && ownsPit(state.currentPlayer, index) && seeds > 0;

    if (playable) {
        pit.classList.add("playable");
    }
    if (seeds === 0) {
        pit.classList.add("empty");
    }
    if (state.capturedPits && state.capturedPits.includes(index)) {
        pit.classList.add("captured");
    }

    const seedsSpan = document.createElement("span");
    seedsSpan.className = "pit-seeds";
    seedsSpan.textContent = seeds;
    pit.appendChild(seedsSpan);

    const label = document.createElement("span");
    label.className = "pit-label";
    label.textContent = pitLabel(index);
    pit.appendChild(label);

    if (playable) {
        pit.addEventListener("click", () => playMove(index));
    }

    return pit;
}

function ownsPit(player, index) {
    return player === 0 ? index < 6 : index >= 6;
}

async function startGame() {
    const response = await fetch("/api/game/new", { method: "POST" });
    state = await response.json();
    messageEl.textContent = "";
    render();
}

async function playMove(pitIndex) {
    messageEl.textContent = "";

    const response = await fetch("/api/game/move", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ pit: pitIndex }),
    });

    if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        messageEl.textContent = problem.detail || "Something went wrong.";
        return;
    }

    state = await response.json();

    if (state.capturedPits && state.capturedPits.length > 0) {
        const captured = state.capturedPits.map((i) => pitLabel(i)).join(", ");
        messageEl.textContent = `Captured pit(s): ${captured}!`;
    }

    render();
}

startButton.addEventListener("click", startGame);

startGame();
