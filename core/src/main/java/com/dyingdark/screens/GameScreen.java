package com.dyingdark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.Matrix4;
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
    private static final float W = 640, H = 426;
    private static final float TILE = 24f;
    private static final float PLAYER_SIZE = 16f;
    private static final float HUD_H = 58f;

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

    private boolean victoryScreen = false;

    // Portal
    private static final float PORTAL_X = W / 2f, PORTAL_Y = H / 2f - 20;
    private static final float PORTAL_RADIUS = 28f;

    // ── Map type ───────────────────────────────────────────
    private enum MapType { DUNGEON, OVERWORLD }
    private MapType mapType = MapType.DUNGEON;

    // ── Kenney Tile Textures ───────────────────────────────
    // Player tiles by race: Human=98, Elf=112, Orc=109, Necro=84, Dwarf=87
    // Enemy tiles: Ghost=121, Bat=120, Mage=111, Spider=122, Boss=108
    private Texture[] floorTiles;
    private Texture[] wallTiles;
    private boolean tilesLoaded = false;

    // ── Tiny Swords Assets ────────────────────────────────
    // Ground tilemap slices (Tilemap_Flat 640x256, 10x4 grid of 64x64)
    private Texture[] tsGroundTiles;  // grass tiles cut from Tilemap_Flat
    private Texture   tsTreeSpritesheet; // Trees/Tree.png 768x576 (6x6 frames of 128x96)
    private Texture   tsBannerHorizontal;
    private Texture   tsCarvedPanel;
    private Texture   tsRibbonRed, tsRibbonBlue, tsRibbonYellow;
    private Texture[] tsIcons;         // icons 01-10
    private boolean   tsLoaded = false;
    // Pre-cut grass tile regions (from Tilemap_Flat)
    private TextureRegion[] tsGrassRegions;

    // Character sprites (Kenney tiles)
    private Texture[] playerSprites; // indexed by race ordinal
    private Texture[] enemySprites;  // 0=ghost(goblin), 1=bat(skeleton), 2=mage(archer), 3=spider(boss), 4=boss
    private boolean spritesLoaded = false;

    // ── Camera / zoom ──────────────────────────────────────
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    // Virtual world size (smaller = more zoom)
    private static final float VIRTUAL_W = 640f, VIRTUAL_H = 426f;
    // Фиксированная матрица для HUD (не следует за камерой)
    private final Matrix4 hudMatrix = new Matrix4();

    // ── Proto (Human) animated sprite ─────────────────────
    // Spritesheets: 6 frames idle/walk (384x64), 3 frames hurt (192x64), frame size 64x64
    private static final int PROTO_FRAME_W = 64, PROTO_FRAME_H = 64;
    private static final int PROTO_IDLE_FRAMES = 6, PROTO_WALK_FRAMES = 6, PROTO_HURT_FRAMES = 3;
    private static final float PROTO_ANIM_SPEED = 0.1f;

    // Sheets per direction [0=Down, 1=Right, 2=Up]
    private Texture[] protoIdleSheets;  // 3
    private Texture[] protoWalkSheets;  // 3
    private Texture[] protoHurtSheets;  // 3

    // Animation state
    private enum ProtoState { IDLE, WALK, HURT }
    private ProtoState protoState = ProtoState.IDLE;
    private int   protoDir   = 0; // 0=down,1=right,2=up
    private float protoFrame = 0;
    private float prevPlayerX, prevPlayerY;
    private boolean protoLoaded = false;
    private boolean protoFaceLeft = false; // used for flipping when moving left

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
        this.font      = new BitmapFont(); font.getData().setScale(1.0f);
        this.smallFont = new BitmapFont(); smallFont.getData().setScale(0.8f);

        // Camera: virtual 640x426 mapped to screen → ~1.5x zoom vs 960x640
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_W, VIRTUAL_H, camera);
        camera.position.set(VIRTUAL_W / 2f, VIRTUAL_H / 2f, 0);
        camera.update();
        // HUD матрица: фиксированный ortho 640×426 — не двигается с камерой
        hudMatrix.setToOrtho2D(0, 0, VIRTUAL_W, VIRTUAL_H);

        events.subscribe(EventType.ENEMY_DIED,        (t, d) -> { goldRef[0] += 10; log("+10g  Enemy defeated!"); save.addGold(10); });
        events.subscribe(EventType.PLAYER_DAMAGED,    (t, d) -> { hitFlash = 0.25f; log("Damage: -" + d); });
        events.subscribe(EventType.PLAYER_HEALED,     (t, d) -> log("Healed +" + d + " HP"));
        events.subscribe(EventType.ARTIFACT_COLLECTED,(t, d) -> { save.saveArtifact((String)d); log("Artifact: " + d); });
        events.subscribe(EventType.SCROLL_USED,       (t, d) -> { save.saveScroll((String)d); log("Used: " + d); });
        events.subscribe(EventType.LEVEL_CLEARED,     (t, d) -> log("Floor " + depth + " cleared!"));
        events.subscribe(EventType.PLAYER_DIED,       (t, d) -> gameOver = true);

        loadTiles();
        loadTinySwords();
        // Determine map type based on depth (overworld every other floor)
        mapType = (depth % 2 == 1) ? MapType.OVERWORLD : MapType.DUNGEON;
        // Proto (Human) animated spritesheet loading
        try {
            String[] idlePaths = {"assets/proto_idle_down.png","assets/proto_idle_right.png","assets/proto_idle_up.png"};
            String[] walkPaths = {"assets/proto_walk_down.png","assets/proto_walk_right.png","assets/proto_walk_up.png"};
            String[] hurtPaths = {"assets/proto_hurt_down.png","assets/proto_hurt_right.png","assets/proto_hurt_up.png"};
            protoIdleSheets = new Texture[3];
            protoWalkSheets = new Texture[3];
            protoHurtSheets = new Texture[3];
            for (int i = 0; i < 3; i++) {
                protoIdleSheets[i] = new Texture(Gdx.files.internal(idlePaths[i]));
                protoWalkSheets[i] = new Texture(Gdx.files.internal(walkPaths[i]));
                protoHurtSheets[i] = new Texture(Gdx.files.internal(hurtPaths[i]));
            }
            protoLoaded = true;
        } catch (Exception ex) {
            protoLoaded = false;
            Gdx.app.log("GameScreen", "Proto sprites failed: " + ex.getMessage());
        }

        startRun();
    }

    private void loadTiles() {
        // Floor + wall tiles — independent try/catch
        try {
            floorTiles = new Texture[4];
            for (int i = 0; i < 4; i++) {
                String path = "assets/tiles/tile_" + String.format("%04d", i) + ".png";
                if (Gdx.files.internal(path).exists())
                    floorTiles[i] = new Texture(Gdx.files.internal(path));
            }
            wallTiles = new Texture[3];
            for (int i = 0; i < 3; i++) {
                String path = "assets/tiles/tile_" + String.format("%04d", 20 + i) + ".png";
                if (Gdx.files.internal(path).exists())
                    wallTiles[i] = new Texture(Gdx.files.internal(path));
            }
            tilesLoaded = true;
        } catch (Exception ex) {
            tilesLoaded = false;
            Gdx.app.log("GameScreen", "Floor/wall tiles failed: " + ex.getMessage());
        }

        // Character sprites — each loaded individually, no .exists() check needed
        // new Texture() on a missing file throws — caught per-sprite so one miss doesn't kill others
        try {
            // Player sprites by race ordinal: Human=98, Elf=112, Orc=109, Necro=84, Dwarf=87
            int[] playerTileNums = {98, 112, 109, 84, 87};
            playerSprites = new Texture[playerTileNums.length];
            for (int i = 0; i < playerTileNums.length; i++) {
                try {
                    playerSprites[i] = new Texture(Gdx.files.internal(
                        "assets/tiles/tile_" + String.format("%04d", playerTileNums[i]) + ".png"));
                } catch (Exception tex) {
                    Gdx.app.log("GameScreen", "player sprite " + playerTileNums[i] + " failed");
                }
            }
            // Enemy sprites: Ghost(GOBLIN)=121, Bat(SKELETON)=120, Mage(ARCHER)=111, Spider=122, Boss=108
            int[] enemyTileNums = {121, 120, 111, 122, 108};
            enemySprites = new Texture[enemyTileNums.length];
            for (int i = 0; i < enemyTileNums.length; i++) {
                try {
                    enemySprites[i] = new Texture(Gdx.files.internal(
                        "assets/tiles/tile_" + String.format("%04d", enemyTileNums[i]) + ".png"));
                } catch (Exception tex) {
                    Gdx.app.log("GameScreen", "enemy sprite " + enemyTileNums[i] + " failed");
                }
            }
            spritesLoaded = true;
        } catch (Exception ex) {
            spritesLoaded = false;
            Gdx.app.log("GameScreen", "Sprite init failed: " + ex.getMessage());
        }
    }

    private void loadTinySwords() {
        try {
            // Tilemap_Flat: 640×256, 10 columns × 4 rows of 64×64 tiles
            Texture flatMap = new Texture(Gdx.files.internal("assets/tiny_swords/ground_r0_c0.png"));
            flatMap.dispose(); // just a probe; load the full atlas instead
            // Load full Tilemap_Flat as atlas for TextureRegion slicing
            Texture tilemap = new Texture(Gdx.files.internal("assets/tiny_swords/ground_r0_c0.png"));
            // Pre-load individual ground PNGs (already sliced by Python)
            tsGroundTiles = new Texture[8];
            // Row 0 col 0-7: grass variants
            int[][] grassCoords = {{0,0},{0,1},{0,2},{0,3},{0,4},{0,5},{1,0},{1,1}};
            for (int i = 0; i < grassCoords.length; i++) {
                int r = grassCoords[i][0], c = grassCoords[i][1];
                String path = "assets/tiny_swords/ground_r" + r + "_c" + c + ".png";
                if (Gdx.files.internal(path).exists())
                    tsGroundTiles[i] = new Texture(Gdx.files.internal(path));
            }
            tilemap.dispose();

            // UI assets
            if (Gdx.files.internal("assets/tiny_swords/banner_horizontal.png").exists())
                tsBannerHorizontal = new Texture(Gdx.files.internal("assets/tiny_swords/banner_horizontal.png"));
            if (Gdx.files.internal("assets/tiny_swords/carved_panel.png").exists())
                tsCarvedPanel = new Texture(Gdx.files.internal("assets/tiny_swords/carved_panel.png"));
            if (Gdx.files.internal("assets/tiny_swords/ribbon_red.png").exists())
                tsRibbonRed = new Texture(Gdx.files.internal("assets/tiny_swords/ribbon_red.png"));
            if (Gdx.files.internal("assets/tiny_swords/ribbon_blue.png").exists())
                tsRibbonBlue = new Texture(Gdx.files.internal("assets/tiny_swords/ribbon_blue.png"));
            if (Gdx.files.internal("assets/tiny_swords/ribbon_yellow.png").exists())
                tsRibbonYellow = new Texture(Gdx.files.internal("assets/tiny_swords/ribbon_yellow.png"));

            // Icons 01-10
            tsIcons = new Texture[10];
            for (int i = 1; i <= 10; i++) {
                String path = "assets/tiny_swords/icon_" + String.format("%02d", i) + ".png";
                if (Gdx.files.internal(path).exists())
                    tsIcons[i-1] = new Texture(Gdx.files.internal(path));
            }

            tsLoaded = true;
        } catch (Exception ex) {
            tsLoaded = false;
            Gdx.app.log("GameScreen", "TinySwords load failed: " + ex.getMessage());
        }
    }

    private void startRun() {
        player = new Player(W / 2f, HUD_H + 80, race);
        for (String art : save.getArtifacts()) applyArtifact(art);
        float diff = config.getDifficultyMultiplier();
        rooms = new DungeonGenerator().generate(depth, diff);
        currentRoomIdx = 0;
        save.incrementRuns();
        log("Dying Dark — Floor " + (depth + 1) + "/5 | " + race.displayName);
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

        if (victoryScreen) { renderVictory(); handleVictoryInput(); return; }
        if (gameOver) { renderGameOver(); handleGameOverInput(); return; }

        update(delta);

        // Apply camera to batch and shapes
        game.batch.setProjectionMatrix(camera.combined);
        game.shapes.setProjectionMatrix(camera.combined);

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

        // Update proto animation state for HUMAN
        if (race == Race.HUMAN && protoLoaded) {
            float dx = player.getX() - prevPlayerX;
            float dy = player.getY() - prevPlayerY;
            float moved = (float)Math.sqrt(dx*dx + dy*dy);
            if (hitFlash > 0) {
                protoState = ProtoState.HURT;
            } else if (moved > 0.5f) {
                protoState = ProtoState.WALK;
                // determine direction from movement
                if (Math.abs(dy) > Math.abs(dx)) {
                    protoDir = dy > 0 ? 2 : 0; // up or down
                } else {
                    protoDir = 1; // right sheet (flip horizontally for left)
                    protoFaceLeft = dx < 0;
                }
            } else {
                protoState = ProtoState.IDLE;
            }
            int totalFrames = protoState == ProtoState.HURT ? PROTO_HURT_FRAMES : (protoState == ProtoState.WALK ? PROTO_WALK_FRAMES : PROTO_IDLE_FRAMES);
            protoFrame = (protoFrame + delta / PROTO_ANIM_SPEED) % totalFrames;
            prevPlayerX = player.getX();
            prevPlayerY = player.getY();
        }

        Room room = currentRoom();
        room.update(delta, player);

        if (!player.isAlive()) {
            events.publish(EventType.PLAYER_DIED, null);
            gameOver = true;
            return;
        }

        if (room.isCleared() && !roomClear) {
            // Portal room: advance only when player steps into portal
            if (room.isPortalRoom()) {
                float pdx = player.getX() - PORTAL_X;
                float pdy = player.getY() - PORTAL_Y;
                if (Math.sqrt(pdx*pdx + pdy*pdy) < PORTAL_RADIUS) {
                    roomClear  = true;
                    clearTimer = 0.8f;
                    collectRoomLoot(room);
                    log("Entering the portal...");
                }
            } else {
                roomClear  = true;
                clearTimer = 1.5f;
                events.publish(EventType.LEVEL_CLEARED, depth);
                collectRoomLoot(room);
            }
        }

        if (roomClear) {
            clearTimer -= delta;
            if (clearTimer <= 0) {
                roomClear = false;
                currentRoomIdx++;
                if (currentRoomIdx >= rooms.size()) {
                    depth++;
                    if (depth >= DungeonGenerator.MAX_DEPTH) {
                        victoryScreen = true;
                        return;
                    }
                    rooms = new DungeonGenerator().generate(depth, config.getDifficultyMultiplier());
                    mapType = (depth % 2 == 1) ? MapType.OVERWORLD : MapType.DUNGEON;
                    currentRoomIdx = 0;
                    player.setX(W / 2f);
                    player.setY(H / 2f);
                    log("Floor " + (depth + 1) + (mapType == MapType.OVERWORLD ? " — overworld!" : " — deeper into the dark..."));
                    if (depth % 2 == 0) openShop();
                } else {
                    // Reset player to spawn position of new room
                    player.setX(W / 2f);
                    player.setY(HUD_H + 80);
                }
            }
        }

        // ── Камера следует за игроком (lerp, ограничена границами комнаты) ──
        float targetCamX = player.getX();
        float targetCamY = player.getY() + (HUD_H / 2f);
        float halfW = VIRTUAL_W / 2f, halfH = VIRTUAL_H / 2f;
        targetCamX = Math.max(halfW, Math.min(W - halfW, targetCamX));
        targetCamY = Math.max(halfH, Math.min(H - halfH, targetCamY));
        float camLerp = 1f - (float)Math.pow(0.01, delta); // экспоненциальный lerp
        camera.position.x += (targetCamX - camera.position.x) * camLerp;
        camera.position.y += (targetCamY - camera.position.y) * camLerp;
        camera.update();
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
            log("Shop closed. Onward!");
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
        if (!hit) log("Miss!");
    }

    private void useHealPotion() {
        if (healPotions <= 0) { log("No heal potions!"); return; }
        healPotions--;
        int amount = ConsumableType.HEAL_POTION.healAmount;
        player.heal(amount);
        events.publish(EventType.PLAYER_HEALED, amount);
        events.publish(EventType.SCROLL_USED, "HealPotion");
    }

    private void useManaPotion() {
        if (manaPotions <= 0) { log("No mana potions!"); return; }
        manaPotions--;
        player.restoreMana(60);
        log("Mana +60!");
        events.publish(EventType.SCROLL_USED, "ManaPotion");
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
                case "HealScroll"    -> { healPotions++;  log("Found: Heal Potion!"); }
                case "ShieldScroll"  -> { shieldScrolls++; log("Found: Shield Scroll!"); }
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
        if (mapType == MapType.OVERWORLD) {
            Gdx.gl.glClearColor(0.48f, 0.72f, 0.35f, 1f); // bright grass green sky
        } else {
            Gdx.gl.glClearColor(0.04f, 0.04f, 0.08f, 1f);
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawFloor();
        drawWalls();
        // Spawn room safe zone indicator
        if (currentRoom().isSpawnRoom()) drawSpawnZone();
        if (currentRoom().isPortalRoom() && currentRoom().isCleared()) drawPortal();
        drawAttackRadius();
        drawProjectiles();
        drawEnemies();
        drawPlayer();
        drawHUD();
        drawRoomClearBanner();
    }

    // ── FLOOR: use Kenney tiles if loaded, else fallback ──
    private void drawFloor() {
        if (mapType == MapType.OVERWORLD && tsLoaded) {
            drawOverworldFloor();
        } else if (tilesLoaded && floorTiles[0] != null) {
            drawFloorWithTiles();
        } else {
            drawFloorFallback();
        }
    }

    private void drawOverworldFloor() {
        float floorY = HUD_H;
        // Draw grass tiles using Tiny Swords ground tiles
        game.batch.begin();
        game.batch.setColor(1f, 1f, 1f, 1f);
        int cols = (int)(W / TILE);
        int rows = (int)((H - floorY) / TILE) + 1;
        for (int tx = 0; tx < cols; tx++) {
            for (int ty = 0; ty < rows; ty++) {
                // Deterministic tile selection from grass variants
                int hash = ((tx * 7 + ty * 13) ^ (tx * ty)) & 7;
                Texture t = (tsGroundTiles != null && hash < tsGroundTiles.length) ? tsGroundTiles[hash] : null;
                if (t != null) {
                    game.batch.draw(t, tx * TILE, floorY + ty * TILE, TILE, TILE);
                }
            }
        }
        game.batch.end();

        // Subtle day-sky tint overlay to give outdoor feel
        game.shapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.6f, 0.85f, 0.5f, 0.05f);
        game.shapes.rect(0, floorY, W, H - floorY);
        game.shapes.end();
    }

    private void drawFloorWithTiles() {
        float floorY = HUD_H;
        game.batch.begin();
        for (int tx = 0; tx < (int)(W / TILE); tx++) {
            for (int ty = (int)(floorY / TILE); ty < (int)(H / TILE); ty++) {
                int tileIdx = ((tx * 7 + ty * 13) % floorTiles.length);
                Texture t = floorTiles[tileIdx];
                if (t != null) {
                    game.batch.setColor(0.9f, 0.9f, 0.9f, 1f);
                    game.batch.draw(t, tx * TILE, ty * TILE, TILE, TILE);
                }
            }
        }
        game.batch.setColor(Color.WHITE);
        game.batch.end();

        // Subtle neon glow pools on top
        Color pc = RACE_NEON[race.ordinal()];
        float flicker = 0.6f + 0.4f * (float)Math.sin(time * 2.3f);
        float[][] glowCenters = {
            {TILE + 16, floorY + 16}, {W - TILE - 16, floorY + 16},
            {TILE + 16, H - TILE - 16}, {W - TILE - 16, H - TILE - 16}, {W/2f, H/2f},
        };
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int gi = 0; gi < glowCenters.length; gi++) {
            float glowX = glowCenters[gi][0], glowY = glowCenters[gi][1];
            boolean isCenter = gi == 4;
            float r2 = isCenter ? pc.r * 0.04f : 0.35f * flicker;
            float g2 = isCenter ? pc.g * 0.04f : 0.22f * flicker;
            float b2 = isCenter ? pc.b * 0.04f : 0.05f * flicker;
            for (int layer = 6; layer >= 1; layer--) {
                float rad = layer * (isCenter ? 60f : 45f);
                float intensity = (7f - layer) / 7f * 0.4f;
                game.shapes.setColor(r2 * intensity, g2 * intensity, b2 * intensity, 1f);
                game.shapes.rect(glowX - rad, glowY - rad, rad*2, rad*2);
            }
        }
        game.shapes.end();
    }

    private void drawFloorFallback() {
        Color pc = RACE_NEON[race.ordinal()];
        float floorY = HUD_H;
        float floorH = H - floorY;

        // === BASE STONE FLOOR — alternating large stone slabs ===
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int tx = 0; tx < (int)(W / TILE); tx++) {
            for (int ty = (int)(floorY / TILE); ty < (int)(H / TILE); ty++) {
                // Varied stone colors — 4 types based on position hash
                int hash = (tx * 7 + ty * 13) % 8;
                float r, g, b;
                switch (hash) {
                    case 0 -> { r=0.14f; g=0.11f; b=0.10f; } // warm dark stone
                    case 1 -> { r=0.12f; g=0.10f; b=0.12f; } // cold stone
                    case 2 -> { r=0.10f; g=0.10f; b=0.15f; } // blueish
                    case 3 -> { r=0.16f; g=0.12f; b=0.09f; } // brownish
                    default -> { r=0.11f; g=0.10f; b=0.11f; }
                }
                game.shapes.setColor(r, g, b, 1f);
                game.shapes.rect(tx * TILE + 1, ty * TILE + 1, TILE - 2, TILE - 2);
            }
        }
        game.shapes.end();

        // === MORTAR LINES between stone tiles ===
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.06f, 0.05f, 0.05f, 1f);
        for (int tx = 0; tx <= (int)(W / TILE); tx++)
            game.shapes.line(tx*TILE, floorY, tx*TILE, H);
        for (int ty = (int)(floorY/TILE); ty <= (int)(H/TILE); ty++)
            game.shapes.line(0, ty*TILE, W, ty*TILE);
        game.shapes.end();

        // === CRACK DETAILS on some tiles ===
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.07f, 0.06f, 0.06f, 1f);
        // Deterministic cracks based on tile pos
        for (int tx = 1; tx < (int)(W/TILE)-1; tx++) {
            for (int ty = (int)(floorY/TILE)+1; ty < (int)(H/TILE)-1; ty++) {
                int hash = (tx * 31 + ty * 17) % 11;
                float bx = tx * TILE, by = ty * TILE;
                if (hash == 0) {
                    game.shapes.line(bx+8, by+6, bx+20, by+18);
                    game.shapes.line(bx+20, by+18, bx+24, by+26);
                } else if (hash == 1) {
                    game.shapes.line(bx+4, by+20, bx+18, by+14);
                } else if (hash == 2) {
                    game.shapes.line(bx+14, by+4, bx+10, by+22);
                    game.shapes.line(bx+10, by+22, bx+20, by+28);
                }
            }
        }
        game.shapes.end();

        // === STONE RUBBLE / DEBRIS (small filled rects) ===
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        int[][] rubblePositions = {
            {96, (int)floorY+60}, {176, (int)floorY+130}, {800, (int)floorY+80},
            {720, (int)floorY+200}, {400, (int)floorY+50}, {560, (int)floorY+330},
            {130, (int)H-120}, {860, (int)H-90}, {640, (int)H-140},
        };
        for (int[] rp : rubblePositions) {
            game.shapes.setColor(0.18f, 0.14f, 0.12f, 1f);
            game.shapes.rect(rp[0], rp[1], 8, 5);
            game.shapes.rect(rp[0]+12, rp[1]+3, 5, 4);
            game.shapes.rect(rp[0]+5, rp[1]-4, 6, 3);
            game.shapes.setColor(0.12f, 0.10f, 0.09f, 1f);
            game.shapes.rect(rp[0]+2, rp[1]+2, 4, 3);
        }
        game.shapes.end();

        // === SUBTLE NEON GLOW POOLS (torchlight simulation) ===
        float[][] glowCenters = {
            {TILE + 16, floorY + 16},          // top-left torch
            {W - TILE - 16, floorY + 16},       // top-right
            {TILE + 16, H - TILE - 16},          // bottom-left
            {W - TILE - 16, H - TILE - 16},      // bottom-right
            {W/2f, H/2f},                         // center subtle
        };
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        float flicker = 0.6f + 0.4f * (float)Math.sin(time * 2.3f);
        for (int gi = 0; gi < glowCenters.length; gi++) {
            float glowX = glowCenters[gi][0], glowY = glowCenters[gi][1];
            boolean isCenter = gi == 4;
            float r2 = isCenter ? pc.r * 0.04f : 0.35f * flicker;
            float g2 = isCenter ? pc.g * 0.04f : 0.22f * flicker;
            float b2 = isCenter ? pc.b * 0.04f : 0.05f * flicker;
            // Multi-layer radial glow (concentric rects approximation)
            for (int layer = 6; layer >= 1; layer--) {
                float rad = layer * (isCenter ? 60f : 45f);
                float intensity = (7f - layer) / 7f * 0.6f;
                game.shapes.setColor(r2 * intensity, g2 * intensity, b2 * intensity, 1f);
                game.shapes.rect(glowX - rad, glowY - rad, rad*2, rad*2);
            }
        }
        game.shapes.end();
    }

    private void drawWalls() {
        Color pc = RACE_NEON[race.ordinal()];
        float flicker = 0.55f + 0.15f*(float)Math.sin(time * 1.8f);

        if (mapType == MapType.OVERWORLD) {
            drawOverworldBorder(pc, flicker);
            return;
        }

        // === WALL TILES — use Kenney assets if loaded ===
        if (tilesLoaded && wallTiles[0] != null) {
            game.batch.begin();
            game.batch.setColor(0.85f, 0.85f, 0.85f, 1f);
            Texture wt = wallTiles[0];
            // Top wall
            for (int tx = 0; tx < (int)(W/TILE); tx++) game.batch.draw(wt, tx*TILE, H-TILE, TILE, TILE);
            // Bottom wall
            for (int tx = 0; tx < (int)(W/TILE); tx++) game.batch.draw(wt, tx*TILE, HUD_H, TILE, TILE);
            // No left/right wall tiles — avoids door-like gaps on sides
            game.batch.setColor(Color.WHITE);
            game.batch.end();

            // Still draw corners, torches and neon over tiles
            drawPillar(TILE, HUD_H + TILE, pc, flicker);
            drawPillar(W - 2*TILE, HUD_H + TILE, pc, flicker);
            drawPillar(TILE, H - 2*TILE, pc, flicker);
            drawPillar(W - 2*TILE, H - 2*TILE, pc, flicker);
            drawTorch(TILE + 16, H - TILE - 12, flicker);
            drawTorch(W - TILE - 16, H - TILE - 12, flicker);
            drawTorch(TILE + 16, HUD_H + TILE + 12, flicker);
            drawTorch(W - TILE - 16, HUD_H + TILE + 12, flicker);
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            game.shapes.setColor(pc.r*flicker*0.5f, pc.g*flicker*0.5f, pc.b*flicker*0.5f, 1f);
            game.shapes.rect(TILE, HUD_H + TILE, W - 2*TILE, H - HUD_H - 2*TILE);
            game.shapes.end();
            drawWallRunes(pc, flicker);
            return;
        }

        // === WALL STONE BLOCKS (fallback) ===
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Top wall row
        for (int tx = 0; tx < (int)(W/TILE); tx++) {
            int hash = (tx * 11 + 3) % 5;
            float base = 0.12f + hash * 0.015f;
            game.shapes.setColor(base, base * 0.8f, base * 0.7f, 1f);
            game.shapes.rect(tx * TILE + 1, H - TILE + 1, TILE - 2, TILE - 2);
            // Horizontal brick line in middle of tile
            game.shapes.setColor(base * 0.6f, base * 0.5f, base * 0.4f, 1f);
            game.shapes.rect(tx * TILE + 2, H - TILE + TILE/2f, TILE - 4, 2);
        }
        // Bottom wall row (above HUD)
        for (int tx = 0; tx < (int)(W/TILE); tx++) {
            int hash = (tx * 7 + 5) % 5;
            float base = 0.12f + hash * 0.015f;
            game.shapes.setColor(base, base * 0.8f, base * 0.7f, 1f);
            game.shapes.rect(tx * TILE + 1, HUD_H + 1, TILE - 2, TILE - 2);
            game.shapes.setColor(base * 0.6f, base * 0.5f, base * 0.4f, 1f);
            game.shapes.rect(tx * TILE + 2, HUD_H + TILE/2f, TILE - 4, 2);
        }
        // Left wall column
        for (int ty = (int)(HUD_H/TILE)+1; ty < (int)(H/TILE)-1; ty++) {
            int hash = (ty * 13 + 1) % 5;
            float base = 0.11f + hash * 0.015f;
            game.shapes.setColor(base, base * 0.8f, base * 0.7f, 1f);
            game.shapes.rect(1, ty * TILE + 1, TILE - 2, TILE - 2);
            game.shapes.setColor(base * 0.6f, base * 0.5f, base * 0.4f, 1f);
            game.shapes.rect(2, ty * TILE + TILE/2f, TILE/2f, 2);
        }
        // Right wall column
        for (int ty = (int)(HUD_H/TILE)+1; ty < (int)(H/TILE)-1; ty++) {
            int hash = (ty * 9 + 7) % 5;
            float base = 0.11f + hash * 0.015f;
            game.shapes.setColor(base, base * 0.8f, base * 0.7f, 1f);
            game.shapes.rect(W - TILE + 1, ty * TILE + 1, TILE - 2, TILE - 2);
            game.shapes.setColor(base * 0.6f, base * 0.5f, base * 0.4f, 1f);
            game.shapes.rect(W - TILE + 2, ty * TILE + TILE/2f, TILE/2f, 2);
        }
        game.shapes.end();

        // === WALL SHADOW (inner darkening near walls) ===
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int d = 0; d < 4; d++) {
            float alpha = (4f - d) * 0.04f;
            game.shapes.setColor(0f, 0f, 0f, alpha);
            game.shapes.rect(TILE + d, HUD_H + TILE + d, W - 2*TILE - 2*d, H - HUD_H - 2*TILE - 2*d);
        }
        game.shapes.end();

        // === CORNER PILLARS ===
        drawPillar(TILE, HUD_H + TILE, pc, flicker);
        drawPillar(W - 2*TILE, HUD_H + TILE, pc, flicker);
        drawPillar(TILE, H - 2*TILE, pc, flicker);
        drawPillar(W - 2*TILE, H - 2*TILE, pc, flicker);

        // === WALL TORCHES ===
        drawTorch(TILE + 16, H - TILE - 12, flicker);
        drawTorch(W - TILE - 16, H - TILE - 12, flicker);
        drawTorch(TILE + 16, HUD_H + TILE + 12, flicker);
        drawTorch(W - TILE - 16, HUD_H + TILE + 12, flicker);

        // === NEON BORDER OUTLINE (inner glow) ===
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(pc.r*flicker*0.5f, pc.g*flicker*0.5f, pc.b*flicker*0.5f, 1f);
        game.shapes.rect(TILE, HUD_H + TILE, W - 2*TILE, H - HUD_H - 2*TILE);
        game.shapes.setColor(pc.r*flicker*0.2f, pc.g*flicker*0.2f, pc.b*flicker*0.2f, 1f);
        game.shapes.rect(TILE + 2, HUD_H + TILE + 2, W - 2*TILE - 4, H - HUD_H - 2*TILE - 4);
        game.shapes.end();

        // === WALL RUNE CARVINGS ===
        drawWallRunes(pc, flicker);
    }

    /** Overworld map border — treeline fence using Tiny Swords ground tiles as edge markers */
    private void drawOverworldBorder(Color pc, float flicker) {
        // Draw a subtle grassy border / fence effect — no stone walls
        // Bottom border (just above HUD)
        game.shapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.25f, 0.55f, 0.18f, 1f);
        game.shapes.rect(0, HUD_H, W, TILE); // green border strip bottom
        game.shapes.setColor(0.22f, 0.48f, 0.14f, 1f);
        game.shapes.rect(0, H - TILE, W, TILE); // top
        game.shapes.rect(0, HUD_H, TILE, H - HUD_H); // left
        game.shapes.rect(W - TILE, HUD_H, TILE, H - HUD_H); // right
        game.shapes.end();

        // Draw "tree trunk" columns at regular intervals along borders
        game.shapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        int treeSpacing = (int)(TILE * 3);
        // Top and bottom tree lines
        for (int tx = 0; tx < (int)(W / treeSpacing); tx++) {
            float bx = tx * treeSpacing + TILE / 2f;
            drawTree(bx, H - TILE * 0.5f, flicker);
            drawTree(bx, HUD_H + TILE * 0.5f, flicker);
        }
        // Left and right tree lines
        for (int ty = 1; ty < (int)((H - HUD_H) / treeSpacing); ty++) {
            float by = HUD_H + ty * treeSpacing;
            drawTree(TILE * 0.5f, by, flicker);
            drawTree(W - TILE * 0.5f, by, flicker);
        }
        game.shapes.end();

        // Daylight ambient overlay (top sky tint, bottom grass shadow)
        game.shapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.5f, 0.8f, 1.0f, 0.04f);
        game.shapes.rect(TILE, H - TILE * 3, W - 2 * TILE, TILE * 2);
        game.shapes.end();

        // Inner boundary line (soft green)
        game.shapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.3f * flicker, 0.7f * flicker, 0.2f * flicker, 0.8f);
        game.shapes.rect(TILE, HUD_H + TILE, W - 2 * TILE, H - HUD_H - 2 * TILE);
        game.shapes.end();
    }

    /** Simple pixel-art tree: trunk + round canopy */
    private void drawTree(float cx, float cy, float flicker) {
        // Canopy (green circle)
        float canopyR = TILE * 0.55f;
        float sway = (float)Math.sin(time * 1.5f + cx * 0.05f) * 1.5f;
        game.shapes.setColor(0.15f + 0.05f * flicker, 0.5f + 0.1f * flicker, 0.08f, 1f);
        game.shapes.circle(cx + sway, cy + canopyR * 0.6f, canopyR, 12);
        // Highlight
        game.shapes.setColor(0.25f, 0.65f, 0.12f, 0.6f);
        game.shapes.circle(cx - canopyR * 0.2f + sway, cy + canopyR * 0.9f, canopyR * 0.5f, 8);
        // Trunk
        game.shapes.setColor(0.38f, 0.22f, 0.08f, 1f);
        game.shapes.rect(cx - 3, cy - TILE * 0.1f, 6, TILE * 0.5f);
    }

    private void drawPillar(float px, float py, Color pc, float flicker) {
        float pw = TILE, ph = TILE;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Stone base
        game.shapes.setColor(0.18f, 0.14f, 0.12f, 1f);
        game.shapes.rect(px, py, pw, ph);
        // Dark inner
        game.shapes.setColor(0.10f, 0.08f, 0.07f, 1f);
        game.shapes.rect(px + 4, py + 4, pw - 8, ph - 8);
        // Highlight edge
        game.shapes.setColor(0.25f, 0.20f, 0.17f, 1f);
        game.shapes.rect(px, py + ph - 3, pw, 3); // top
        game.shapes.rect(px, py, 3, ph);           // left
        game.shapes.end();
        // Glow
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(pc.r * flicker * 0.4f, pc.g * flicker * 0.4f, pc.b * flicker * 0.4f, 1f);
        game.shapes.rect(px, py, pw, ph);
        game.shapes.end();
    }

    private void drawTorch(float tx, float ty, float flicker) {
        // Torch bracket
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.25f, 0.18f, 0.1f, 1f);
        game.shapes.rect(tx - 3, ty - 14, 6, 14);
        // Torch head
        game.shapes.setColor(0.35f, 0.22f, 0.1f, 1f);
        game.shapes.rect(tx - 4, ty - 16, 8, 4);
        // Flame (outer)
        float fl = flicker;
        game.shapes.setColor(1f, 0.55f * fl, 0.05f, 1f);
        game.shapes.triangle(tx - 6, ty - 14, tx + 6, ty - 14, tx, ty - 14 + 18*fl);
        // Flame (inner bright)
        game.shapes.setColor(1f, 0.9f, 0.3f * fl, 1f);
        game.shapes.triangle(tx - 3, ty - 14, tx + 3, ty - 14, tx, ty - 14 + 10*fl);
        // Spark glow
        game.shapes.setColor(1f, 0.8f, 0.2f, fl * 0.7f);
        game.shapes.circle(tx, ty - 10, 8 * fl, 10);
        game.shapes.end();
        // Light halo rings
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(1f, 0.6f, 0.1f, fl * 0.15f);
        game.shapes.circle(tx, ty - 10, 22 * fl, 14);
        game.shapes.setColor(1f, 0.5f, 0.1f, fl * 0.08f);
        game.shapes.circle(tx, ty - 10, 36 * fl, 16);
        game.shapes.end();
    }

    private void drawWallRunes(Color pc, float flicker) {
        // Small glowing rune carvings on walls
        float[][] runeSpots = {
            {W/2f, H - TILE + 4},
            {W/2f, HUD_H + TILE - 4},
            {TILE - 2, H/2f},
            {W - TILE + 2, H/2f},
        };
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(pc.r * flicker * 0.6f, pc.g * flicker * 0.6f, pc.b * flicker * 0.6f, 1f);
        for (float[] rs : runeSpots) {
            float rx = rs[0], ry = rs[1];
            // Simple rune glyph — lines forming a stylized symbol
            game.shapes.line(rx-8, ry, rx+8, ry);
            game.shapes.line(rx, ry-8, rx, ry+8);
            game.shapes.line(rx-5, ry-5, rx+5, ry+5);
            game.shapes.circle(rx, ry, 10, 8);
        }
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
            float size = boss ? 22f : 14f;

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

        // Enemy type labels removed — sprites now convey identity
    }

    private void drawNeonEnemy(Enemy e, float ex, float ey, float size, Color c, boolean boss) {
        float glow = 0.6f + 0.4f*(float)Math.sin(time*3 + ex);
        String type = e.getType();

        // Try Kenney sprite first
        // enemySprites: 0=Ghost(GOBLIN), 1=Bat(SKELETON), 2=Mage(ARCHER), 3=Spider(spare), 4=Boss(BOSS)
        int sprIdx = switch (type) {
            case "GOBLIN"   -> 0;
            case "SKELETON" -> 1;
            case "ARCHER"   -> 2;
            case "BOSS"     -> 4;
            default         -> -1;
        };

        Texture enemyTex = (enemySprites != null && sprIdx >= 0 && sprIdx < enemySprites.length)
                            ? enemySprites[sprIdx] : null;
        if (enemyTex != null) {
            float ss = size * 2f;
            // Neon glow CIRCLE behind sprite (не квадрат)
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            game.shapes.setColor(c.r * glow, c.g * glow, c.b * glow, 0.8f);
            game.shapes.circle(ex, ey, ss / 2 + 3, 20);
            game.shapes.end();

            game.batch.begin();
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(enemyTex, ex - ss/2, ey - ss/2, ss, ss);
            game.batch.end();
        } else {
            // Fallback vector sprites
            switch (type) {
                case "GOBLIN"   -> drawGoblinSprite(ex, ey, c, glow, size);
                case "SKELETON" -> drawSkeletonSprite(ex, ey, c, glow, size);
                case "ARCHER"   -> drawArcherSprite(ex, ey, c, glow, size);
                case "BOSS"     -> drawBossSprite(ex, ey, c, glow, size);
                default -> {
                    game.shapes.begin(ShapeRenderer.ShapeType.Filled);
                    game.shapes.setColor(c.r*0.15f, c.g*0.15f, c.b*0.15f, 1f);
                    game.shapes.rect(ex-size/2, ey-size/2, size, size);
                    game.shapes.end();
                    game.shapes.begin(ShapeRenderer.ShapeType.Line);
                    game.shapes.setColor(c.r*glow, c.g*glow, c.b*glow, 1f);
                    game.shapes.rect(ex-size/2, ey-size/2, size, size);
                    game.shapes.end();
                }
            }
        }
    }

    /** Goblin: small, hunched, big ears, green */
    private void drawGoblinSprite(float cx, float cy, Color c, float glow, float size) {
        float s = size / 18f; // normalise
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Body (hunched, darker torso)
        game.shapes.setColor(c.r*0.5f, c.g*0.5f, c.b*0.2f, 1f);
        game.shapes.rect(cx - 7*s, cy - 8*s, 14*s, 12*s);
        // Head (round)
        game.shapes.setColor(c.r*glow, c.g*glow, c.b*glow*0.5f, 1f);
        game.shapes.circle(cx, cy + 8*s, 7*s, 12);
        // Big pointy ears
        game.shapes.triangle(cx - 7*s, cy + 11*s,  cx - 14*s, cy + 19*s,  cx - 5*s, cy + 14*s);
        game.shapes.triangle(cx + 7*s, cy + 11*s,  cx + 14*s, cy + 19*s,  cx + 5*s, cy + 14*s);
        // Eyes (red/white glow)
        game.shapes.setColor(1f, 0.2f, 0.1f, 1f);
        game.shapes.circle(cx - 3*s, cy + 9*s, 1.5f*s, 6);
        game.shapes.circle(cx + 3*s, cy + 9*s, 1.5f*s, 6);
        // Legs
        game.shapes.setColor(c.r*0.3f, c.g*0.5f, c.b*0.2f, 1f);
        game.shapes.rect(cx - 6*s, cy - 16*s, 5*s, 9*s);
        game.shapes.rect(cx + 1*s, cy - 16*s, 5*s, 9*s);
        // Weapon (small club)
        game.shapes.setColor(0.6f, 0.4f, 0.2f, 1f);
        game.shapes.rect(cx + 7*s, cy - 6*s, 2*s, 12*s);
        game.shapes.circle(cx + 8*s, cy + 7*s, 3*s, 8);
        game.shapes.end();
        // Neon outline
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(c.r*glow, c.g*glow, c.b*glow*0.6f, 1f);
        game.shapes.circle(cx, cy + 8*s, 7.5f*s, 12);
        game.shapes.rect(cx - 7.5f*s, cy - 8.5f*s, 15*s, 13*s);
        game.shapes.end();
    }

    /** Skeleton: bones, ribs, skull, white-grey */
    private void drawSkeletonSprite(float cx, float cy, Color c, float glow, float size) {
        float s = size / 18f;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Pelvis
        game.shapes.setColor(c.r*0.7f, c.g*0.75f, c.b*0.6f, 1f);
        game.shapes.rect(cx - 7*s, cy - 6*s, 14*s, 4*s);
        // Spine (single rect)
        game.shapes.rect(cx - 1.5f*s, cy - 6*s, 3*s, 16*s);
        // Ribs (3 pairs)
        for (int i = 0; i < 3; i++) {
            float ry = cy + 2*s + i*3*s;
            game.shapes.rect(cx - 7*s, ry, 5*s, 2*s);
            game.shapes.rect(cx + 2*s, ry, 5*s, 2*s);
        }
        // Skull
        game.shapes.setColor(c.r*glow, c.g*glow, c.b*glow, 1f);
        game.shapes.circle(cx, cy + 14*s, 6*s, 14);
        // Jaw (darker)
        game.shapes.setColor(c.r*0.6f, c.g*0.65f, c.b*0.5f, 1f);
        game.shapes.rect(cx - 4*s, cy + 8*s, 8*s, 3*s);
        // Eye sockets (dark holes)
        game.shapes.setColor(0.02f, 0.02f, 0.05f, 1f);
        game.shapes.circle(cx - 2.5f*s, cy + 15*s, 1.8f*s, 8);
        game.shapes.circle(cx + 2.5f*s, cy + 15*s, 1.8f*s, 8);
        // Eye glow
        game.shapes.setColor(c.r*glow, c.g*glow, 0.2f, 1f);
        game.shapes.circle(cx - 2.5f*s, cy + 15*s, 0.9f*s, 6);
        game.shapes.circle(cx + 2.5f*s, cy + 15*s, 0.9f*s, 6);
        // Leg bones
        game.shapes.setColor(c.r*0.7f, c.g*0.75f, c.b*0.6f, 1f);
        game.shapes.rect(cx - 7*s, cy - 15*s, 3*s, 10*s);
        game.shapes.rect(cx + 4*s, cy - 15*s, 3*s, 10*s);
        // Arm bones
        game.shapes.rect(cx - 10*s, cy + 2*s, 4*s, 2*s);
        game.shapes.rect(cx + 6*s,  cy + 2*s, 4*s, 2*s);
        // Sword
        game.shapes.setColor(0.7f, 0.8f, 1f, 1f);
        game.shapes.rect(cx + 9*s, cy - 10*s, 2*s, 20*s);
        game.shapes.rect(cx + 6*s, cy + 7*s, 8*s, 2*s);
        game.shapes.end();
        // Outline
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(c.r*glow, c.g*glow, c.b*glow, 1f);
        game.shapes.circle(cx, cy + 14*s, 6.5f*s, 14);
        game.shapes.end();
    }

    /** Archer: hooded figure with bow */
    private void drawArcherSprite(float cx, float cy, Color c, float glow, float size) {
        float s = size / 18f;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Robe / body
        game.shapes.setColor(c.r*0.4f, c.g*0.35f, 0.05f, 1f);
        game.shapes.rect(cx - 6*s, cy - 8*s, 12*s, 14*s);
        // Cloak flare (wider at bottom)
        game.shapes.triangle(cx - 10*s, cy - 8*s, cx + 10*s, cy - 8*s, cx, cy - 18*s);
        // Head / hood
        game.shapes.setColor(c.r*0.35f, c.g*0.3f, 0.04f, 1f);
        game.shapes.circle(cx, cy + 10*s, 7*s, 12);
        // Hood top (darker triangle)
        game.shapes.setColor(c.r*0.2f, c.g*0.18f, 0.02f, 1f);
        game.shapes.triangle(cx - 7*s, cy + 13*s, cx + 7*s, cy + 13*s, cx, cy + 20*s);
        // Face shadow / eyes
        game.shapes.setColor(c.r*glow, c.g*glow, 0.1f, 1f);
        game.shapes.circle(cx - 2.5f*s, cy + 10*s, 1.5f*s, 6);
        game.shapes.circle(cx + 2.5f*s, cy + 10*s, 1.5f*s, 6);
        // Bow (left side)
        game.shapes.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.6f, 0.4f, 0.15f, 1f);
        float bx = cx - 13*s, by1 = cy + 14*s, by2 = cy - 8*s;
        float prevX2 = bx, prevY2 = by1;
        for (int seg = 1; seg <= 12; seg++) {
            float t = seg / 12f;
            float nx2 = bx + 5*s*(float)Math.sin(Math.PI*t);
            float ny2 = by1 + (by2-by1)*t;
            game.shapes.line(prevX2, prevY2, nx2, ny2); prevX2=nx2; prevY2=ny2;
        }
        // String
        game.shapes.setColor(0.9f, 0.9f, 0.8f, 0.8f);
        game.shapes.line(bx, by1, bx, by2);
        // Arrow nocked
        game.shapes.setColor(c.r*glow, c.g*glow, 0.2f, 1f);
        game.shapes.line(bx, cy + 3*s, cx - 3*s, cy + 3*s);
        game.shapes.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.8f, 0.5f, 0.1f, 1f);
        game.shapes.triangle(cx - 3*s, cy + 5*s, cx - 3*s, cy + 1*s, cx + 1*s, cy + 3*s);
        game.shapes.end();
        // Quiver on back
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.5f, 0.3f, 0.1f, 1f);
        game.shapes.rect(cx + 7*s, cy, 3*s, 10*s);
        for (int i = 0; i < 3; i++) {
            game.shapes.setColor(c.r*glow, c.g*glow, 0.1f, 1f);
            game.shapes.rect(cx + 7.5f*s + i*0.8f*s, cy + 10*s, 1.5f*s, 4*s);
        }
        game.shapes.end();
    }

    /** Boss: massive, horned demon with crown */
    private void drawBossSprite(float cx, float cy, Color c, float glow, float size) {
        float s = size / 28f;
        float pulse = 0.7f + 0.3f*(float)Math.sin(time*5);
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Aura / shadow base
        game.shapes.setColor(c.r*0.08f, c.g*0.04f, c.b*0.12f, 1f);
        game.shapes.circle(cx, cy, 28*s, 20);
        // Wide body
        game.shapes.setColor(c.r*0.3f, c.g*0.08f, c.b*0.3f, 1f);
        game.shapes.rect(cx - 18*s, cy - 16*s, 36*s, 26*s);
        // Muscle shoulders
        game.shapes.circle(cx - 18*s, cy + 10*s, 10*s, 12);
        game.shapes.circle(cx + 18*s, cy + 10*s, 10*s, 12);
        // Arms
        game.shapes.setColor(c.r*0.25f, c.g*0.06f, c.b*0.25f, 1f);
        game.shapes.rect(cx - 28*s, cy - 14*s, 10*s, 24*s);
        game.shapes.rect(cx + 18*s, cy - 14*s, 10*s, 24*s);
        // Clawed hands
        game.shapes.setColor(0.9f, 0.1f, 0.1f, 1f);
        float hx1 = cx - 28*s, hx2 = cx + 28*s, hy = cy - 14*s;
        for (int cl = 0; cl < 3; cl++) {
            game.shapes.triangle(hx1 + cl*3*s, hy, hx1 + cl*3*s + 2*s, hy, hx1 + cl*3*s + 1*s, hy - 5*s);
            game.shapes.triangle(hx2 - cl*3*s - 2*s, hy, hx2 - cl*3*s, hy, hx2 - cl*3*s - 1*s, hy - 5*s);
        }
        // Legs
        game.shapes.setColor(c.r*0.25f, c.g*0.06f, c.b*0.25f, 1f);
        game.shapes.rect(cx - 16*s, cy - 28*s, 12*s, 14*s);
        game.shapes.rect(cx + 4*s,  cy - 28*s, 12*s, 14*s);
        // Hooves
        game.shapes.setColor(0.15f, 0.1f, 0.05f, 1f);
        game.shapes.rect(cx - 17*s, cy - 33*s, 13*s, 6*s);
        game.shapes.rect(cx + 3*s,  cy - 33*s, 13*s, 6*s);
        // Head
        game.shapes.setColor(c.r*0.5f, c.g*0.1f, c.b*0.5f, 1f);
        game.shapes.circle(cx, cy + 20*s, 14*s, 16);
        // Horns
        game.shapes.setColor(0.15f, 0.05f, 0.02f, 1f);
        game.shapes.triangle(cx - 10*s, cy + 28*s, cx - 14*s, cy + 46*s, cx - 6*s, cy + 28*s);
        game.shapes.triangle(cx + 10*s, cy + 28*s, cx + 14*s, cy + 46*s, cx + 6*s, cy + 28*s);
        game.shapes.triangle(cx - 4*s, cy + 30*s, cx - 6*s, cy + 42*s, cx - 1*s, cy + 30*s);
        game.shapes.triangle(cx + 4*s, cy + 30*s, cx + 6*s, cy + 42*s, cx + 1*s, cy + 30*s);
        // Crown / glowing eye sockets
        game.shapes.setColor(1f, 0.8f, 0.1f, 1f);
        for (int pt = 0; pt < 5; pt++) {
            float angle = (float)Math.PI * pt / 4f;
            float kx = cx + 14*s*(float)Math.cos(angle);
            float ky = cy + 26*s + 8*s*(float)Math.sin(angle);
            game.shapes.circle(kx, ky, 1.5f*s, 6);
        }
        // Eyes — glowing
        game.shapes.setColor(1f, 0.2f, 0.9f*pulse, 1f);
        game.shapes.circle(cx - 5*s, cy + 21*s, 3*s, 10);
        game.shapes.circle(cx + 5*s, cy + 21*s, 3*s, 10);
        game.shapes.setColor(1f, 1f, 1f, pulse);
        game.shapes.circle(cx - 5*s, cy + 21*s, 1.2f*s, 6);
        game.shapes.circle(cx + 5*s, cy + 21*s, 1.2f*s, 6);
        // Rune markings on chest
        game.shapes.setColor(c.r*glow, c.g*glow, c.b*glow, 0.6f);
        for (int r2 = 0; r2 < 3; r2++) {
            game.shapes.rect(cx - 8*s + r2*6*s, cy, 3*s, 1.5f*s);
            game.shapes.rect(cx - 8*s + r2*6*s, cy - 5*s, 3*s, 1.5f*s);
        }
        game.shapes.end();
        // Neon border
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(c.r*glow, c.g*glow, c.b*glow, 1f);
        game.shapes.circle(cx, cy + 20*s, 14.5f*s, 16);
        game.shapes.setColor(c.r*glow*0.5f, c.g*glow*0.3f, c.b*glow, 0.7f);
        game.shapes.circle(cx, cy, 30*s, 24);
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
        float spriteSize = PLAYER_SIZE * 2f;

        // Shield pulse aura — только когда активен щит
        if (shieldTimer > 0) {
            float sAlpha = 0.18f + 0.12f*(float)Math.sin(time*6);
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            game.shapes.setColor(0.2f, 0.5f, 1f, sAlpha * 3f);
            game.shapes.circle(px, py, 50, 24);
            game.shapes.end();
            game.shapes.begin(ShapeRenderer.ShapeType.Filled);
            game.shapes.setColor(0.2f, 0.5f, 1f, sAlpha * 0.4f);
            game.shapes.circle(px, py, 50, 24);
            game.shapes.end();
        }

        // ── HUMAN: animated Proto spritesheet ──
        if (race == Race.HUMAN && protoLoaded) {
            // Pick sheet based on state and direction
            Texture[] sheets = switch (protoState) {
                case WALK  -> protoWalkSheets;
                case HURT  -> protoHurtSheets;
                default    -> protoIdleSheets;
            };
            int totalFrames = protoState == ProtoState.HURT ? PROTO_HURT_FRAMES : (protoState == ProtoState.WALK ? PROTO_WALK_FRAMES : PROTO_IDLE_FRAMES);
            int frameIdx = (int) protoFrame % totalFrames;
            Texture sheet = sheets[protoDir];

            // Determine if we need to flip (moving left = use right sheet but flipped)
            boolean flipX = (protoState == ProtoState.WALK && protoDir == 1 && protoFaceLeft);

            TextureRegion frame = new TextureRegion(sheet,
                frameIdx * PROTO_FRAME_W, 0, PROTO_FRAME_W, PROTO_FRAME_H);
            if (flipX) frame.flip(true, false);

            float drawSize = 80f; // крупнее — соразмерен врагам
            float halfSize = drawSize / 2f;

            // Neon glow CIRCLE (не квадрат)
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            Color gc = hitFlash > 0 ? new Color(1f, 0.2f, 0.2f, 1f) : nc;
            game.shapes.setColor(gc.r * glow, gc.g * glow, gc.b * glow, 0.7f);
            game.shapes.circle(px, py, halfSize + 4, 24);
            game.shapes.end();

            game.batch.begin();
            if (hitFlash > 0) game.batch.setColor(1f, 0.5f, 0.5f, 1f);
            else game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(frame, px - halfSize, py - halfSize, drawSize, drawSize);
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.end();

        } else {
            // ── Other races: Kenney tile or fallback vector ──
            int raceIdx = race.ordinal();
            Texture playerTex = (playerSprites != null && raceIdx < playerSprites.length)
                                 ? playerSprites[raceIdx] : null;
            if (playerTex != null) {
                game.shapes.begin(ShapeRenderer.ShapeType.Line);
                Color gc = hitFlash > 0 ? new Color(1f, 0.2f, 0.2f, 1f) : nc;
                game.shapes.setColor(gc.r * glow, gc.g * glow, gc.b * glow, 1f);
                game.shapes.circle(px, py, spriteSize / 2 + 4, 24);
                game.shapes.end();

                game.batch.begin();
                if (hitFlash > 0) game.batch.setColor(1f, 0.4f, 0.4f, 1f);
                else game.batch.setColor(1f, 1f, 1f, 1f);
                game.batch.draw(playerTex, px - spriteSize/2, py - spriteSize/2, spriteSize, spriteSize);
                game.batch.setColor(1f, 1f, 1f, 1f);
                game.batch.end();
            } else {
                game.shapes.begin(ShapeRenderer.ShapeType.Filled);
                Color pc2 = hitFlash > 0 ? new Color(1f,0.2f,0.2f,1f) : new Color(nc.r*0.2f, nc.g*0.2f, nc.b*0.2f, 1f);
                game.shapes.setColor(pc2);
                game.shapes.circle(px, py, PLAYER_SIZE*0.9f, 16);
                game.shapes.end();
                game.shapes.begin(ShapeRenderer.ShapeType.Line);
                game.shapes.setColor(nc.r*glow, nc.g*glow, nc.b*glow, 1f);
                game.shapes.circle(px, py, PLAYER_SIZE*0.9f, 16);
                game.shapes.end();
            }
        }

        // Draw equipped weapon next to player
        drawEquippedWeaponOnPlayer(px, py, nc);

        // Artifact indicators (small dots)
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        int ai = 0;
        if (hasShield)  { game.shapes.setColor(0.3f,0.7f,1f,1f); game.shapes.circle(px-spriteSize/2-8-ai*12, py+spriteSize/2, 4, 8); ai++; }
        if (hasSpeed)   { game.shapes.setColor(1f,0.9f,0.1f,1f); game.shapes.circle(px-spriteSize/2-8-ai*12, py+spriteSize/2, 4, 8); ai++; }
        if (hasVampire) { game.shapes.setColor(0.8f,0.1f,0.8f,1f); game.shapes.circle(px-spriteSize/2-8-ai*12, py+spriteSize/2, 4, 8); }
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

    private void drawSpawnZone() {
        float pulse = 0.4f + 0.2f * (float)Math.sin(time * 2f);
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.1f * pulse, 0.4f * pulse, 0.1f * pulse, 1f);
        game.shapes.circle(W / 2f, (H + HUD_H) / 2f, 55, 24);
        game.shapes.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.2f, 0.9f * pulse, 0.3f, 0.7f);
        game.shapes.circle(W / 2f, (H + HUD_H) / 2f, 55, 24);
        game.shapes.circle(W / 2f, (H + HUD_H) / 2f, 57, 24);
        game.shapes.end();
    }

    private void drawPortal() {
        float pulse = 0.6f + 0.4f * (float)Math.sin(time * 3.5f);
        float spin  = time * 90f; // degrees per second

        // Outer glow rings
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int layer = 5; layer >= 1; layer--) {
            float r = PORTAL_RADIUS + layer * 5f;
            float intensity = (6f - layer) / 6f * 0.18f * pulse;
            game.shapes.setColor(0.5f * intensity, 0.1f * intensity, intensity, 1f);
            game.shapes.circle(PORTAL_X, PORTAL_Y, r, 32);
        }
        // Portal body gradient (concentric filled circles)
        game.shapes.setColor(0.05f, 0f, 0.12f, 1f);
        game.shapes.circle(PORTAL_X, PORTAL_Y, PORTAL_RADIUS, 32);
        game.shapes.setColor(0.2f * pulse, 0.05f, 0.5f * pulse, 1f);
        game.shapes.circle(PORTAL_X, PORTAL_Y, PORTAL_RADIUS * 0.7f, 24);
        game.shapes.setColor(0.6f * pulse, 0.2f * pulse, 1f * pulse, 1f);
        game.shapes.circle(PORTAL_X, PORTAL_Y, PORTAL_RADIUS * 0.4f, 16);
        game.shapes.setColor(1f, 0.9f * pulse, 1f, 1f);
        game.shapes.circle(PORTAL_X, PORTAL_Y, PORTAL_RADIUS * 0.15f, 10);
        game.shapes.end();

        // Spinning rune ring
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.8f * pulse, 0.3f, 1f, 1f);
        game.shapes.circle(PORTAL_X, PORTAL_Y, PORTAL_RADIUS, 32);
        game.shapes.setColor(0.6f * pulse, 0.2f, 0.9f, 1f);
        game.shapes.circle(PORTAL_X, PORTAL_Y, PORTAL_RADIUS + 4, 32);
        // 8 spinning tick marks around the portal
        for (int i = 0; i < 8; i++) {
            float angle = (float)Math.toRadians(spin + i * 45f);
            float innerR = PORTAL_RADIUS + 6;
            float outerR = PORTAL_RADIUS + 12;
            game.shapes.setColor(0.9f * pulse, 0.5f * pulse, 1f, 1f);
            game.shapes.line(
                PORTAL_X + innerR * (float)Math.cos(angle),
                PORTAL_Y + innerR * (float)Math.sin(angle),
                PORTAL_X + outerR * (float)Math.cos(angle),
                PORTAL_Y + outerR * (float)Math.sin(angle)
            );
        }
        game.shapes.end();

        // "PORTAL" label above
        game.batch.begin();
        smallFont.setColor(0.8f, 0.5f * pulse, 1f, pulse);
        float lx = PORTAL_X - 22;
        float ly = PORTAL_Y + PORTAL_RADIUS + 22;
        smallFont.draw(game.batch, "PORTAL", lx, ly);
        game.batch.end();
    }

    private void drawHUD() {
        // Переключаемся на фиксированную матрицу HUD (не следует за камерой)
        game.batch.setProjectionMatrix(hudMatrix);
        game.shapes.setProjectionMatrix(hudMatrix);

        Color nc = RACE_NEON[race.ordinal()];
        float hudY = 0;

        // ── HUD background: stone parchment feel ──────────────
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Base dark background
        game.shapes.setColor(0.10f, 0.08f, 0.05f, 1f);
        game.shapes.rect(0, hudY, W, HUD_H);
        // Warm stone gradient: lighter band near top
        game.shapes.setColor(0.16f, 0.12f, 0.08f, 1f);
        game.shapes.rect(0, HUD_H - 12f, W, 12f);
        game.shapes.end();

        // Top border line — golden parchment color
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.75f, 0.55f, 0.18f, 1f);
        game.shapes.line(0, HUD_H - 1, W, HUD_H - 1);
        game.shapes.setColor(0.45f, 0.32f, 0.10f, 0.6f);
        game.shapes.line(0, HUD_H - 3, W, HUD_H - 3);
        game.shapes.end();

        // ── Tiny Swords ribbon as HP label decoration ─────────
        if (tsLoaded && tsRibbonRed != null) {
            game.batch.begin();
            // Draw ribbon behind HP bar, scaled to fit (ribbon is 192x64, show as 80x20)
            game.batch.setColor(1f, 1f, 1f, 0.9f);
            game.batch.draw(tsRibbonRed, 4, 50, 80, 20);
            game.batch.end();
        }

        // ── HP bar ────────────────────────────────────────────
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

        // ── Consumable icons: use Tiny Swords icons if loaded ──
        game.shapes.end();
        game.batch.begin();
        int cx = 220;
        for (int i = 0; i < healPotions; i++) {
            if (tsLoaded && tsIcons != null && tsIcons[0] != null) {
                game.batch.setColor(0.3f, 1f, 0.4f, 1f);
                game.batch.draw(tsIcons[0], cx + i*18 - 8, 20, 16, 16);
            }
        }
        cx += Math.max(1, healPotions)*18 + 4;
        for (int i = 0; i < shieldScrolls; i++) {
            if (tsLoaded && tsIcons != null && tsIcons[2] != null) {
                game.batch.setColor(0.4f, 0.7f, 1f, 1f);
                game.batch.draw(tsIcons[2], cx + i*18 - 8, 20, 16, 16);
            }
        }
        cx += Math.max(1, shieldScrolls)*18 + 4;
        for (int i = 0; i < manaPotions; i++) {
            if (tsLoaded && tsIcons != null && tsIcons[3] != null) {
                game.batch.setColor(0.7f, 0.4f, 1f, 1f);
                game.batch.draw(tsIcons[3], cx + i*18 - 8, 20, 16, 16);
            }
        }
        game.batch.setColor(Color.WHITE);
        game.batch.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Fallback circles if tsIcons not loaded
        if (!tsLoaded || tsIcons == null) {
            cx = 220;
            for (int i = 0; i < healPotions; i++) { game.shapes.setColor(0.2f, 0.9f, 0.3f, 1f); game.shapes.circle(cx + i*18, 30, 6, 8); }
            cx += healPotions*18 + 10;
            for (int i = 0; i < shieldScrolls; i++) { game.shapes.setColor(0.2f, 0.4f, 1f, 1f); game.shapes.circle(cx + i*18, 30, 6, 8); }
            cx += shieldScrolls*18 + 10;
            for (int i = 0; i < manaPotions; i++) { game.shapes.setColor(0.5f, 0.3f, 1f, 1f); game.shapes.circle(cx + i*18, 30, 6, 8); }
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

        // ── Иконка магазина (сундук) — правый нижний угол HUD ──
        float shopIconX = W - 58f, shopIconY = 8f;
        float pulse2 = 0.7f + 0.3f*(float)Math.sin(time * 2.5f);

        if (tsLoaded && tsIcons != null && tsIcons[5] != null) {
            // Use Tiny Swords icon (icon_06 = coins/gold) as shop icon
            game.shapes.end();
            game.batch.begin();
            game.batch.setColor(pulse2, pulse2, pulse2 * 0.8f, 1f);
            game.batch.draw(tsIcons[5], shopIconX - 2, shopIconY, 30, 30);
            game.batch.end();
            game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        } else {
            // Fallback: draw procedural chest
            // Основание сундука
            game.shapes.setColor(0.55f * pulse2, 0.35f * pulse2, 0.1f, 1f);
            game.shapes.rect(shopIconX, shopIconY + 4, 22, 12);
            // Крышка
            game.shapes.setColor(0.65f * pulse2, 0.45f * pulse2, 0.15f, 1f);
            game.shapes.rect(shopIconX, shopIconY + 15, 22, 6);
            // Замок (жёлтый)
            game.shapes.setColor(1f * pulse2, 0.85f * pulse2, 0.2f, 1f);
            game.shapes.circle(shopIconX + 11, shopIconY + 10, 3f, 8);
        }

        game.shapes.end();

        // Контур сундука (Line поверх) — only for fallback mode
        if (!(tsLoaded && tsIcons != null && tsIcons[5] != null)) {
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            game.shapes.setColor(1f * pulse2, 0.85f * pulse2, 0.2f, 1f);
            game.shapes.rect(shopIconX, shopIconY + 4, 22, 12);
            game.shapes.rect(shopIconX, shopIconY + 15, 22, 6);
            game.shapes.end();
        }

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
            String lbl = switch(s){ case HELMET -> "HLM"; case CHESTPLATE -> "CHE"; case LEGGINGS -> "LEG"; };
            if (ai != null) { smallFont.setColor(nc2.r, nc2.g, nc2.b, 1f); lbl = "T"+ai.tier; }
            else smallFont.setColor(0.35f, 0.35f, 0.4f, 1f);
            smallFont.draw(game.batch, lbl, alx, 50);
            alx += 28;
        }

        // Consumable labels
        smallFont.setColor(0.6f, 0.6f, 0.7f, 1f);
        smallFont.draw(game.batch, "Q=Heal(" + healPotions + ")  E=Shield(" + shieldScrolls + ")  R=Mana(" + manaPotions + ")  TAB=Shop", 220, 16);

        font.setColor(1f, 0.85f, 0.2f, 1f);
        font.draw(game.batch, "Floor " + (depth + 1) + "/5  Gold " + goldRef[0], W-180, 50);

        // TAB label under shop icon
        float pulse3 = 0.7f + 0.3f*(float)Math.sin(time * 2.5f);
        smallFont.setColor(1f * pulse3, 0.85f * pulse3, 0.2f, 1f);
        smallFont.draw(game.batch, "TAB", W - 56, 8);

        // Room type label
        Room curRoom = currentRoom();
        String roomLabel;
        Color roomLabelColor;
        if (curRoom.isSpawnRoom()) { roomLabel = "SAFE ZONE"; roomLabelColor = new Color(0.3f,1f,0.4f,1f); }
        else if (curRoom.isPortalRoom() && curRoom.isCleared()) { roomLabel = "PORTAL ROOM — walk into portal!"; roomLabelColor = new Color(0.8f,0.4f,1f,1f); }
        else if (curRoom.isPortalRoom()) { roomLabel = "PORTAL ROOM — clear enemies!"; roomLabelColor = new Color(0.8f,0.4f,1f,0.8f); }
        else { int combatIdx = currentRoomIdx; roomLabel = "Room " + combatIdx + "/" + (rooms.size()-1); roomLabelColor = new Color(1f,0.85f,0.2f,0.7f); }
        smallFont.setColor(roomLabelColor);
        layout.setText(smallFont, roomLabel);
        smallFont.draw(game.batch, roomLabel, W/2f - layout.width/2f, H - 2);

        if (shieldTimer > 0) {
            smallFont.setColor(0.4f, 0.8f, 1f, 1f);
            smallFont.draw(game.batch, "SHIELD " + (int)(shieldTimer+1) + "s", W/2f-30, 50);
        }

        smallFont.setColor(0.35f, 0.35f, 0.45f, 1f);
        smallFont.draw(game.batch, "WASD=Move  SPACE=Attack", 10, H - 48);

        if (logTimer > 0) {
            float alpha = Math.min(1f, logTimer);
            smallFont.setColor(1f, 0.9f, 0.3f, alpha);
            layout.setText(smallFont, logMsg);
            smallFont.draw(game.batch, logMsg, W/2f - layout.width/2f, H - 55);
        }
        game.batch.end();

        // Возвращаем матрицу камеры
        game.batch.setProjectionMatrix(camera.combined);
        game.shapes.setProjectionMatrix(camera.combined);
    }

    private void drawRoomClearBanner() {
        if (!roomClear) return;
        Room cur = currentRoom();
        int nextRoom = currentRoomIdx + 1;
        boolean isLastRoom = nextRoom >= rooms.size();
        String line1, line2;
        if (cur.isPortalRoom()) {
            line1 = "ENTERING PORTAL!";
            line2 = isLastRoom ? "Descending to next floor..." : "Moving on...";
        } else if (isLastRoom) {
            line1 = "FLOOR CLEARED!";
            line2 = "Descending deeper...";
        } else {
            line1 = "ROOM CLEARED!";
            line2 = "Room " + nextRoom + " / " + (rooms.size() - 1) + " — advancing...";
        }

        float bw = 260, bh = 50;
        float glow = 0.6f + 0.4f*(float)Math.sin(time*6);
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0f, 0.18f, 0.05f, 0.85f);
        game.shapes.rect(W/2f-bw/2, H/2f-bh/2, bw, bh);
        game.shapes.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.2f*glow, 1f*glow, 0.3f*glow, 1f);
        game.shapes.rect(W/2f-bw/2, H/2f-bh/2, bw, bh);
        game.shapes.end();
        game.batch.begin();
        font.setColor(0.3f, 1f, 0.3f, 1f);
        layout.setText(font, line1);
        font.draw(game.batch, line1, W/2f - layout.width/2f, H/2f + 16);
        smallFont.setColor(0.6f, 0.9f, 0.6f, 0.85f);
        layout.setText(smallFont, line2);
        smallFont.draw(game.batch, line2, W/2f - layout.width/2f, H/2f - 6);
        game.batch.end();
    }

    private void renderVictory() {
        Gdx.gl.glClearColor(0.02f, 0.06f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Reset projection to the fixed HUD matrix so coordinates are screen-space
        game.batch.setProjectionMatrix(hudMatrix);
        game.shapes.setProjectionMatrix(hudMatrix);

        float glow = 0.7f + 0.3f*(float)Math.sin(time * 4);

        // Golden radial glow
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int layer = 8; layer >= 1; layer--) {
            float rad = layer * 55f;
            float intensity = (9f - layer) / 9f * 0.12f * glow;
            game.shapes.setColor(intensity * 1f, intensity * 0.85f, 0f, 1f);
            game.shapes.circle(W/2f, H/2f, rad, 32);
        }
        game.shapes.end();

        game.batch.begin();
        font.getData().setScale(2.5f);
        font.setColor(1f, 0.9f, 0.1f, glow);
        layout.setText(font, "VICTORY!");
        font.draw(game.batch, "VICTORY!", W/2f - layout.width/2f, H/2f + 100);
        font.getData().setScale(1.0f);

        font.setColor(0.3f, 1f, 0.4f, 1f);
        layout.setText(font, "You conquered all 5 floors!");
        font.draw(game.batch, layout.toString(), W/2f - layout.width/2f, H/2f + 20);

        smallFont.setColor(0.8f, 0.8f, 0.5f, 1f);
        layout.setText(smallFont, "Gold collected: " + goldRef[0] + "   Race: " + race.displayName);
        smallFont.draw(game.batch, layout.toString(), W/2f - layout.width/2f, H/2f - 30);

        smallFont.setColor(0.6f, 0.6f, 0.6f, 1f);
        layout.setText(smallFont, "ENTER = Play Again    ESC = Main Menu");
        smallFont.draw(game.batch, layout.toString(), W/2f - layout.width/2f, H/2f - 80);
        game.batch.end();
    }

    private void handleVictoryInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            Screen next = new RaceSelectScreen(game);
            dispose();
            game.setScreen(next);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Screen next = new MenuScreen(game);
            dispose();
            game.setScreen(next);
        }
    }

    private void renderGameOver() {
        Gdx.gl.glClearColor(0.02f, 0.01f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Reset projection to fixed HUD matrix so screen-space coords are correct
        game.batch.setProjectionMatrix(hudMatrix);
        game.shapes.setProjectionMatrix(hudMatrix);

        // Neon glitch effect
        float t = (float)Math.sin(time*8) * 3;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.6f, 0f, 0f, 0.08f);
        game.shapes.rect(0, H/2f-60+t, W, 120);
        game.shapes.end();

        game.batch.begin();
        font.getData().setScale(2.5f);
        font.setColor(0.9f, 0.1f, 0.1f, 1f);
        layout.setText(font, "YOU DIED");
        font.draw(game.batch, "YOU DIED", W/2f-layout.width/2f+t, H/2f+80);
        font.getData().setScale(1.0f);

        smallFont.setColor(0.65f, 0.65f, 0.65f, 1f);
        layout.setText(smallFont, "Floor " + depth + "   Gold " + goldRef[0] + "   " + race.displayName);
        smallFont.draw(game.batch, layout.toString(), W/2f-layout.width/2f, H/2f);

        layout.setText(smallFont, "ENTER = Restart    ESC = Menu");
        smallFont.draw(game.batch, layout.toString(), W/2f-layout.width/2f, H/2f-55);
        game.batch.end();
    }

    private void handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            Screen next = new RaceSelectScreen(game);
            dispose();
            game.setScreen(next);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Screen next = new MenuScreen(game);
            dispose();
            game.setScreen(next);
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
    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        font.dispose();
        smallFont.dispose();
        if (floorTiles   != null) for (Texture t : floorTiles)   if (t != null) t.dispose();
        if (wallTiles    != null) for (Texture t : wallTiles)     if (t != null) t.dispose();
        if (playerSprites != null) for (Texture t : playerSprites) if (t != null) t.dispose();
        if (enemySprites  != null) for (Texture t : enemySprites)  if (t != null) t.dispose();
        if (protoIdleSheets != null) for (Texture t : protoIdleSheets) if (t != null) t.dispose();
        if (protoWalkSheets != null) for (Texture t : protoWalkSheets) if (t != null) t.dispose();
        if (protoHurtSheets != null) for (Texture t : protoHurtSheets) if (t != null) t.dispose();
        // Tiny Swords assets
        if (tsGroundTiles != null) for (Texture t : tsGroundTiles) if (t != null) t.dispose();
        if (tsBannerHorizontal != null) tsBannerHorizontal.dispose();
        if (tsCarvedPanel != null) tsCarvedPanel.dispose();
        if (tsRibbonRed != null) tsRibbonRed.dispose();
        if (tsRibbonBlue != null) tsRibbonBlue.dispose();
        if (tsRibbonYellow != null) tsRibbonYellow.dispose();
        if (tsIcons != null) for (Texture t : tsIcons) if (t != null) t.dispose();
    }
}
