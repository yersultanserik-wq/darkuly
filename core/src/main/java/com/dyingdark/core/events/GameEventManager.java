package com.dyingdark.core.events;

import java.util.*;

/** PATTERN: Observer — central event bus */
public class GameEventManager {

    public enum EventType {
        ENEMY_DIED, PLAYER_DAMAGED, PLAYER_HEALED,
        ARTIFACT_COLLECTED, SCROLL_USED, LEVEL_CLEARED, PLAYER_DIED
    }

    public interface Listener { void onEvent(EventType type, Object data); }

    private final Map<EventType, List<Listener>> map = new EnumMap<>(EventType.class);

    public void subscribe(EventType t, Listener l) {
        map.computeIfAbsent(t, k -> new ArrayList<>()).add(l);
    }

    public void publish(EventType type, Object data) {
        List<Listener> list = map.getOrDefault(type, Collections.emptyList());
        for (Listener l : new ArrayList<>(list)) l.onEvent(type, data);
    }
}
