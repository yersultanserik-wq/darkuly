package com.dyingdark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.dyingdark.DyingDarkGame;
import com.dyingdark.core.events.GameEventManager;
import com.dyingdark.core.events.GameEventManager.EventType;
import com.dyingdark.core.managers.GameConfig;
import com.dyingdark.core.managers.SaveManager;
import com.dyingdark.dungeon.DungeonGenerator;
import com.dyingdark.dungeon.Room;
import com.dyingdark.entities.Enemy;
import com.dyingdark.entities.Player;
import com.dyingdark.items.ArmorItem;
import com.dyingdark.items.ArmorSlot;
import com.dyingdark.items.ConsumableType;
import com.dyingdark.items.WeaponType;
import com.dyingdark.races.Race;

import java.util.List;

public class GameScreen implements Screen {

    // ── Constants ──────────────────────────────────────────
    private static final float W = 960, H = 640;
    private static final float TILE = 32f;
    private static final float PLAYER_SIZE = 22f;
    private static final float HUD_H = 88f;

    // ── Core ───────────────────────────────────────────────
    private final DyingDarkGame game;
    private final GameEventManager events = new GameEventManager();
    private final SaveManager save   = SaveManager.getInstance();
    private final GameConfig  config = GameConfig.getInstance();

    // ── Game state ─────────────────────────────────────────
    private Player player;
    private final Race race;
    private List<Room> rooms;
    private int currentRoomIdx = 0;
    private int depth = 0;
    private final int[] goldRef = {0}; // wrapped so ShopScreen can mutate it

    private boolean hasShield  = false;
    private boolean hasSpeed   = false;
    private boolean hasVampire = false;

    private int healPotions   = 1;
    private int manaPotions   = 0;
    private int shieldScrolls = 0;
    private float shieldTimer = 0;

    private float attackFlash = 0;
    private float hitFlash    = 0;

    private String logMsg   = "";
    private float  logTimer = 0;

    private boolean gameOver  = false;
    private boolean roomClear = false;
    private float   clearTimer = 0;

    // Shop state
    private boolean inShop = false;
    private ShopScreen shopScreen;

    // ── Rendering ──────────────────────────────────────────
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GlyphLayout layout = new GlyphLayout();

    // ── Neon palette ───────────────────────────────────────
    private static final Color C_GOBLIN   = new Color(0.1f, 1.0f, 0.3f, 1f);
    private static final Color C_SKELETON = new Color(0.9f, 0.95f, 0.8f, 1f);
    private static final Color C_ARCHER   = new Color(1.0f, 0.6f, 0.1f, 1f);
    private static final Color C_BOSS     = new Color(1.0f, 0.1f, 1.0f, 1f);

    // Race-specific player neon colors
    private static final Color[] RACE_NEON = {
        new Color(0.2f, 0.6f, 1.0f, 1f),  // HUMAN
        new Color(0.2f, 1.0f, 0.5f, 1f),  // ELF
        new Color(1.0f, 0.35f, 0.1f, 1f), // ORC
        new Color(0.7f, 0.2f, 1.0f, 1f),  // NECRO
        new Color(1.0f, 0.85f, 0.2f, 1f), // DWARF
    };

    private float time = 0;

    public GameScreen(DyingDarkGame game, Race race) {
        this.game = game;
        this.race = race;
        this.font      = new BitmapFont(); font.getData().setScale(1.6f);
        this.smallFont = new BitmapFont(); smallFont.getData().setScale(1.2f);

        events.subscribe(EventType.ENEMY_DIED,        (t, d) -> { goldRef[0] += 10; log("+10g  Враг повержен!"); save.addGold(10); });
        events.subscribe(EventType.PLAYER_DAMAGED,    (t, d) -> { hitFlash = 0.25f; log("Урон: -" + d); });
        events.subscribe(EventType.PLAYER_HEALED,     (t, d) -> log("Лечение +" + d + " HP"));
        events.subscribe(EventType.ARTIFACT_COLLECTED,(t, d) -> { save.saveArtifact((String)d); log("Артефакт: " + d); });
        events.subscribe(EventType.SCROLL_USED,       (t, d) -> { save.saveScroll((String)d); log("Использован: " + d); });
        events.subscribe(EventType.LEVEL_CLEARED,     (t, d) -> log("Этаж " + depth + " пройден!"));
        events.subscribe(EventType.PLAYER_DIED,       (t, d) -> gameOver = true);

        startRun();
    }

    private void startRun() {
        player = new Player(W / 2f, H / 2f - HUD_H / 2f, race);
        for (String art : save.getArtifacts()) applyArtifact(art);
        float diff = config.getDifficultyMultiplier();
        rooms = new DungeonGenerator().generate(depth, diff);
        currentRoomIdx = 0;
        save.incrementRuns();
        log("Dying Dark — Этаж " + depth + " | " + race.displayName);
    }

    private void applyArtifact(String art) {
        switch (art) {
            case "SHIELD"  -> { if (!hasShield)  { hasShield  = true; player.addDefense(30); } }
            case "SPEED"   -> { if (!hasSpeed)   { hasSpeed   = true; player.setSpeed(player.getSpeed() * 1.4f); } }
            case "VAMPIRE" -> { if (!hasVampire) { hasVampire = true; player.addAttack(25); player.addMaxHp(50); } }
        }
    }

    @Override
    public void render(float delta) {
        time += delta;

        if (inShop) {
            if (shopScreen != null) shopScreen.render(delta);
            return;
        }

        if (gameOver) { renderGameOver(); handleGameOverInput(); return; }

        update(delta);
        draw();
    }

    // ── UPDATE ─────────────────────────────────────────────
    private void update(float delta) {
        if (attackFlash > 0) attackFlash -= delta;
        if (hitFlash    > 0) hitFlash    -= delta;
        if (logTimer    > 0) logTimer    -= delta;
        if (shieldTimer > 0) shieldTimer -= delta;

        handleInput(delta);
        player.update(delta);

        Room room = currentRoom();
        room.update(delta, player);

        if (!player.isAlive()) {
            events.publish(EventType.PLAYER_DIED, null);
            gameOver = true;
            return;
        }

        if (room.isCleared() && !roomClear) {
            roomClear  = true;
            clearTimer = 1.5f;
            events.publish(EventType.LEVEL_CLEARED, depth);
            collectRoomLoot(room);
        }

        if (roomClear) {
            clearTimer -= delta;
            if (clearTimer <= 0) {
                roomClear = false;
                currentRoomIdx++;
                if (currentRoomIdx >= rooms.size()) {
                    depth++;
                    rooms = new DungeonGenerator().generate(depth, config.getDifficultyMultiplier());
                    currentRoomIdx = 0;
                    log("Спуск на этаж " + depth + "!");
                    // Open shop every 2 floors
                    if (depth % 2 == 0) openShop();
                }
            }
        }
    }

    private void handleInput(float delta) {
        float spd = player.getSpeed();
        float nx = player.getX(), ny = player.getY();
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    ny += spd * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  ny -= spd * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  nx -= spd * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) nx += spd * delta;

        float margin = PLAYER_SIZE;
        nx = Math.max(margin, Math.min(W - margin, nx));
        ny = Math.max(HUD_H + margin, Math.min(H - margin, ny));
        player.setX(nx); player.setY(ny);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && player.canAttack()) doPlayerAttack();
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) useHealPotion();
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) useShieldScroll();
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) useManaPotion();
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) openShop();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) config.setDifficulty("EASY");
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) config.setDifficulty("NORMAL");
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) config.setDifficulty("HARD");
    }

    private void openShop() {
        inShop = true;
        shopScreen = new ShopScreen(game, player, goldRef, () -> {
            inShop = false;
            shopScreen = null;
            log("Магазин закрыт. Вперёд!");
        });
    }

    private void doPlayerAttack() {
        player.startAttack();
        attackFlash = 0.18f;
        float radius = player.getAttackRadius();
        boolean hit = false;
        for (Enemy e : currentRoom().getEnemies()) {
            if (!e.isAlive()) continue;
            float dx = e.getX() - player.getX();
            float dy = e.getY() - player.getY();
            // Ranged weapon uses different radius
            float checkRadius = (player.getEquippedWeapon().range > 0) ? player.getEquippedWeapon().range : radius;
            if (Math.sqrt(dx*dx + dy*dy) <= checkRadius) {
                int prevHp = e.getHp();
                e.takeDamage(player.getAttack());
                int dmg = prevHp - e.getHp();
                if (hasVampire && dmg > 0) {
                    player.heal(dmg / 3);
                    events.publish(EventType.PLAYER_HEALED, dmg / 3);
                }
                if (!e.isAlive()) events.publish(EventType.ENEMY_DIED, e.getName());
                hit = true;
            }
        }
        if (!hit) log("Мимо!");
    }

    private void useHealPotion() {
        if (healPotions <= 0) { log("Нет зелий лечения!"); return; }
        healPotions--;
        int amount = ConsumableType.HEAL_POTION.healAmount;
        player.heal(amount);
        events.publish(EventType.PLAYER_HEALED, amount);
        events.publish(EventType.SCROLL_USED, "HealPotion");
    }

    private void useManaPotion() {
        if (manaPotions <= 0) { log("Нет зелий маны!"); return; }
        manaPotions--;
        player.restoreMana(60);
        log("Мана +60!");
        events.publish(EventType.SCROLL_USED, "ManaPotion");
    }

    private void useShieldScroll() {
        if (shieldScrolls <= 0) { log("Нет свитков щита!"); return; }
        shieldScrolls--;
        shieldTimer = 5f;
        events.publish(EventType.SCROLL_USED, "ShieldScroll");
        log("Щит активен 5с!");
    }

    private void collectRoomLoot(Room room) {
        for (String loot : room.getLoot()) {
            switch (loot) {
                case "HealScroll"    -> { healPotions++;  log("Найдено: Зелье лечения!"); }
                case "ShieldScroll"  -> { shieldScrolls++; log("Найдено: Свиток щита!"); }
                case "ArtifactChest" -> collectRandomArtifact();
            }
        }
    }

    private void collectRandomArtifact() {
        String[] artifacts = {"SHIELD","SPEED","VAMPIRE"};
        String art = artifacts[(int)(Math.random() * artifacts.length)];
        applyArtifact(art);
        events.publish(EventType.ARTIFACT_COLLECTED, art);
    }

    // ── DRAW ───────────────────────────────────────────────
    private void draw() {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawFloor();
        drawWalls();
        drawAttackRadius();
        drawProjectiles();
        drawEnemies();
        drawPlayer();
        drawHUD();
        drawRoomClearBanner();
    }

    // ── FLOOR: dark checkerboard with subtle neon grid ─────
    private void drawFloor() {
        Color pc = RACE_NEON[race.ordinal()];
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int tx = 0; tx < W / TILE; tx++) {
            for (int ty = (int)(HUD_H/TILE); ty < H / TILE; ty++) {
                boolean dark = (tx + ty) % 2 == 0;
                game.shapes.setColor(dark ? 0.07f : 0.09f, dark ? 0.07f : 0.09f, dark ? 0.12f : 0.15f, 1f);
                game.shapes.rect(tx * TILE, ty * TILE, TILE, TILE);
            }
        }
        game.shapes.end();

        // Neon grid lines (very subtle)
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(pc.r * 0.07f, pc.g * 0.07f, pc.b * 0.07f, 1f);
        for (int tx = 0; tx <= W / TILE; tx++) game.shapes.line(tx*TILE, HUD_H, tx*TILE, H);
        for (int ty = (int)(HUD_H/TILE); ty <= H/TILE; ty++) game.shapes.line(0, ty*TILE, W, ty*TILE);
        game.shapes.end();
    }

    private void drawWalls() {
        Color pc = RACE_NEON[race.ordinal()];
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.1f, 0.08f, 0.08f, 1f);
        game.shapes.rect(0, H - TILE, W, TILE);
        game.shapes.rect(0, HUD_H, W, TILE);
        game.shapes.rect(0, HUD_H, TILE, H - HUD_H);
        game.shapes.rect(W - TILE, HUD_H, TILE, H - HUD_H);
        game.shapes.end();

        // Neon wall outline
        float glow = 0.4f + 0.15f*(float)Math.sin(time*1.5f);
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(pc.r*glow, pc.g*glow, pc.b*glow, 1f);
        game.shapes.rect(TILE, HUD_H + TILE, W - 2*TILE, H - HUD_H - 2*TILE);
        // Double glow border
        game.shapes.setColor(pc.r*glow*0.4f, pc.g*glow*0.4f, pc.b*glow*0.4f, 1f);
        game.shapes.rect(TILE+2, HUD_H+TILE+2, W-2*TILE-4, H-HUD_H-2*TILE-4);
        game.shapes.end();
    }

    private void drawAttackRadius() {
        if (attackFlash <= 0) return;
        WeaponType w = player.getEquippedWeapon();
        float r = (w.range > 0) ? w.range : w.attackRadius;
        Color wc = weaponNeonColor(w);
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(wc.r, wc.g, wc.b, attackFlash * 5);
        drawCircle(player.getX(), player.getY(), r, 32);
        // double ring
        game.shapes.setColor(1f, 1f, 1f, attackFlash * 2);
        drawCircle(player.getX(), player.getY(), r + 4, 32);
        game.shapes.end();
    }

    private void drawProjectiles() {
        // Bow/staff ranged visual: draw a small bolt from player toward nearest enemy when attacking
        if (attackFlash <= 0) return;
        WeaponType w = player.getEquippedWeapon();
        if (w.range <= 0) return;

        Enemy target = null;
        float minDist = Float.MAX_VALUE;
        for (Enemy e : currentRoom().getEnemies()) {
            if (!e.isAlive()) continue;
            float dx = e.getX()-player.getX(), dy = e.getY()-player.getY();
            float d = (float)Math.sqrt(dx*dx+dy*dy);
            if (d < minDist) { minDist = d; target = e; }
        }
        if (target == null) return;

        Color wc = weaponNeonColor(w);
        float alpha = attackFlash * 6;
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(wc.r, wc.g, wc.b, alpha);
        game.shapes.line(player.getX(), player.getY(), target.getX(), target.getY());
        game.shapes.end();

        // Impact flash at target
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(wc.r, wc.g, wc.b, alpha * 0.7f);
        game.shapes.circle(target.getX(), target.getY(), 10 * attackFlash, 12);
        game.shapes.end();
    }

    private void drawEnemies() {
        for (Enemy e : currentRoom().getEnemies()) {
            if (!e.isAlive()) continue;
            float ex = e.getX(), ey = e.getY();
            boolean boss = e.getType().equals("BOSS");
            float size = boss ? 28f : 18f;

            Color c = switch (e.getType()) {
                case "GOBLIN"   -> C_GOBLIN;
                case "SKELETON" -> C_SKELETON;
                case "ARCHER"   -> C_ARCHER;
                case "BOSS"     -> C_BOSS;
                default         -> Color.WHITE;
            };

            // Enemy body: neon-outlined shape
            drawNeonEnemy(e, ex, ey, size, c, boss);

            // HP bar
            drawHpBar(ex - (size+4)/2f, ey + size/2f + 5, size+4, 5, e.getHp(), e.getMaxHp(), c);
        }

        // Enemy labels
        game.batch.begin();
        smallFont.setColor(1f, 1f, 1f, 0.8f);
        for (Enemy e : currentRoom().getEnemies()) {
            if (!e.isAlive()) continue;
            float size = e.getType().equals("BOSS") ? 28f : 18f;
            String label = e.getType().equals("BOSS") ? "БОСС" : e.getType().substring(0,1);
            smallFont.draw(game.batch, label, e.getX() - 5, e.getY() + size/2f + 3);
        }
        game.batch.end();
    }

    private void drawNeonEnemy(Enemy e, float ex, float ey, float size, Color c, boolean boss) {
        // Body fill (dark)
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(c.r*0.15f, c.g*0.15f, c.b*0.15f, 1f);
        if (boss) game.shapes.circle(ex, ey, size, 20);
        else      game.shapes.rect(ex-size/2, ey-size/2, size, size);
        game.shapes.end();

        // Neon border
        float glow = 0.6f + 0.4f*(float)Math.sin(time*3 + ex);
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(c.r*glow, c.g*glow, c.b*glow, 1f);
        if (boss) game.shapes.circle(ex, ey, size, 20);
        else      game.shapes.rect(ex-size/2, ey-size/2, size, size);
        // Double border
        game.shapes.setColor(c.r*glow*0.4f, c.g*glow*0.4f, c.b*glow*0.4f, 1f);
        if (boss) game.shapes.circle(ex, ey, size+3, 20);
        else      game.shapes.rect(ex-size/2-2, ey-size/2-2, size+4, size+4);
        game.shapes.end();

        // Eye glow
        float eyeY = boss ? ey+4 : ey+3;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(c.r, c.g, c.b, 0.9f);
        game.shapes.circle(ex-4, eyeY, 2.5f, 8);
        game.shapes.circle(ex+4, eyeY, 2.5f, 8);
        game.shapes.end();
    }

    private void drawHpBar(float x, float y, float w, float h, int hp, int maxHp, Color c) {
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.2f, 0f, 0f, 1f);
        game.shapes.rect(x, y, w, h);
        float fill = (float)hp/maxHp * w;
        game.shapes.setColor(c.r*0.8f, c.g*0.8f, c.b*0.8f, 1f);
        game.shapes.rect(x, y, fill, h);
        game.shapes.end();
    }

    private void drawPlayer() {
        float px = player.getX(), py = player.getY();
        Color nc = RACE_NEON[race.ordinal()];
        float glow = 0.7f + 0.3f*(float)Math.sin(time*4);

        game.shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Shield pulse aura
        if (shieldTimer > 0) {
            float sAlpha = 0.15f + 0.1f*(float)Math.sin(time*6);
            game.shapes.setColor(0.2f, 0.5f, 1f, sAlpha);
            game.shapes.circle(px, py, PLAYER_SIZE + 14, 24);
        }

        // Body fill (dark race color)
        Color pc = hitFlash > 0 ? new Color(1f,0.2f,0.2f,1f) : new Color(nc.r*0.2f, nc.g*0.2f, nc.b*0.2f, 1f);
        game.shapes.setColor(pc);
        game.shapes.circle(px, py, PLAYER_SIZE*0.9f, 16);
        game.shapes.end();

        // Neon border
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(nc.r*glow, nc.g*glow, nc.b*glow, 1f);
        game.shapes.circle(px, py, PLAYER_SIZE*0.9f, 16);
        game.shapes.setColor(nc.r*glow*0.3f, nc.g*glow*0.3f, nc.b*glow*0.3f, 1f);
        game.shapes.circle(px, py, PLAYER_SIZE*0.9f+3, 16);
        game.shapes.end();

        // Draw equipped weapon next to player
        drawEquippedWeaponOnPlayer(px, py, nc);

        // Artifact indicators (small dots)
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        int ai = 0;
        if (hasShield)  { game.shapes.setColor(0.3f,0.7f,1f,1f); game.shapes.circle(px-PLAYER_SIZE-8-ai*12, py+PLAYER_SIZE, 4, 8); ai++; }
        if (hasSpeed)   { game.shapes.setColor(1f,0.9f,0.1f,1f); game.shapes.circle(px-PLAYER_SIZE-8-ai*12, py+PLAYER_SIZE, 4, 8); ai++; }
        if (hasVampire) { game.shapes.setColor(0.8f,0.1f,0.8f,1f); game.shapes.circle(px-PLAYER_SIZE-8-ai*12, py+PLAYER_SIZE, 4, 8); }
        game.shapes.end();
    }

    private void drawEquippedWeaponOnPlayer(float px, float py, Color nc) {
        WeaponType w = player.getEquippedWeapon();
        float wx = px + PLAYER_SIZE + 5;
        float wy = py;
        Color wc = weaponNeonColor(w);
        float glow = (attackFlash > 0) ? 1f : 0.55f;

        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(wc.r*glow, wc.g*glow, wc.b*glow, 1f);

        switch (w.shape) {
            case CROSS  -> {
                game.shapes.rect(wx+2, wy-14, 4, 28);
                game.shapes.rect(wx-4, wy+4, 18, 4);
            }
            case AXE    -> {
                game.shapes.triangle(wx, wy+14, wx+16, wy+14, wx+8, wy-10);
                game.shapes.rect(wx+5, wy-18, 3, 10);
            }
            case BOW    -> {
                game.shapes.end();
                game.shapes.begin(ShapeRenderer.ShapeType.Line);
                game.shapes.setColor(wc.r*glow, wc.g*glow, wc.b*glow, 1f);
                for (int i=0;i<10;i++) {
                    float t1 = i/10f, t2=(i+1)/10f;
                    game.shapes.line(
                        wx+(float)Math.sin(t1*Math.PI)*8, wy-10+t1*20,
                        wx+(float)Math.sin(t2*Math.PI)*8, wy-10+t2*20);
                }
                game.shapes.line(wx, wy-10, wx, wy+10);
            }
            case STAFF  -> {
                game.shapes.rect(wx+5, wy-18, 3, 36);
                game.shapes.circle(wx+6, wy+22, 6, 10);
            }
            case DAGGER -> {
                game.shapes.rect(wx+4, wy-12, 3, 24);
                game.shapes.triangle(wx+3, wy+12, wx+8, wy+12, wx+5, wy+22);
            }
        }
        game.shapes.end();
    }

    private Color weaponNeonColor(WeaponType w) {
        return switch (w) {
            case SWORD  -> new Color(0.4f, 0.8f, 1.0f, 1f);
            case AXE    -> new Color(1.0f, 0.4f, 0.1f, 1f);
            case BOW    -> new Color(0.3f, 1.0f, 0.4f, 1f);
            case STAFF  -> new Color(0.8f, 0.3f, 1.0f, 1f);
            case DAGGER -> new Color(1.0f, 0.9f, 0.2f, 1f);
        };
    }

    private void drawHUD() {
        Color nc = RACE_NEON[race.ordinal()];
        float hudY = 0;

        // HUD background
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.04f, 0.04f, 0.07f, 1f);
        game.shapes.rect(0, hudY, W, HUD_H);
        game.shapes.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(nc.r*0.5f, nc.g*0.5f, nc.b*0.5f, 1f);
        game.shapes.rect(0, HUD_H-1, W, 1);
        game.shapes.end();

        // HP bar
        float hpW = 200;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.2f, 0f, 0f, 1f);
        game.shapes.rect(10, 54, hpW, 18);
        float hpRatio = (float)player.getHp() / player.getMaxHp();
        game.shapes.setColor(1f-hpRatio, hpRatio*0.85f, 0f, 1f);
        game.shapes.rect(10, 54, hpRatio*hpW, 18);

        // Mana bar (for NECRO shows bigger, others smaller)
        float manaW = 120;
        float manaRatio = (float)player.getMana()/player.getMaxMana();
        game.shapes.setColor(0.05f, 0.05f, 0.2f, 1f);
        game.shapes.rect(10, 35, manaW, 10);
        game.shapes.setColor(0.3f, 0.4f, 1f, 1f);
        game.shapes.rect(10, 35, manaRatio*manaW, 10);

        // Armor slots display
        int armorX = 220;
        for (ArmorSlot s : ArmorSlot.values()) {
            ArmorItem ai = player.getArmor(s);
            Color slotC = ai != null ? new Color(nc.r*0.8f, nc.g*0.8f, nc.b*0.8f, 1f) : new Color(0.2f,0.2f,0.2f,1f);
            game.shapes.setColor(slotC);
            game.shapes.rect(armorX, 52, 20, 22);
            armorX += 28;
        }

        // Consumable count icons
        int cx = 220;
        for (int i = 0; i < healPotions; i++) {
            game.shapes.setColor(0.2f, 0.9f, 0.3f, 1f);
            game.shapes.circle(cx + i*18, 30, 6, 8);
        }
        cx += healPotions*18 + 10;
        for (int i = 0; i < shieldScrolls; i++) {
            game.shapes.setColor(0.2f, 0.4f, 1f, 1f);
            game.shapes.circle(cx + i*18, 30, 6, 8);
        }
        cx += shieldScrolls*18 + 10;
        for (int i = 0; i < manaPotions; i++) {
            game.shapes.setColor(0.5f, 0.3f, 1f, 1f);
            game.shapes.circle(cx + i*18, 30, 6, 8);
        }

        // Room progress
        for (int i = 0; i < rooms.size(); i++) {
            boolean done = i < currentRoomIdx;
            boolean cur  = i == currentRoomIdx;
            game.shapes.setColor(done ? nc.r*0.4f : (cur ? nc.r : 0.25f),
                                  done ? nc.g*0.4f : (cur ? nc.g : 0.25f),
                                  done ? nc.b*0.4f : (cur ? nc.b : 0.25f), 1f);
            game.shapes.circle(W - 30 - (rooms.size()-i)*22, 68, cur ? 8 : 5, 10);
        }

        game.shapes.end();

        // HUD text
        game.batch.begin();
        font.setColor(1f, 1f, 1f, 1f);
        font.draw(game.batch, "HP " + player.getHp() + "/" + player.getMaxHp(), 10, 50);

        smallFont.setColor(0.5f, 0.6f, 1f, 0.85f);
        smallFont.draw(game.batch, "MP " + player.getMana() + "/" + player.getMaxMana(), 10, 32);

        // Race + weapon
        Color nc2 = RACE_NEON[race.ordinal()];
        smallFont.setColor(nc2.r, nc2.g, nc2.b, 0.9f);
        smallFont.draw(game.batch, race.displayName, 10, 16);

        smallFont.setColor(weaponNeonColor(player.getEquippedWeapon()));
        smallFont.draw(game.batch, player.getEquippedWeapon().displayName, 10+70, 16);

        // Armor labels
        int alx = 222;
        smallFont.setColor(0.6f, 0.6f, 0.7f, 1f);
        for (ArmorSlot s : ArmorSlot.values()) {
            ArmorItem ai = player.getArmor(s);
            String lbl = switch(s){ case HELMET -> "Шл"; case CHESTPLATE -> "Нгд"; case LEGGINGS -> "Пнж"; };
            if (ai != null) { smallFont.setColor(nc2.r, nc2.g, nc2.b, 1f); lbl = "T"+ai.tier; }
            else smallFont.setColor(0.35f, 0.35f, 0.4f, 1f);
            smallFont.draw(game.batch, lbl, alx, 50);
            alx += 28;
        }

        // Consumable labels
        smallFont.setColor(0.6f, 0.6f, 0.7f, 1f);
        smallFont.draw(game.batch, "Q=Зелье(" + healPotions + ")  E=Щит(" + shieldScrolls + ")  R=Мана(" + manaPotions + ")  TAB=Магазин", 220, 16);

        font.setColor(1f, 0.85f, 0.2f, 1f);
        font.draw(game.batch, "Этаж " + depth + "  Золото " + goldRef[0], W-240, 50);

        if (shieldTimer > 0) {
            smallFont.setColor(0.4f, 0.8f, 1f, 1f);
            smallFont.draw(game.batch, "ЩИТ " + (int)(shieldTimer+1) + "с", W/2f-30, 50);
        }

        smallFont.setColor(0.35f, 0.35f, 0.45f, 1f);
        smallFont.draw(game.batch, "WASD=Движение  SPACE=Атака", 10, H - 48);

        if (logTimer > 0) {
            float alpha = Math.min(1f, logTimer);
            smallFont.setColor(1f, 0.9f, 0.3f, alpha);
            layout.setText(smallFont, logMsg);
            smallFont.draw(game.batch, logMsg, W/2f - layout.width/2f, H - 55);
        }
        game.batch.end();
    }

    private void drawRoomClearBanner() {
        if (!roomClear) return;
        float bw = 340, bh = 52;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0f, 0.3f, 0f, 0.7f);
        game.shapes.rect(W/2f-bw/2, H/2f-bh/2, bw, bh);
        game.shapes.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.2f, 1f, 0.3f, 0.8f);
        game.shapes.rect(W/2f-bw/2, H/2f-bh/2, bw, bh);
        game.shapes.end();
        game.batch.begin();
        font.setColor(0.3f, 1f, 0.3f, 1f);
        layout.setText(font, "КОМНАТА ЗАЧИЩЕНА!");
        font.draw(game.batch, "КОМНАТА ЗАЧИЩЕНА!", W/2f - layout.width/2f, H/2f + 18);
        game.batch.end();
    }

    private void renderGameOver() {
        Gdx.gl.glClearColor(0.02f, 0.01f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Neon glitch effect
        float t = (float)Math.sin(time*8) * 3;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.6f, 0f, 0f, 0.08f);
        game.shapes.rect(0, H/2f-60+t, W, 120);
        game.shapes.end();

        game.batch.begin();
        font.getData().setScale(4f);
        font.setColor(0.9f, 0.1f, 0.1f, 1f);
        layout.setText(font, "ВЫ МЕРТВЫ");
        font.draw(game.batch, "ВЫ МЕРТВЫ", W/2f-layout.width/2f+t, H/2f+80);
        font.getData().setScale(1.6f);

        smallFont.setColor(0.65f, 0.65f, 0.65f, 1f);
        layout.setText(smallFont, "Этаж " + depth + "   Золото " + goldRef[0] + "   " + race.displayName);
        smallFont.draw(game.batch, layout.toString(), W/2f-layout.width/2f, H/2f);

        layout.setText(smallFont, "ENTER = Сначала    ESC = Меню");
        smallFont.draw(game.batch, layout.toString(), W/2f-layout.width/2f, H/2f-55);
        game.batch.end();
    }

    private void handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new RaceSelectScreen(game));
            dispose();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            dispose();
        }
    }

    // ── Helpers ────────────────────────────────────────────
    private Room currentRoom() { return rooms.get(currentRoomIdx); }
    private void log(String m) { logMsg = m; logTimer = 2.5f; }

    private void drawCircle(float cx, float cy, float r, int seg) {
        float angle = 0, step = 360f/seg;
        for (int i = 0; i < seg; i++) {
            float a1=(float)Math.toRadians(angle), a2=(float)Math.toRadians(angle+step);
            game.shapes.line(cx+r*(float)Math.cos(a1), cy+r*(float)Math.sin(a1),
                             cx+r*(float)Math.cos(a2), cy+r*(float)Math.sin(a2));
            angle+=step;
        }
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { font.dispose(); smallFont.dispose(); }
}
