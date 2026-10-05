package dev.erissos.betteradvancements.model;

import java.time.Instant;

public final class PlayerChallengeProgress {

    private int progress;
    private boolean completed;
    private Instant completedAt;
    private String completedCycleKey;

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void complete() {
        this.completed = true;
        this.completedAt = Instant.now();
    }

    public void complete(String cycleKey) {
        complete();
        this.completedCycleKey = cycleKey;
    }

    public void reset() {
        this.progress = 0;
        this.completed = false;
        this.completedAt = null;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant value) { completedAt=value; }

    public String getCompletedCycleKey() {
        return completedCycleKey;
    }

    public void setCompletedCycleKey(String completedCycleKey) {
        this.completedCycleKey = completedCycleKey;
    }

    public PlayerChallengeProgress copy() {
        PlayerChallengeProgress copy = new PlayerChallengeProgress();
        copy.progress = progress; copy.completed = completed; copy.completedAt = completedAt; copy.completedCycleKey = completedCycleKey;
        return copy;
    }
}
