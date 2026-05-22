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
import com.dyingdark.entities.Player;
import com.dyingdark.items.*;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen implements Screen {

    private static final float W = 960, H = 640;
    private static final Color GOLD_COLOR  = new Color(1f, 0.85f, 0.2f, 1f);
    private static final Color GREEN_COLOR = new Color(0.2f, 0.9f, 0.4f, 1f);
    private static final Color RED_COLOR   = new Color(0.9f, 0.2f, 0.2f, 1f);

    private final DyingDarkGame game;
    private final Player player;
    private int gold;
    private final Runnable onExit;   // callback → resume game

    private float time = 0;

    // Shop items as display records
    private record ShopEntry(String id, String name, String desc, int price, Runnable buy) {}
    private final List<ShopEntry> entries = new ArrayList<>();
    private int cursor = 0;
    private String statusMsg = "";
    private float statusTimer = 0;

    private final BitmapFont titleFont, font, smallFont;
    private final GlyphLayout layout = new GlyphLayout();

    public ShopScreen(DyingDarkGame game, Player player, int[] goldRef, Runnable onExit) {
        this.game   = game;
        this.player = player;
        this.gold   = goldRef[0];
        this.onExit = onExit;

        titleFont = new BitmapFont(); titleFont.getData().setScale(3.2f);
        font      = new BitmapFont(); font.getData().setScale(1.8f);
        smallFont = new BitmapFont(); smallFont.getData().setScale(1.3f);

        buildEntries(goldRef);
    }

    private void buildEntries(int[] goldRef) {
        // Weapons
        for (WeaponType w : WeaponType.values()) {
            int price = w.getPrice(1);
            entries.add(new ShopEntry(
                "WPN_" + w.name(),
                "⚔ " + w.displayName + " T1",
                w.description + " | Урон: +" + w.damage,
                price,
                () -> {
                    if (goldRef[0] >= price) {
                        goldRef[0] -= price;
                        gold = goldRef[0];
                        player.equipWeapon(w);
                        status("Куплено: " + w.displayName, true);
                    } else {
                        status("Нет денег!", false);
                    }
                }
            ));
        }

        // Armor tiers 1-4 per slot
        for (ArmorSlot slot : ArmorSlot.values()) {
            for (int tier = 1; tier <= 4; tier++) {
                ArmorItem item = new ArmorItem(slot, tier);
                int price = item.price;
                String tierStr = "T" + tier;
                entries.add(new ShopEntry(
                    item.id,
                    slotIcon(slot) + " " + item.displayName,
                    "Броня +" + item.defenseBonus + " | HP +" + item.hpBonus,
                    price,
                    () -> {
                        if (goldRef[0] >= price) {
                            goldRef[0] -= price;
                            gold = goldRef[0];
                            player.equipArmor(item);
                            status("Куплено: " + item.displayName, true);
                        } else {
                            status("Нет денег!", false);
                        }
                    }
                ));
            }
        }

        // Consumables
        for (ConsumableType c : ConsumableType.values()) {
            int price = c.price;
            entries.add(new ShopEntry(
                "CON_" + c.name(),
                consumableIcon(c) + " " + c.displayName,
                c.description,
                price,
                () -> {
                    if (goldRef[0] >= price) {
                        goldRef[0] -= price;
                        gold = goldRef[0];
                        applyConsumable(c);
                        status("Куплено: " + c.displayName, true);
                    } else {
                        status("Нет денег!", false);
                    }
                }
            ));
        }
    }

    private void applyConsumable(ConsumableType c) {
        switch (c) {
            case HEAL_POTION   -> player.heal(c.healAmount);
            case MANA_POTION   -> player.restoreMana(60);
            case SHIELD_SCROLL -> { /* handled via GameScreen shieldTimer – store on player */ }
        }
    }

    private String slotIcon(ArmorSlot s) {
        return switch (s) { case HELMET -> "[Шл]"; case CHESTPLATE -> "[Нгд]"; case LEGGINGS -> "[Пнж]"; };
    }

    private String consumableIcon(ConsumableType c) {
        return switch (c) { case HEAL_POTION -> "(H)"; case MANA_POTION -> "(M)"; case SHIELD_SCROLL -> "(S)"; };
    }

    private void status(String msg, boolean good) {
        statusMsg   = msg;
        statusTimer = 2f;
    }

    @Override
    public void render(float delta) {
        time += delta;
        if (statusTimer > 0) statusTimer -= delta;

        Gdx.gl.glClearColor(0.03f, 0.03f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput();
        drawBg();
        drawTitle();
        drawGold();
        drawItemList();
        drawPreview();
        drawStatus();
        drawFooter();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)   || Gdx.input.isKeyJustPressed(Input.Keys.W))
            cursor = (cursor + entries.size() - 1) % entries.size();
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            cursor = (cursor + 1) % entries.size();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            entries.get(cursor).buy().run();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            onExit.run();
            dispose();
        }
    }

    private void drawBg() {
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.1f, 0.08f, 0.04f, 1f);
        for (int x = 0; x < W; x += 48) game.shapes.line(x, 0, x, H);
        for (int y = 0; y < H; y += 48) game.shapes.line(0, y, W, y);
        game.shapes.end();
    }

    private void drawTitle() {
        game.batch.begin();
        titleFont.setColor(GOLD_COLOR);
        layout.setText(titleFont, "МАГАЗИН");
        titleFont.draw(game.batch, "МАГАЗИН", W / 2f - layout.width / 2f, H - 18);
        game.batch.end();

        // Decorative line
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(GOLD_COLOR.r, GOLD_COLOR.g, GOLD_COLOR.b, 0.5f);
        game.shapes.rect(60, H - 62, W - 120, 2);
        game.shapes.end();
    }

    private void drawGold() {
        game.batch.begin();
        font.setColor(GOLD_COLOR);
        font.draw(game.batch, "Золото: " + gold, W - 260, H - 22);
        game.batch.end();
    }

    private void drawItemList() {
        int visCount = 14;
        int start = Math.max(0, Math.min(cursor - visCount / 2, entries.size() - visCount));
        float itemH = 36;
        float listY = H - 80;

        // List background
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.05f, 0.05f, 0.08f, 1f);
        game.shapes.rect(40, listY - visCount * itemH, 500, visCount * itemH);
        game.shapes.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.25f, 0.2f, 0.08f, 1f);
        game.shapes.rect(40, listY - visCount * itemH, 500, visCount * itemH);
        game.shapes.end();

        for (int i = start; i < Math.min(start + visCount, entries.size()); i++) {
            ShopEntry e = entries.get(i);
            boolean sel = i == cursor;
            float iy = listY - (i - start + 1) * itemH + itemH * 0.35f;

            if (sel) {
                float glow = 0.5f + 0.5f * (float)Math.sin(time * 4);
                game.shapes.begin(ShapeRenderer.ShapeType.Filled);
                game.shapes.setColor(GOLD_COLOR.r * 0.15f, GOLD_COLOR.g * 0.15f, GOLD_COLOR.b * 0.05f, 1f);
                game.shapes.rect(41, iy - 4, 498, itemH - 2);
                game.shapes.end();
                game.shapes.begin(ShapeRenderer.ShapeType.Line);
                game.shapes.setColor(GOLD_COLOR.r * glow, GOLD_COLOR.g * glow, GOLD_COLOR.b * glow, 1f);
                game.shapes.rect(41, iy - 4, 498, itemH - 2);
                game.shapes.end();
            }

            boolean canAfford = gold >= e.price();
            game.batch.begin();
            font.setColor(sel ? 1f : 0.7f, sel ? 0.95f : 0.7f, sel ? 0.4f : 0.5f, 1f);
            font.draw(game.batch, e.name(), 55, iy + 22);
            // Price tag
            Color pc = canAfford ? GREEN_COLOR : RED_COLOR;
            font.setColor(pc.r, pc.g, pc.b, sel ? 1f : 0.7f);
            String priceStr = e.price() + "g";
            layout.setText(font, priceStr);
            font.draw(game.batch, priceStr, 528 - layout.width, iy + 22);
            game.batch.end();
        }
    }

    private void drawPreview() {
        if (entries.isEmpty()) return;
        ShopEntry e = entries.get(cursor);
        float px = 565, py = H - 80, pw = 355, ph = 360;

        // Preview panel
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.05f, 0.05f, 0.09f, 1f);
        game.shapes.rect(px, py - ph, pw, ph);
        game.shapes.end();
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(GOLD_COLOR.r * 0.5f, GOLD_COLOR.g * 0.5f, GOLD_COLOR.b * 0.2f, 1f);
        game.shapes.rect(px, py - ph, pw, ph);
        game.shapes.end();

        // Draw weapon shape preview if weapon
        if (e.id().startsWith("WPN_")) {
            WeaponType wt = WeaponType.valueOf(e.id().substring(4));
            drawWeaponPreview(wt, px + pw / 2f, py - ph / 2f + 40);
        }

        game.batch.begin();
        font.setColor(1f, 0.9f, 0.5f, 1f);
        layout.setText(font, e.name());
        font.draw(game.batch, e.name(), px + pw/2f - layout.width/2f, py - 12);

        smallFont.setColor(0.8f, 0.8f, 0.8f, 1f);
        // Word wrap description
        String[] words = e.desc().split(" ");
        StringBuilder line = new StringBuilder();
        float lineY = py - 75;
        for (String w : words) {
            String test = line + (line.length() > 0 ? " " : "") + w;
            layout.setText(smallFont, test);
            if (layout.width > pw - 20) {
                layout.setText(smallFont, line.toString());
                smallFont.draw(game.batch, line.toString(), px + pw/2f - layout.width/2f, lineY);
                lineY -= 18;
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            layout.setText(smallFont, line.toString());
            smallFont.draw(game.batch, line.toString(), px + pw/2f - layout.width/2f, lineY);
        }

        // Price big
        boolean canAfford = gold >= e.price();
        font.setColor(canAfford ? GREEN_COLOR : RED_COLOR);
        String priceStr = "Цена: " + e.price() + " золота";
        layout.setText(font, priceStr);
        font.draw(game.batch, priceStr, px + pw/2f - layout.width/2f, py - ph + 40);
        game.batch.end();

        // Equipped indicator
        drawEquippedStatus(e, px, py - ph, pw);
    }

    private void drawEquippedStatus(ShopEntry e, float px, float py, float pw) {
        String eqMsg = null;
        if (e.id().startsWith("WPN_")) {
            WeaponType wt = WeaponType.valueOf(e.id().substring(4));
            if (player.getEquippedWeapon() == wt) eqMsg = "[НАДЕТО]";
        } else if (e.id().startsWith("ARM_")) {
            // Check if this armor is equipped
            for (ArmorSlot s : ArmorSlot.values()) {
                ArmorItem cur = player.getArmor(s);
                if (cur != null && cur.id.equals(e.id())) { eqMsg = "[НАДЕТО]"; break; }
            }
        }
        if (eqMsg != null) {
            game.batch.begin();
            font.setColor(0.3f, 1f, 0.5f, 1f);
            layout.setText(font, eqMsg);
            font.draw(game.batch, eqMsg, px + pw/2f - layout.width/2f, py + 22);
            game.batch.end();
        }
    }

    private void drawWeaponPreview(WeaponType w, float cx, float cy) {
        Color wc = weaponColor(w);
        float glow = 0.6f + 0.4f * (float)Math.sin(time * 3);
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(wc.r * glow, wc.g * glow, wc.b * glow, 1f);

        switch (w.shape) {
            case CROSS -> {
                // Sword: vertical blade + crossguard
                game.shapes.rect(cx - 5,  cy - 55, 10, 80);  // blade
                game.shapes.rect(cx - 28, cy + 15, 56, 9);   // guard
                game.shapes.rect(cx - 4,  cy - 75, 8, 22);   // tip
                // Grip
                game.shapes.setColor(wc.r * 0.5f, wc.g * 0.5f, wc.b * 0.5f, 1f);
                game.shapes.rect(cx - 4, cy - 90, 8, 38);
            }
            case AXE -> {
                // Axe: wide blade at top
                game.shapes.triangle(cx - 40, cy + 30, cx + 40, cy + 30, cx, cy - 30);
                game.shapes.triangle(cx - 40, cy + 50, cx + 40, cy + 50, cx - 40, cy + 30);
                // Handle
                game.shapes.setColor(wc.r * 0.5f, wc.g * 0.5f, wc.b * 0.5f, 1f);
                game.shapes.rect(cx - 5, cy - 85, 10, 60);
            }
            case BOW -> {
                // Bow arc + string
                game.shapes.end();
                game.shapes.begin(ShapeRenderer.ShapeType.Line);
                game.shapes.setColor(wc.r * glow, wc.g * glow, wc.b * glow, 1f);
                // Arc approximated
                float prevX = cx - 15, prevY = cy + 60;
                for (int seg = 1; seg <= 20; seg++) {
                    float t = seg / 20f;
                    float nx2 = cx - 15 + 30 * t;
                    float ny2 = cy + 60 - 120 * (float)Math.sin(Math.PI * t);
                    game.shapes.line(prevX, prevY, nx2, ny2);
                    prevX = nx2; prevY = ny2;
                }
                // String
                game.shapes.setColor(0.9f, 0.9f, 0.7f, 0.8f);
                game.shapes.line(cx - 15, cy + 60, cx - 15, cy - 60);
                // Arrow
                game.shapes.setColor(wc.r, wc.g, wc.b, 1f);
                game.shapes.line(cx - 10, cy, cx + 50, cy);
                game.shapes.line(cx + 50, cy, cx + 38, cy + 8);
                game.shapes.line(cx + 50, cy, cx + 38, cy - 8);
            }
            case STAFF -> {
                // Tall staff + orb
                game.shapes.rect(cx - 5, cy - 80, 10, 110);   // shaft
                game.shapes.circle(cx, cy + 40, 22, 20);       // orb
                game.shapes.end();
                game.shapes.begin(ShapeRenderer.ShapeType.Line);
                game.shapes.setColor(1f, 1f, 1f, glow * 0.6f);
                game.shapes.circle(cx, cy + 40, 28, 20); // glow ring
            }
            case DAGGER -> {
                // Thin narrow blade
                game.shapes.rect(cx - 3, cy - 50, 6, 70);
                game.shapes.triangle(cx - 3, cy + 20, cx + 3, cy + 20, cx, cy + 50);
                game.shapes.rect(cx - 15, cy + 8, 30, 6);    // small guard
                game.shapes.setColor(wc.r * 0.5f, wc.g * 0.5f, wc.b * 0.5f, 1f);
                game.shapes.rect(cx - 4, cy - 75, 8, 28);
            }
        }
        game.shapes.end();

        // Neon outline glow (Line mode)
        if (w.shape != WeaponShape.BOW && w.shape != WeaponShape.STAFF) {
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            game.shapes.setColor(wc.r, wc.g, wc.b, glow * 0.4f);
            game.shapes.rect(cx - 42, cy - 90, 84, 160);
            game.shapes.end();
        }
    }

    private Color weaponColor(WeaponType w) {
        return switch (w) {
            case SWORD  -> new Color(0.4f, 0.8f, 1.0f, 1f);
            case AXE    -> new Color(1.0f, 0.4f, 0.1f, 1f);
            case BOW    -> new Color(0.3f, 1.0f, 0.4f, 1f);
            case STAFF  -> new Color(0.8f, 0.3f, 1.0f, 1f);
            case DAGGER -> new Color(1.0f, 0.9f, 0.2f, 1f);
        };
    }

    private void drawStatus() {
        if (statusTimer <= 0) return;
        game.batch.begin();
        float alpha = Math.min(1f, statusTimer);
        font.setColor(statusMsg.contains("!") && !statusMsg.contains("Куп") ? 1f : 0.3f,
                      statusMsg.contains("Куп") ? 0.9f : 0.2f, 0.2f, alpha);
        layout.setText(font, statusMsg);
        font.draw(game.batch, statusMsg, W / 2f - layout.width / 2f, 90);
        game.batch.end();
    }

    private void drawFooter() {
        game.batch.begin();
        smallFont.setColor(0.4f, 0.4f, 0.5f, 1f);
        layout.setText(smallFont, "↑↓ навигация    ENTER = купить    ESC = выйти из магазина");
        smallFont.draw(game.batch, "↑↓ навигация    ENTER = купить    ESC = выйти из магазина",
                W / 2f - layout.width / 2f, 20);
        game.batch.end();
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { titleFont.dispose(); font.dispose(); smallFont.dispose(); }
}
