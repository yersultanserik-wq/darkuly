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

import java.util.List;

/**
 * Main game screen with full graphical rendering.
 * Integrates all 8 design patterns.
 */
public class GameScreen implements Screen {

    // ── Constants ──────────────────────────────────────────
    private static final float W = 960, H = 640;
    private static final float TILE = 32f;
    private static final float PLAYER_SIZE = 22f;
    private static final float HUD_H = 80f;
    private static final float ATTACK_RADIUS = 55f;

    // ── Core ───────────────────────────────────────────────
    private final DyingDarkGame game;
    private final GameEventManager events = new GameEventManager();
    private final SaveManager save  = SaveManager.getInstance();
    private final GameConfig config = GameConfig.getInstance();

    // ── Game state ─────────────────────────────────────────
    private Player player;
    private List<Room> rooms;
    private int currentRoomIdx = 0;
    private int depth = 0;
    private int gold  = 0;

    // Artifact bonuses (Decorator pattern — applied to player stats)
    private boolean hasShield  = false;
    private boolean hasSpeed   = false;
    private boolean hasVampire = false;

    // Scroll system
    private int healScrolls   = 1;
    private int shieldScrolls = 0;
    private float shieldTimer = 0;

    // Attack flash
    private float attackFlash  = 0;
    private float hitFlash     = 0;

    // Message log (Observer output)
    private String logMsg   = "";
    private float  logTimer = 0;

    // Game over / victory
    private boolean gameOver   = false;
    private boolean roomClear  = false;
    private float   clearTimer = 0;

    // ── Rendering ──────────────────────────────────────────
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GlyphLayout layout = new GlyphLayout();

    // ── Enemy colors ───────────────────────────────────────
    private static final Color C_GOBLIN   = new Color(0.2f, 0.8f, 0.2f, 1f);
    private static final Color C_SKELETON = new Color(0.9f, 0.9f, 0.8f, 1f);
    private static final Color C_ARCHER   = new Color(0.8f, 0.5f, 0.1f, 1f);
    private static final Color C_BOSS     = new Color(0.8f, 0.0f, 0.8f, 1f);
    private static final Color C_PLAYER   = new Color(0.2f, 0.5f, 1.0f, 1f);

    public GameScreen(DyingDarkGame game) {
        this.game  = game;
        this.font  = new BitmapFont();
        this.smallFont = new BitmapFont();
        font.getData().setScale(1.6f);
        smallFont.getData().setScale(1.2f);

        // PATTERN: Observer — wire listeners
        events.subscribe(EventType.ENEMY_DIED,        (t, d) -> { gold += 10; log("+" + 10 + "g  Enemy down!"); save.addGold(10); });
        events.subscribe(EventType.PLAYER_DAMAGED,    (t, d) -> { hitFlash = 0.25f; log("Took " + d + " damage!"); });
        events.subscribe(EventType.PLAYER_HEALED,     (t, d) -> log("Healed +" + d + " HP"));
        events.subscribe(EventType.ARTIFACT_COLLECTED,(t, d) -> { save.saveArtifact((String)d); log("Artifact: " + d); });
        events.subscribe(EventType.SCROLL_USED,       (t, d) -> { save.saveScroll((String)d);   log("Scroll: " + d); });
        events.subscribe(EventType.LEVEL_CLEARED,     (t, d) -> log("Floor " + depth + " cleared!"));
        events.subscribe(EventType.PLAYER_DIED,       (t, d) -> gameOver = true);

        startRun();
    }

    // ── PATTERN: Facade — startRun ─────────────────────────
    private void startRun() {
        player = new Player(W / 2f, H / 2f - HUD_H / 2f);

        // PATTERN: Decorator — restore permanent artifacts
        for (String art : save.getArtifacts()) applyArtifact(art);

        // PATTERN: Singleton — get difficulty
        float diff = config.getDifficultyMultiplier();
        rooms = new DungeonGenerator().generate(depth, diff);
        currentRoomIdx = 0;
        save.incrementRuns();
        log("Dying Dark — Floor " + depth);
    }

    // PATTERN: Decorator — apply artifact bonuses to player
    private void applyArtifact(String art) {
        switch (art) {
            case "SHIELD"  -> { if (!hasShield)  { hasShield  = true; player.addDefense(30); } }
            case "SPEED"   -> { if (!hasSpeed)   { hasSpeed   = true; player.setSpeed(player.getSpeed() * 1.4f); } }
            case "VAMPIRE" -> { if (!hasVampire) { hasVampire = true; player.addAttack(25); player.addMaxHp(50); } }
        }
    }

    @Override
    public void render(float delta) {
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

        // Room cleared?
        if (room.isCleared() && !roomClear) {
            roomClear = true;
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
                    float diff = config.getDifficultyMultiplier();
                    rooms = new DungeonGenerator().generate(depth, diff);
                    currentRoomIdx = 0;
                    log("Descending to floor " + depth + "!");
                }
            }
        }
    }

    private void handleInput(float delta) {
        float spd = player.getSpeed();
        float nx = player.getX(), ny = player.getY();

        // PATTERN: Command — movement
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    ny += spd * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  ny -= spd * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  nx -= spd * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) nx += spd * delta;

        // Clamp to game area
        float margin = PLAYER_SIZE;
        nx = Math.max(margin, Math.min(W - margin, nx));
        ny = Math.max(HUD_H + margin, Math.min(H - margin, ny));
        player.setX(nx); player.setY(ny);

        // PATTERN: Command — attack
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && player.canAttack()) {
            doPlayerAttack();
        }

        // PATTERN: Command — use scroll
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            useHealScroll();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            useShieldScroll();
        }

        // Difficulty toggle (for demo)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) config.setDifficulty("EASY");
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) config.setDifficulty("NORMAL");
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) config.setDifficulty("HARD");
    }

    // PATTERN: Command — attack command
    private void doPlayerAttack() {
        player.startAttack();
        attackFlash = 0.15f;
        boolean hit = false;
        for (Enemy e : currentRoom().getEnemies()) {
            if (!e.isAlive()) continue;
            float dx = e.getX() - player.getX();
            float dy = e.getY() - player.getY();
            if (Math.sqrt(dx*dx + dy*dy) <= ATTACK_RADIUS) {
                int prevHp = e.getHp();
                e.takeDamage(player.getAttack());
                int dmg = prevHp - e.getHp();
                // PATTERN: Decorator (Vampire) — lifesteal
                if (hasVampire && dmg > 0) {
                    player.heal(dmg / 3);
                    events.publish(EventType.PLAYER_HEALED, dmg / 3);
                }
                if (!e.isAlive()) events.publish(EventType.ENEMY_DIED, e.getName());
                hit = true;
            }
        }
        if (!hit) log("Miss!");
    }

    // PATTERN: Command — scroll commands
    private void useHealScroll() {
        if (healScrolls <= 0) { log("No heal scrolls!"); return; }
        healScrolls--;
        int amount = 50;
        player.heal(amount);
        events.publish(EventType.PLAYER_HEALED, amount);
        events.publish(EventType.SCROLL_USED, "HealScroll");
    }

    private void useShieldScroll() {
        if (shieldScrolls <= 0) { log("No shield scrolls!"); return; }
        shieldScrolls--;
        shieldTimer = 5f;
        events.publish(EventType.SCROLL_USED, "ShieldScroll");
        log("Shield active 5s!");
    }

    private void collectRoomLoot(Room room) {
        for (String loot : room.getLoot()) {
            switch (loot) {
                case "HealScroll"   -> { healScrolls++;  log("Found HealScroll!"); }
                case "ShieldScroll" -> { shieldScrolls++; log("Found ShieldScroll!"); }
                case "ArtifactChest" -> collectRandomArtifact();
            }
        }
    }

    private void collectRandomArtifact() {
        String[] artifacts = {"SHIELD", "SPEED", "VAMPIRE"};
        String art = artifacts[(int)(Math.random() * artifacts.length)];
        applyArtifact(art);
        events.publish(EventType.ARTIFACT_COLLECTED, art);
    }

    // ── DRAW ───────────────────────────────────────────────
    private void draw() {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawFloor();
        drawWalls();
        drawAttackRadius();
        drawEnemies();
        drawPlayer();
        drawHUD();
        drawRoomClearBanner();
    }

    private void drawFloor() {
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Dark floor tiles
        for (int tx = 0; tx < W / TILE; tx++) {
            for (int ty = (int)(HUD_H/TILE); ty < H / TILE; ty++) {
                boolean dark = (tx + ty) % 2 == 0;
                game.shapes.setColor(dark ? 0.09f : 0.11f, dark ? 0.09f : 0.11f, dark ? 0.14f : 0.17f, 1f);
                game.shapes.rect(tx * TILE, ty * TILE, TILE, TILE);
            }
        }
        game.shapes.end();
    }

    private void drawWalls() {
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.15f, 0.1f, 0.1f, 1f);
        // Top, bottom, left, right walls
        game.shapes.rect(0, H - TILE, W, TILE);
        game.shapes.rect(0, HUD_H, W, TILE);
        game.shapes.rect(0, HUD_H, TILE, H - HUD_H);
        game.shapes.rect(W - TILE, HUD_H, TILE, H - HUD_H);
        game.shapes.end();

        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.3f, 0.2f, 0.2f, 1f);
        game.shapes.rect(TILE, HUD_H + TILE, W - 2*TILE, H - HUD_H - 2*TILE);
        game.shapes.end();
    }

    private void drawAttackRadius() {
        if (attackFlash <= 0) return;
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(1f, 0.8f, 0.2f, attackFlash * 4);
        drawCircle(player.getX(), player.getY(), ATTACK_RADIUS, 32);
        game.shapes.end();
    }

    private void drawEnemies() {
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Enemy e : currentRoom().getEnemies()) {
            if (!e.isAlive()) continue;
            float ex = e.getX(), ey = e.getY();
            float size = e.getType().equals("BOSS") ? 28f : 18f;

            Color c = switch (e.getType()) {
                case "GOBLIN"   -> C_GOBLIN;
                case "SKELETON" -> C_SKELETON;
                case "ARCHER"   -> C_ARCHER;
                case "BOSS"     -> C_BOSS;
                default         -> Color.WHITE;
            };
            game.shapes.setColor(c);
            // Body
            game.shapes.rect(ex - size/2, ey - size/2, size, size);
            // HP bar
            float barW = size + 8;
            game.shapes.setColor(0.3f, 0f, 0f, 1f);
            game.shapes.rect(ex - barW/2, ey + size/2 + 4, barW, 5);
            game.shapes.setColor(0.9f, 0.1f, 0.1f, 1f);
            float fill = (float) e.getHp() / e.getMaxHp() * barW;
            game.shapes.rect(ex - barW/2, ey + size/2 + 4, fill, 5);
        }
        game.shapes.end();

        // Enemy labels
        game.batch.begin();
        smallFont.setColor(1f, 1f, 1f, 0.7f);
        for (Enemy e : currentRoom().getEnemies()) {
            if (!e.isAlive()) continue;
            float size = e.getType().equals("BOSS") ? 28f : 18f;
            String label = e.getType().equals("BOSS") ? "BOSS" : e.getType().substring(0, 1);
            smallFont.draw(game.batch, label, e.getX() - 5, e.getY() + size/2 + 3);
        }
        game.batch.end();
    }

    private void drawPlayer() {
        float px = player.getX(), py = player.getY();

        game.shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Shield glow
        if (shieldTimer > 0) {
            game.shapes.setColor(0.2f, 0.5f, 1f, 0.25f);
            // approximate circle with octagon
            game.shapes.rect(px - PLAYER_SIZE - 8, py - PLAYER_SIZE - 8, (PLAYER_SIZE + 8)*2, (PLAYER_SIZE + 8)*2);
        }

        // Hit flash overlay
        Color pc = hitFlash > 0 ? new Color(1f, 0.3f, 0.3f, 1f) : C_PLAYER;
        game.shapes.setColor(pc);
        game.shapes.rect(px - PLAYER_SIZE/2, py - PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE);

        // Artifact indicators
        if (hasShield) {
            game.shapes.setColor(0.3f, 0.7f, 1f, 0.8f);
            game.shapes.rect(px - PLAYER_SIZE/2 - 4, py - PLAYER_SIZE/2, 4, PLAYER_SIZE);
        }
        if (hasVampire) {
            game.shapes.setColor(0.8f, 0.1f, 0.8f, 0.8f);
            game.shapes.rect(px + PLAYER_SIZE/2, py - PLAYER_SIZE/2, 4, PLAYER_SIZE);
        }
        game.shapes.end();

        // Arrow showing attack direction
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(1f, 1f, 0f, 0.6f);
        game.shapes.line(px, py + PLAYER_SIZE/2, px, py + PLAYER_SIZE/2 + 8);
        game.shapes.end();
    }

    private void drawHUD() {
        float hudY = 0;

        // HUD background
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.05f, 0.05f, 0.08f, 1f);
        game.shapes.rect(0, hudY, W, HUD_H);
        game.shapes.setColor(0.2f, 0.1f, 0.1f, 1f);
        game.shapes.rect(0, HUD_H - 2, W, 2);

        // HP bar
        float hpW = 180;
        game.shapes.setColor(0.3f, 0f, 0f, 1f);
        game.shapes.rect(10, 42, hpW, 18);
        float hpFill = (float) player.getHp() / player.getMaxHp() * hpW;
        // Color: green → yellow → red
        float ratio = (float) player.getHp() / player.getMaxHp();
        game.shapes.setColor(1f - ratio, ratio * 0.8f, 0f, 1f);
        game.shapes.rect(10, 42, hpFill, 18);

        // Scroll icons
        for (int i = 0; i < healScrolls; i++) {
            game.shapes.setColor(0.2f, 0.8f, 0.3f, 1f);
            game.shapes.rect(210 + i * 20, 44, 14, 14);
        }
        for (int i = 0; i < shieldScrolls; i++) {
            game.shapes.setColor(0.2f, 0.4f, 1.0f, 1f);
            game.shapes.rect(210 + healScrolls * 20 + i * 20, 44, 14, 14);
        }

        // Artifact indicators in HUD
        int ax = 400;
        if (hasShield)  { game.shapes.setColor(0.3f,0.7f,1f,1f); game.shapes.rect(ax, 44, 14, 14); ax += 22; }
        if (hasSpeed)   { game.shapes.setColor(1f,0.9f,0.1f,1f); game.shapes.rect(ax, 44, 14, 14); ax += 22; }
        if (hasVampire) { game.shapes.setColor(0.8f,0.1f,0.8f,1f); game.shapes.rect(ax, 44, 14, 14); }

        // Room progress dots
        for (int i = 0; i < rooms.size(); i++) {
            boolean done = i < currentRoomIdx;
            boolean cur  = i == currentRoomIdx;
            game.shapes.setColor(done ? 0.2f : (cur ? 0.9f : 0.3f),
                                  done ? 0.7f : (cur ? 0.9f : 0.3f),
                                  0.1f, 1f);
            game.shapes.rect(W - 30 - (rooms.size() - i) * 22, 44, 16, 16);
        }
        game.shapes.end();

        // HUD text
        game.batch.begin();
        font.setColor(1f, 1f, 1f, 1f);
        font.draw(game.batch, "HP  " + player.getHp() + "/" + player.getMaxHp(), 10, 38);

        smallFont.setColor(0.8f, 0.8f, 0.8f, 1f);
        smallFont.draw(game.batch, "Q=Heal(" + healScrolls + ")  E=Shield(" + shieldScrolls + ")  SPACE=Attack", 210, 34);

        // Floor / gold
        smallFont.setColor(1f, 0.85f, 0.2f, 1f);
        smallFont.draw(game.batch, "Floor " + depth + "   Gold " + gold, W - 200, 38);

        // Shield timer
        if (shieldTimer > 0) {
            smallFont.setColor(0.4f, 0.8f, 1f, 1f);
            smallFont.draw(game.batch, "SHIELD " + (int)(shieldTimer + 1) + "s", W/2f - 35, 38);
        }

        // Controls reminder
        smallFont.setColor(0.4f, 0.4f, 0.5f, 1f);
        smallFont.draw(game.batch, "WASD=Move  F1/F2/F3=Difficulty", 10, 16);

        // Log message
        if (logTimer > 0) {
            float alpha = Math.min(1f, logTimer);
            smallFont.setColor(1f, 0.9f, 0.3f, alpha);
            layout.setText(smallFont, logMsg);
            smallFont.draw(game.batch, logMsg, W/2f - layout.width/2f, H - 50);
        }
        game.batch.end();
    }

    private void drawRoomClearBanner() {
        if (!roomClear) return;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0f, 0.5f, 0f, 0.5f);
        game.shapes.rect(W/2f - 180, H/2f - 25, 360, 50);
        game.shapes.end();
        game.batch.begin();
        font.setColor(0.3f, 1f, 0.3f, 1f);
        layout.setText(font, "ROOM CLEARED!");
        font.draw(game.batch, "ROOM CLEARED!", W/2f - layout.width/2f, H/2f + 18);
        game.batch.end();
    }

    private void renderGameOver() {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.04f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.batch.begin();
        font.setColor(0.9f, 0.1f, 0.1f, 1f);
        layout.setText(font, "YOU DIED");
        font.draw(game.batch, "YOU DIED", W/2f - layout.width/2f, H/2f + 60);
        smallFont.setColor(0.7f, 0.7f, 0.7f, 1f);
        layout.setText(smallFont, "Floor " + depth + "   Gold " + gold);
        smallFont.draw(game.batch, "Floor " + depth + "   Gold " + gold, W/2f - layout.width/2f, H/2f);
        layout.setText(smallFont, "ENTER = Play Again    ESC = Menu");
        smallFont.draw(game.batch, "ENTER = Play Again    ESC = Menu", W/2f - layout.width/2f, H/2f - 50);
        game.batch.end();
    }

    private void handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new GameScreen(game));
            dispose();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            dispose();
        }
    }

    // ── Helpers ────────────────────────────────────────────
    private Room currentRoom() { return rooms.get(currentRoomIdx); }

    private void log(String msg) { logMsg = msg; logTimer = 2.5f; }

    private void drawCircle(float cx, float cy, float r, int segments) {
        float angle = 0;
        float step  = 360f / segments;
        for (int i = 0; i < segments; i++) {
            float a1 = (float) Math.toRadians(angle);
            float a2 = (float) Math.toRadians(angle + step);
            game.shapes.line(cx + r*(float)Math.cos(a1), cy + r*(float)Math.sin(a1),
                             cx + r*(float)Math.cos(a2), cy + r*(float)Math.sin(a2));
            angle += step;
        }
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { font.dispose(); smallFont.dispose(); }
}
