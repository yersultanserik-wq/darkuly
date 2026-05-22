package com.dyingdark.core.managers;

/** PATTERN: Singleton */
public class GameConfig {
    private static volatile GameConfig instance;
    private String difficulty = "NORMAL";
    private float difficultyMultiplier = 1.0f;

    private GameConfig() {}

    public static GameConfig getInstance() {
        if (instance == null) {
            synchronized (GameConfig.class) {
                if (instance == null) instance = new GameConfig();
            }
        }
        return instance;
    }

    public void setDifficulty(String d) {
        this.difficulty = d;
        this.difficultyMultiplier = switch (d) {
            case "EASY"   -> 0.7f;
            case "HARD"   -> 1.5f;
            case "INSANE" -> 2.0f;
            default       -> 1.0f;
        };
    }

    public String getDifficulty()          { return difficulty; }
    public float getDifficultyMultiplier() { return difficultyMultiplier; }
}
