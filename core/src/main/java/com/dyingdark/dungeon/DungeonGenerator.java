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
        // Exactly 3 rooms per floor: spawn + 1 combat + portal
        int combatRooms = 1;
        int roomCount   = 3;
        List<Room> rooms = new ArrayList<>();

        for (int i = 0; i < roomCount; i++) {
            Room room = new Room(i);

            if (i == 0) {
                // ── Spawn room: safe, no enemies ──
                room.setSpawnRoom(true);

            } else if (i == roomCount - 1) {
                // ── Portal room: has portal, no boss, a few enemies ──
                room.setPortalRoom(true);
                int count = 1 + rng.nextInt(2 + depth);
                for (int j = 0; j < count; j++) {
                    room.addEnemy(factory.create(pickType(depth), randX(), randY()));
                }
                room.addLoot("HealScroll");

            } else {
                // ── Combat room ──
                int count = 2 + rng.nextInt(2 + depth);
                for (int j = 0; j < count; j++) {
                    room.addEnemy(factory.create(pickType(depth), randX(), randY()));
                }
                // Boss on last combat room (just before portal)
                if (i == roomCount - 2) {
                    room.addEnemy(factory.create("BOSS", 480, 320));
                }
                // Loot
                if (rng.nextFloat() < 0.5f) room.addLoot("HealScroll");
                if (rng.nextFloat() < 0.2f) room.addLoot("ShieldScroll");
                if (rng.nextFloat() < 0.25f) room.addLoot("ArtifactChest");
            }

            rooms.add(room);
        }
        return rooms;
    }

    private String pickType(int depth) {
        if (depth >= 4) return new String[]{"SKELETON","ARCHER","ARCHER"}[rng.nextInt(3)];
        if (depth >= 2) return TYPES[rng.nextInt(TYPES.length)];
        return new String[]{"GOBLIN","GOBLIN","SKELETON"}[rng.nextInt(3)];
    }

    private float randX() { return 150 + rng.nextFloat() * 660; }
    private float randY() { return 140 + rng.nextFloat() * 360; }
}
