package com.dyingdark.dungeon;

import com.dyingdark.factories.EnemyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonGenerator {

    public static final int MAX_DEPTH = 5;

    private static final String[] TYPES = {"GOBLIN", "SKELETON", "ARCHER"};
    private final Random rng = new Random();

    public List<Room> generate(int depth, float difficulty) {
        EnemyFactory factory = new EnemyFactory(difficulty);
        // Rooms per floor: 3 rooms on floor 1, up to 5 on floor 5
        int roomCount = 2 + Math.min(depth + 1, 5);
        List<Room> rooms = new ArrayList<>();

        for (int i = 0; i < roomCount; i++) {
            Room room = new Room(i);
            // More enemies on deeper floors
            int count = 2 + rng.nextInt(2 + depth);
            for (int j = 0; j < count; j++) {
                String type;
                // Higher floors introduce harder enemy mixes
                if (depth >= 4) {
                    // Floor 5: mostly skeletons and archers
                    type = new String[]{"SKELETON","ARCHER","ARCHER"}[rng.nextInt(3)];
                } else if (depth >= 2) {
                    // Floor 3-4: mixed
                    type = TYPES[rng.nextInt(TYPES.length)];
                } else {
                    // Floor 1-2: mostly goblins
                    type = new String[]{"GOBLIN","GOBLIN","SKELETON"}[rng.nextInt(3)];
                }
                float x = 120 + rng.nextFloat() * 720;
                float y = 120 + rng.nextFloat() * 380;
                room.addEnemy(factory.create(type, x, y));
            }

            // Loot chances
            if (rng.nextFloat() < 0.5f) room.addLoot("HealScroll");
            if (rng.nextFloat() < 0.2f) room.addLoot("ShieldScroll");
            if (rng.nextFloat() < 0.25f) room.addLoot("ArtifactChest");

            // Boss on last room of each floor
            if (i == roomCount - 1) {
                room.addEnemy(factory.create("BOSS", 480, 320));
            }

            rooms.add(room);
        }
        return rooms;
    }
}
