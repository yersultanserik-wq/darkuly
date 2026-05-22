package com.dyingdark.core.managers;

import java.util.*;

/** PATTERN: Singleton */
public class SaveManager {
    private static volatile SaveManager instance;

    private final List<String> artifacts = new ArrayList<>();
    private final List<String> scrolls   = new ArrayList<>();
    private int totalRuns = 0;
    private int gold      = 0;

    private SaveManager() {}

    public static SaveManager getInstance() {
        if (instance == null) {
            synchronized (SaveManager.class) {
                if (instance == null) instance = new SaveManager();
            }
        }
        return instance;
    }

    public void saveArtifact(String name) { if (!artifacts.contains(name)) artifacts.add(name); }
    public void saveScroll(String name)   { if (!scrolls.contains(name))   scrolls.add(name); }
    public void addGold(int amount)       { gold += amount; }
    public void incrementRuns()           { totalRuns++; }
    public List<String> getArtifacts()   { return Collections.unmodifiableList(artifacts); }
    public List<String> getScrolls()     { return Collections.unmodifiableList(scrolls); }
    public int getGold()                  { return gold; }
    public int getTotalRuns()             { return totalRuns; }
}
