const startButton = document.getElementById("start");
const guessForm = document.getElementById("guess-form");
const guessInput = document.getElementById("guess-input");
const guessSubmit = document.getElementById("guess-submit");
const timerEl = document.getElementById("timer");
const timerBarEl = document.getElementById("timer-bar");
const attemptsEl = document.getElementById("attempts");
const errorEl = document.getElementById("error");
const historyEl = document.getElementById("history");
const gameOverEl = document.getElementById("game-over");
const gameOverMessageEl = document.getElementById("game-over-message");
const gameAreaEl = document.getElementById("game-area");
const bootLogEl = document.getElementById("boot-log");

const TIMER_BAR_WIDTH = 24;

let secondsRemaining = 0;
let totalSeconds = 60;
let countdownHandle = null;
let gameActive = false;

function renderTimerBar() {
    const ratio = Math.max(0, Math.min(1, secondsRemaining / totalSeconds));
    const filled = Math.round(ratio * TIMER_BAR_WIDTH);
    timerBarEl.textContent = "[" + "█".repeat(filled) + "░".repeat(TIMER_BAR_WIDTH - filled) + "]";
    timerBarEl.classList.toggle("low", secondsRemaining <= 10);
}

function updateTimerDisplay() {
    timerEl.textContent = secondsRemaining;
    renderTimerBar();
}

function typeLine(text, delayMs) {
    return new Promise((resolve) => {
        const line = document.createElement("div");
        bootLogEl.appendChild(line);
        let i = 0;
        const interval = setInterval(() => {
            line.textContent = text.slice(0, i + 1);
            i++;
            if (i >= text.length) {
                clearInterval(interval);
                setTimeout(resolve, delayMs);
            }
        }, 14);
    });
}

async function runBootSequence() {
    bootLogEl.innerHTML = "";
    const cursor = document.createElement("span");
    cursor.className = "cursor";

    await typeLine("> initializing secure channel...", 150);
    await typeLine("> generating 4-digit cipher...", 150);
    await typeLine("> channel ready. you have 60 seconds.", 250);
    bootLogEl.appendChild(cursor);
}

async function startGame() {
    startButton.hidden = true;
    gameOverEl.hidden = true;
    gameAreaEl.hidden = true;
    historyEl.innerHTML = "";
    attemptsEl.textContent = "0";
    errorEl.textContent = "";

    const response = await fetch("api/game/new", { method: "POST" });
    const data = await response.json();

    totalSeconds = data.timeLimitSeconds;
    secondsRemaining = totalSeconds;

    await runBootSequence();

    gameAreaEl.hidden = false;
    gameActive = true;
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

    const response = await fetch("api/game/guess", {
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
    const line = document.createElement("div");
    line.className = "entry" + (result.won ? " win" : "");
    line.innerHTML = `<span class="prefix">&gt;</span> <span class="guess">${guess}</span> :: <span class="dead">${result.dead} DEAD</span>, <span class="wounded">${result.wounded} WOUNDED</span>`;
    historyEl.appendChild(line);
    historyEl.scrollTop = historyEl.scrollHeight;
}

async function endGame(won, winningGuess) {
    gameActive = false;
    guessInput.disabled = true;
    guessSubmit.disabled = true;
    gameOverEl.hidden = false;
    startButton.hidden = false;
    startButton.textContent = "[ run new_game.sh ]";

    if (won) {
        gameOverMessageEl.textContent = `ACCESS GRANTED — cipher ${winningGuess} confirmed.`;
        return;
    }

    try {
        const response = await fetch("api/game/reveal");
        const data = await response.json();
        gameOverMessageEl.textContent = `CONNECTION TERMINATED — cipher was ${data.secret}.`;
    } catch {
        gameOverMessageEl.textContent = "CONNECTION TERMINATED.";
    }
}

guessInput.addEventListener("input", () => {
    guessInput.value = guessInput.value.replace(/\D/g, "").slice(0, 4);
});

startButton.addEventListener("click", startGame);
guessForm.addEventListener("submit", submitGuess);
