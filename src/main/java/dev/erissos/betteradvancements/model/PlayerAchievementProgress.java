package dev.erissos.betteradvancements.model;

import java.time.Instant;

public final class PlayerAchievementProgress {

    private int progress;
    private boolean completed;
    private Instant completedAt;

    public PlayerAchievementProgress() {
    }

    public PlayerAchievementProgress(int progress, boolean completed, Instant completedAt) {
        this.progress = progress;
        this.completed = completed;
        this.completedAt = completedAt;
    }

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

    public Instant getCompletedAt() {
        return completedAt;
    }
}