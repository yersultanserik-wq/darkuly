package com.dyingdark.factories;

import com.dyingdark.entities.Enemy;
import com.dyingdark.entities.Enemy.MoveBehavior;
import com.dyingdark.entities.Enemy.AttackBehavior;

import java.util.Random;

/** PATTERN: Factory Method */
public class EnemyFactory {
    private static int idCounter = 0;
    private final float difficulty;
    private final Random rng = new Random();

    public EnemyFactory(float difficulty) { this.difficulty = difficulty; }

    public Enemy create(String type, float x, float y) {
        String id = type + "_" + (++idCounter);
        return switch (type.toUpperCase()) {
            case "GOBLIN"   -> new Enemy(id, "GOBLIN", x, y,
                    (int)(60*difficulty), (int)(12*difficulty), 4, 10,
                    MoveBehavior.CHASE, AttackBehavior.MELEE, 70f, 40f);
            case "SKELETON" -> new Enemy(id, "SKELETON", x, y,
                    (int)(80*difficulty), (int)(18*difficulty), 8, 15,
                    MoveBehavior.PATROL, AttackBehavior.MELEE, 40f, 40f);
            case "ARCHER"   -> new Enemy(id, "ARCHER", x, y,
                    (int)(50*difficulty), (int)(20*difficulty), 2, 12,
                    MoveBehavior.FLEE, AttackBehavior.RANGED, 55f, 180f);
            case "BOSS"     -> new Enemy(id, "BOSS", x, y,
                    (int)(400*difficulty), (int)(45*difficulty), 20, 100,
                    MoveBehavior.CHASE, AttackBehavior.MELEE, 50f, 50f);
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
