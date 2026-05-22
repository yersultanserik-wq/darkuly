package com.dyingdark.dungeon;

import com.dyingdark.factories.EnemyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonGenerator {
    private static final String[] TYPES = {"GOBLIN", "SKELETON", "ARCHER"};
    private final Random rng = new Random();

    public List<Room> generate(int depth, float difficulty) {
        EnemyFactory factory = new EnemyFactory(difficulty);
        int roomCount = 4 + depth;
        List<Room> rooms = new ArrayList<>();

        for (int i = 0; i < roomCount; i++) {
            Room room = new Room(i);
            int count = 2 + rng.nextInt(3 + depth);
            for (int j = 0; j < count; j++) {
                String type = TYPES[rng.nextInt(TYPES.length)];
                float x = 100 + rng.nextFloat() * 760;
                float y = 100 + rng.nextFloat() * 440;
                room.addEnemy(factory.create(type, x, y));
            }
            if (rng.nextFloat() < 0.5f) room.addLoot("HealScroll");
            if (rng.nextFloat() < 0.25f) room.addLoot("ArtifactChest");
            if (i == roomCount - 1) room.addEnemy(factory.create("BOSS", 480, 320));
            rooms.add(room);
        }
        return rooms;
    }
}
