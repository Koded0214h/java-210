const startButton = document.getElementById("start");
const restartButton = document.getElementById("restart");
const guessForm = document.getElementById("guess-form");
const guessInput = document.getElementById("guess-input");
const guessSubmit = document.getElementById("guess-submit");
const timerEl = document.getElementById("timer");
const attemptsEl = document.getElementById("attempts");
const errorEl = document.getElementById("error");
const historyEl = document.getElementById("history");
const gameOverEl = document.getElementById("game-over");
const gameOverMessageEl = document.getElementById("game-over-message");

let secondsRemaining = 0;
let countdownHandle = null;
let gameActive = false;

async function startGame() {
    const response = await fetch("/api/game/new", { method: "POST" });
    const data = await response.json();

    secondsRemaining = data.timeLimitSeconds;
    gameActive = true;

    historyEl.innerHTML = "";
    attemptsEl.textContent = "0";
    errorEl.textContent = "";
    gameOverEl.hidden = true;
    startButton.hidden = true;

    guessInput.disabled = false;
    guessSubmit.disabled = false;
    guessInput.value = "";
    guessInput.focus();

    updateTimerDisplay();
    clearInterval(countdownHandle);
    countdownHandle = setInterval(tick, 1000);
}

function tick() {
    secondsRemaining--;
    updateTimerDisplay();

    if (secondsRemaining <= 0) {
        clearInterval(countdownHandle);
        endGame(false);
    }
}

function updateTimerDisplay() {
    timerEl.textContent = secondsRemaining;
    timerEl.classList.toggle("low", secondsRemaining <= 10);
}

async function submitGuess(event) {
    event.preventDefault();
    if (!gameActive) {
        return;
    }

    const guess = guessInput.value.trim();
    errorEl.textContent = "";

    if (!/^\d{4}$/.test(guess)) {
        errorEl.textContent = "Enter exactly 4 digits.";
        return;
    }

    const response = await fetch("/api/game/guess", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ guess }),
    });

    if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        errorEl.textContent = problem.detail || "Something went wrong.";
        return;
    }

    const result = await response.json();
    attemptsEl.textContent = result.attempts;
    addHistoryEntry(guess, result);
    guessInput.value = "";
    guessInput.focus();

    if (result.won) {
        clearInterval(countdownHandle);
        endGame(true, guess);
    }
}

function addHistoryEntry(guess, result) {
    const li = document.createElement("li");
    if (result.won) {
        li.classList.add("win");
    }
    li.innerHTML = `
        <span class="guess">${guess}</span>
        <span class="result"><span class="dead">${result.dead} Dead</span>, <span class="wounded">${result.wounded} Wounded</span></span>
    `;
    historyEl.prepend(li);
}

async function endGame(won, winningGuess) {
    gameActive = false;
    guessInput.disabled = true;
    guessSubmit.disabled = true;
    gameOverEl.hidden = false;
    startButton.hidden = true;

    if (won) {
        gameOverMessageEl.textContent = `You cracked it! The code was ${winningGuess}.`;
        return;
    }

    try {
        const response = await fetch("/api/game/reveal");
        const data = await response.json();
        gameOverMessageEl.textContent = `Time's up! The code was ${data.secret}.`;
    } catch {
        gameOverMessageEl.textContent = "Time's up!";
    }
}

guessInput.addEventListener("input", () => {
    guessInput.value = guessInput.value.replace(/\D/g, "").slice(0, 4);
});

startButton.addEventListener("click", startGame);
restartButton.addEventListener("click", startGame);
guessForm.addEventListener("submit", submitGuess);
