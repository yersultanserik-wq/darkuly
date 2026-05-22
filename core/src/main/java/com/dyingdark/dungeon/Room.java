package com.dyingdark.dungeon;

import com.dyingdark.entities.Enemy;
import com.dyingdark.entities.Player;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private final int id;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<String> loot   = new ArrayList<>();
    private boolean spawnRoom  = false;
    private boolean portalRoom = false;

    public Room(int id) { this.id = id; }

    public void addEnemy(Enemy e) { enemies.add(e); }
    public void addLoot(String l) { loot.add(l); }
    public void setSpawnRoom(boolean v)  { spawnRoom  = v; }
    public void setPortalRoom(boolean v) { portalRoom = v; }

    public void update(float delta, Player player) {
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            e.update(delta, player);
            if (e.canAttack(player)) e.doAttack(player);
        }
    }

    /** Spawn room is always "cleared" (no enemies required) */
    public boolean isCleared() {
        if (spawnRoom) return true;
        return enemies.stream().noneMatch(Enemy::isAlive);
    }

    public int getId()              { return id; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<String> getLoot()   { return loot; }
    public boolean isSpawnRoom()    { return spawnRoom; }
    public boolean isPortalRoom()   { return portalRoom; }
}
