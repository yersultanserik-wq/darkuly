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
    private final Runnable onExit;

    private float time = 0;

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
        for (WeaponType w : WeaponType.values()) {
            int price = w.getPrice(1);
            entries.add(new ShopEntry(
                "WPN_" + w.name(),
                w.displayName,
                w.description + " | Dmg: +" + w.damage,
                price,
                () -> {
                    if (goldRef[0] >= price) {
                        goldRef[0] -= price; gold = goldRef[0];
                        player.equipWeapon(w);
                        status("Bought: " + w.displayName, true);
                    } else { status("Not enough gold!", false); }
                }
            ));
        }

        for (ArmorSlot slot : ArmorSlot.values()) {
            for (int tier = 1; tier <= 4; tier++) {
                ArmorItem item = new ArmorItem(slot, tier);
                int price = item.price;
                entries.add(new ShopEntry(
                    item.id,
                    item.displayName,
                    "Armor +" + item.defenseBonus + " | HP +" + item.hpBonus,
                    price,
                    () -> {
                        if (goldRef[0] >= price) {
                            goldRef[0] -= price; gold = goldRef[0];
                            player.equipArmor(item);
                            status("Bought: " + item.displayName, true);
                        } else { status("Not enough gold!", false); }
                    }
                ));
            }
        }

        for (ConsumableType c : ConsumableType.values()) {
            int price = c.price;
            entries.add(new ShopEntry(
                "CON_" + c.name(),
                c.displayName,
                c.description,
                price,
                () -> {
                    if (goldRef[0] >= price) {
                        goldRef[0] -= price; gold = goldRef[0];
                        applyConsumable(c);
                        status("Bought: " + c.displayName, true);
                    } else { status("Not enough gold!", false); }
                }
            ));
        }
    }

    private void applyConsumable(ConsumableType c) {
        switch (c) {
            case HEAL_POTION   -> player.heal(c.healAmount);
            case MANA_POTION   -> player.restoreMana(60);
            case SHIELD_SCROLL -> {}
        }
    }

    private void status(String msg, boolean good) { statusMsg = msg; statusTimer = 2f; }

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
            onExit.run(); dispose();
        }
    }

    private void drawBg() {
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.08f, 0.06f, 0.04f, 1f);
        for (int x = 0; x < W; x += 48) game.shapes.line(x, 0, x, H);
        for (int y = 0; y < H; y += 48) game.shapes.line(0, y, W, y);
        // Corner ornaments
        game.shapes.setColor(GOLD_COLOR.r * 0.4f, GOLD_COLOR.g * 0.4f, GOLD_COLOR.b * 0.1f, 1f);
        float cs = 30;
        game.shapes.line(10, H-10, 10+cs, H-10); game.shapes.line(10, H-10, 10, H-10-cs);
        game.shapes.line(W-10, H-10, W-10-cs, H-10); game.shapes.line(W-10, H-10, W-10, H-10-cs);
        game.shapes.line(10, 10, 10+cs, 10); game.shapes.line(10, 10, 10, 10+cs);
        game.shapes.line(W-10, 10, W-10-cs, 10); game.shapes.line(W-10, 10, W-10, 10+cs);
        game.shapes.end();
    }

    private void drawTitle() {
        float bw = 380, bh = 52, bx = W/2f - bw/2, by = H - 65;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.12f, 0.09f, 0.03f, 1f);
        game.shapes.rect(bx, by, bw, bh);
        game.shapes.setColor(0.2f, 0.14f, 0.04f, 1f);
        game.shapes.rect(bx - 18, by + 8, 20, bh - 16);
        game.shapes.rect(bx + bw - 2, by + 8, 20, bh - 16);
        game.shapes.end();

        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(GOLD_COLOR.r * 0.8f, GOLD_COLOR.g * 0.8f, GOLD_COLOR.b * 0.3f, 1f);
        game.shapes.rect(bx, by, bw, bh);
        game.shapes.rect(bx - 18, by + 8, 20, bh - 16);
        game.shapes.rect(bx + bw - 2, by + 8, 20, bh - 16);
        game.shapes.end();

        drawCoinIcon(bx - 30, by + bh/2, 12);

        game.batch.begin();
        titleFont.setColor(GOLD_COLOR);
        layout.setText(titleFont, "SHOP");
        titleFont.draw(game.batch, "SHOP", W/2f - layout.width/2f, H - 18);
        game.batch.end();

        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(GOLD_COLOR.r, GOLD_COLOR.g, GOLD_COLOR.b, 0.5f);
        game.shapes.rect(60, H - 70, W - 120, 2);
        game.shapes.end();
    }

    private void drawCoinIcon(float cx, float cy, float r) {
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(GOLD_COLOR);
        game.shapes.circle(cx, cy, r, 16);
        game.shapes.setColor(0.9f, 0.7f, 0.1f, 1f);
        game.shapes.circle(cx, cy, r * 0.6f, 12);
        game.shapes.end();
    }

    private void drawGold() {
        drawCoinIcon(W - 280, H - 34, 9);
        game.batch.begin();
        font.setColor(GOLD_COLOR);
        font.draw(game.batch, "  " + gold, W - 270, H - 22);
        game.batch.end();
    }

    private void drawItemList() {
        int visCount = 14;
        int start = Math.max(0, Math.min(cursor - visCount / 2, entries.size() - visCount));
        float itemH = 36;
        float listY = H - 80;

        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.04f, 0.04f, 0.07f, 1f);
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
            float iconX = 50, iconY = iy + 6;

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

            // Draw item icon — always in its own begin/end pair
            drawItemIcon(e.id(), iconX, iconY + 4, sel ? 1f : 0.6f);

            boolean canAfford = gold >= e.price();
            game.batch.begin();
            font.setColor(sel ? 1f : 0.7f, sel ? 0.95f : 0.7f, sel ? 0.4f : 0.5f, 1f);
            font.draw(game.batch, e.name(), iconX + 22, iy + 22);
            Color pc = canAfford ? GREEN_COLOR : RED_COLOR;
            font.setColor(pc.r, pc.g, pc.b, sel ? 1f : 0.7f);
            String priceStr = e.price() + "g";
            layout.setText(font, priceStr);
            font.draw(game.batch, priceStr, 528 - layout.width, iy + 22);
            game.batch.end();
        }
    }

    /** Draw a small pixel-art icon — always balanced begin/end */
    private void drawItemIcon(String id, float x, float y, float alpha) {
        if (id.startsWith("WPN_")) {
            String wname = id.substring(4);
            if (wname.equals("BOW")) {
                // BOW needs Line mode only
                game.shapes.begin(ShapeRenderer.ShapeType.Line);
                game.shapes.setColor(0.3f, 1f, 0.4f, alpha);
                float px2 = x+3, py2 = y+12;
                for (int s = 1; s <= 8; s++) {
                    float t = s / 8f;
                    float nx2 = x+3 + 4*(float)Math.sin(Math.PI*t);
                    float ny2 = y+12 - 12*t;
                    game.shapes.line(px2, py2, nx2, ny2);
                    px2 = nx2; py2 = ny2;
                }
                game.shapes.line(x+3, y+12, x+3, y);
                game.shapes.setColor(1f, 0.9f, 0.5f, alpha);
                game.shapes.line(x+4, y+6, x+14, y+6);
                game.shapes.end();
                // arrowhead in Filled
                game.shapes.begin(ShapeRenderer.ShapeType.Filled);
                game.shapes.setColor(0.8f, 0.5f, 0.1f, alpha);
                game.shapes.triangle(x+13, y+8, x+13, y+4, x+16, y+6);
                game.shapes.end();
            } else {
                game.shapes.begin(ShapeRenderer.ShapeType.Filled);
                switch (wname) {
                    case "SWORD" -> {
                        game.shapes.setColor(0.5f, 0.8f, 1f, alpha);
                        game.shapes.rect(x+7, y+2,  2, 10);
                        game.shapes.rect(x+4, y+9,  8, 2);
                        game.shapes.setColor(0.6f, 0.4f, 0.2f, alpha);
                        game.shapes.rect(x+7, y,   2, 4);
                    }
                    case "AXE" -> {
                        game.shapes.setColor(1f, 0.5f, 0.1f, alpha);
                        game.shapes.triangle(x+4, y+6, x+14, y+10, x+4, y+14);
                        game.shapes.setColor(0.6f, 0.4f, 0.2f, alpha);
                        game.shapes.rect(x+5, y, 3, 14);
                    }
                    case "STAFF" -> {
                        game.shapes.setColor(0.5f, 0.3f, 0.8f, alpha);
                        game.shapes.rect(x+7, y, 2, 14);
                        game.shapes.setColor(0.9f, 0.4f, 1f, alpha);
                        game.shapes.circle(x+8, y+15, 4, 12);
                    }
                    case "DAGGER" -> {
                        game.shapes.setColor(1f, 0.9f, 0.2f, alpha);
                        game.shapes.rect(x+7, y+4, 2, 9);
                        game.shapes.triangle(x+6, y+13, x+10, y+13, x+8, y+16);
                        game.shapes.setColor(0.6f, 0.4f, 0.2f, alpha);
                        game.shapes.rect(x+7, y, 2, 5);
                        game.shapes.rect(x+5, y+8, 6, 2);
                    }
                }
                game.shapes.end();
            }
        } else if (id.startsWith("ARM_")) {
            String lower = id.toLowerCase();
            game.shapes.begin(ShapeRenderer.ShapeType.Filled);
            if (lower.contains("helmet")) {
                game.shapes.setColor(0.5f, 0.6f, 0.8f, alpha);
                game.shapes.rect(x+4, y+2, 8, 6);
                game.shapes.rect(x+3, y+6, 10, 4);
                game.shapes.rect(x+5, y, 6, 3);
                game.shapes.setColor(0.7f, 0.8f, 1f, alpha);
                game.shapes.rect(x+5, y+8, 2, 4);
            } else if (lower.contains("chestplate")) {
                game.shapes.setColor(0.5f, 0.6f, 0.8f, alpha);
                game.shapes.rect(x+3, y+2, 10, 10);
                game.shapes.rect(x+2, y+8, 2, 5);
                game.shapes.rect(x+12, y+8, 2, 5);
                game.shapes.setColor(0.7f, 0.8f, 1f, alpha);
                game.shapes.rect(x+7, y+5, 2, 6);
            } else {
                game.shapes.setColor(0.5f, 0.6f, 0.8f, alpha);
                game.shapes.rect(x+3, y+8, 5, 8);
                game.shapes.rect(x+9, y+8, 5, 8);
                game.shapes.rect(x+3, y+13, 11, 3);
            }
            game.shapes.end();
        } else if (id.startsWith("CON_")) {
            String cname = id.substring(4);
            game.shapes.begin(ShapeRenderer.ShapeType.Filled);
            switch (cname) {
                case "HEAL_POTION" -> {
                    game.shapes.setColor(0.9f, 0.1f, 0.1f, alpha);
                    game.shapes.rect(x+5, y+2, 6, 9);
                    game.shapes.circle(x+8, y+9, 4, 10);
                    game.shapes.setColor(0.6f, 0.6f, 0.6f, alpha);
                    game.shapes.rect(x+6, y+11, 4, 3);
                    game.shapes.setColor(1f, 0.4f, 0.4f, alpha);
                    game.shapes.circle(x+8, y+7, 2, 8);
                }
                case "MANA_POTION" -> {
                    game.shapes.setColor(0.1f, 0.3f, 1f, alpha);
                    game.shapes.rect(x+5, y+2, 6, 9);
                    game.shapes.circle(x+8, y+9, 4, 10);
                    game.shapes.setColor(0.6f, 0.6f, 0.6f, alpha);
                    game.shapes.rect(x+6, y+11, 4, 3);
                    game.shapes.setColor(0.4f, 0.6f, 1f, alpha);
                    game.shapes.circle(x+8, y+7, 2, 8);
                }
                case "SHIELD_SCROLL" -> {
                    game.shapes.setColor(0.9f, 0.85f, 0.6f, alpha);
                    game.shapes.rect(x+4, y+2, 8, 12);
                    game.shapes.setColor(0.7f, 0.6f, 0.3f, alpha);
                    game.shapes.circle(x+8, y+2, 4, 8);
                    game.shapes.circle(x+8, y+14, 4, 8);
                    game.shapes.setColor(0.3f, 0.6f, 1f, alpha);
                    game.shapes.rect(x+6, y+6, 4, 1);
                    game.shapes.rect(x+6, y+8, 4, 1);
                    game.shapes.rect(x+6, y+10, 4, 1);
                }
            }
            game.shapes.end();
        }
    }

    private void drawPreview() {
        if (entries.isEmpty()) return;
        ShopEntry e = entries.get(cursor);
        float px = 565, py = H - 80, pw = 355, ph = 360;

        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.05f, 0.05f, 0.09f, 1f);
        game.shapes.rect(px, py - ph, pw, ph);
        game.shapes.end();

        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(GOLD_COLOR.r * 0.5f, GOLD_COLOR.g * 0.5f, GOLD_COLOR.b * 0.2f, 1f);
        game.shapes.rect(px, py - ph, pw, ph);
        game.shapes.setColor(GOLD_COLOR.r * 0.2f, GOLD_COLOR.g * 0.2f, GOLD_COLOR.b * 0.1f, 1f);
        game.shapes.rect(px + 4, py - ph + 4, pw - 8, ph - 8);
        game.shapes.end();

        drawLargeItemIcon(e.id(), px + pw/2f - 30, py - 175, 1f);

        if (e.id().startsWith("WPN_")) {
            WeaponType wt = WeaponType.valueOf(e.id().substring(4));
            drawWeaponPreview(wt, px + pw / 2f, py - ph / 2f + 40);
        }

        game.batch.begin();
        font.setColor(1f, 0.9f, 0.5f, 1f);
        layout.setText(font, e.name());
        font.draw(game.batch, e.name(), px + pw/2f - layout.width/2f, py - 12);

        smallFont.setColor(0.8f, 0.8f, 0.8f, 1f);
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
            } else line = new StringBuilder(test);
        }
        if (line.length() > 0) {
            layout.setText(smallFont, line.toString());
            smallFont.draw(game.batch, line.toString(), px + pw/2f - layout.width/2f, lineY);
        }

        boolean canAfford = gold >= e.price();
        font.setColor(canAfford ? GREEN_COLOR : RED_COLOR);
        String priceStr = "Price: " + e.price() + " gold";
        layout.setText(font, priceStr);
        font.draw(game.batch, priceStr, px + pw/2f - layout.width/2f, py - ph + 40);
        game.batch.end();

        drawEquippedStatus(e, px, py - ph, pw);
    }

    private void drawLargeItemIcon(String id, float x, float y, float alpha) {
        float s = 3f;
        if (id.startsWith("WPN_")) {
            String wname = id.substring(4);
            if (wname.equals("BOW")) {
                game.shapes.begin(ShapeRenderer.ShapeType.Line);
                game.shapes.setColor(0.3f, 1f, 0.4f, alpha);
                float px2 = x+3*s, py2 = y+12*s;
                for (int seg = 1; seg <= 16; seg++) {
                    float t = seg/16f;
                    float nx2 = x+3*s + 4*s*(float)Math.sin(Math.PI*t);
                    float ny2 = y+12*s - 12*s*t;
                    game.shapes.line(px2, py2, nx2, ny2); px2=nx2; py2=ny2;
                }
                game.shapes.line(x+3*s, y+12*s, x+3*s, y);
                game.shapes.setColor(1f, 0.9f, 0.5f, alpha);
                game.shapes.line(x+4*s, y+6*s, x+14*s, y+6*s);
                game.shapes.end();
                game.shapes.begin(ShapeRenderer.ShapeType.Filled);
                game.shapes.setColor(0.8f, 0.5f, 0.1f, alpha);
                game.shapes.triangle(x+13*s, y+8*s, x+13*s, y+4*s, x+16*s, y+6*s);
                game.shapes.end();
            } else {
                game.shapes.begin(ShapeRenderer.ShapeType.Filled);
                switch (wname) {
                    case "SWORD" -> {
                        game.shapes.setColor(0.5f, 0.8f, 1f, alpha);
                        game.shapes.rect(x+7*s, y+2*s, 2*s, 10*s);
                        game.shapes.rect(x+4*s, y+9*s, 8*s, 2*s);
                        game.shapes.setColor(0.8f, 0.6f, 0.3f, alpha);
                        game.shapes.rect(x+7*s, y, 2*s, 4*s);
                    }
                    case "AXE" -> {
                        game.shapes.setColor(1f, 0.5f, 0.1f, alpha);
                        game.shapes.triangle(x+4*s, y+6*s, x+14*s, y+10*s, x+4*s, y+14*s);
                        game.shapes.setColor(0.6f, 0.4f, 0.2f, alpha);
                        game.shapes.rect(x+5*s, y, 3*s, 14*s);
                    }
                    case "STAFF" -> {
                        game.shapes.setColor(0.5f, 0.3f, 0.8f, alpha);
                        game.shapes.rect(x+7*s, y, 2*s, 14*s);
                        game.shapes.setColor(0.9f, 0.4f, 1f, alpha);
                        game.shapes.circle(x+8*s, y+15*s, 4*s, 16);
                    }
                    case "DAGGER" -> {
                        game.shapes.setColor(1f, 0.9f, 0.2f, alpha);
                        game.shapes.rect(x+7*s, y+4*s, 2*s, 9*s);
                        game.shapes.triangle(x+6*s, y+13*s, x+10*s, y+13*s, x+8*s, y+16*s);
                        game.shapes.setColor(0.6f, 0.4f, 0.2f, alpha);
                        game.shapes.rect(x+7*s, y, 2*s, 5*s);
                        game.shapes.rect(x+5*s, y+8*s, 6*s, 2*s);
                    }
                }
                game.shapes.end();
            }
        } else if (id.startsWith("ARM_")) {
            String lower = id.toLowerCase();
            game.shapes.begin(ShapeRenderer.ShapeType.Filled);
            if (lower.contains("helmet")) {
                game.shapes.setColor(0.5f, 0.6f, 0.8f, alpha);
                game.shapes.rect(x+4*s, y+2*s, 8*s, 6*s);
                game.shapes.rect(x+3*s, y+6*s, 10*s, 4*s);
                game.shapes.rect(x+5*s, y, 6*s, 3*s);
            } else if (lower.contains("chestplate")) {
                game.shapes.setColor(0.5f, 0.6f, 0.8f, alpha);
                game.shapes.rect(x+3*s, y+2*s, 10*s, 10*s);
                game.shapes.rect(x+2*s, y+8*s, 2*s, 5*s);
                game.shapes.rect(x+12*s, y+8*s, 2*s, 5*s);
            } else {
                game.shapes.setColor(0.5f, 0.6f, 0.8f, alpha);
                game.shapes.rect(x+3*s, y+8*s, 5*s, 8*s);
                game.shapes.rect(x+9*s, y+8*s, 5*s, 8*s);
                game.shapes.rect(x+3*s, y+13*s, 11*s, 3*s);
            }
            game.shapes.end();
        } else if (id.startsWith("CON_")) {
            String cname = id.substring(4);
            game.shapes.begin(ShapeRenderer.ShapeType.Filled);
            switch (cname) {
                case "HEAL_POTION" -> {
                    game.shapes.setColor(0.9f, 0.1f, 0.1f, alpha);
                    game.shapes.rect(x+5*s, y+2*s, 6*s, 9*s);
                    game.shapes.circle(x+8*s, y+9*s, 4*s, 14);
                    game.shapes.setColor(0.6f, 0.6f, 0.6f, alpha);
                    game.shapes.rect(x+6*s, y+11*s, 4*s, 3*s);
                }
                case "MANA_POTION" -> {
                    game.shapes.setColor(0.1f, 0.3f, 1f, alpha);
                    game.shapes.rect(x+5*s, y+2*s, 6*s, 9*s);
                    game.shapes.circle(x+8*s, y+9*s, 4*s, 14);
                    game.shapes.setColor(0.6f, 0.6f, 0.6f, alpha);
                    game.shapes.rect(x+6*s, y+11*s, 4*s, 3*s);
                }
                case "SHIELD_SCROLL" -> {
                    game.shapes.setColor(0.9f, 0.85f, 0.6f, alpha);
                    game.shapes.rect(x+4*s, y+2*s, 8*s, 12*s);
                    game.shapes.setColor(0.7f, 0.6f, 0.3f, alpha);
                    game.shapes.circle(x+8*s, y+2*s, 4*s, 10);
                    game.shapes.circle(x+8*s, y+14*s, 4*s, 10);
                }
            }
            game.shapes.end();
        }
    }

    private void drawEquippedStatus(ShopEntry e, float px, float py, float pw) {
        String eqMsg = null;
        if (e.id().startsWith("WPN_")) {
            WeaponType wt = WeaponType.valueOf(e.id().substring(4));
            if (player.getEquippedWeapon() == wt) eqMsg = "[EQUIPPED]";
        } else if (e.id().startsWith("ARM_")) {
            for (ArmorSlot s : ArmorSlot.values()) {
                ArmorItem cur = player.getArmor(s);
                if (cur != null && cur.id.equals(e.id())) { eqMsg = "[EQUIPPED]"; break; }
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

        if (w.shape == WeaponShape.BOW) {
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            game.shapes.setColor(wc.r * glow, wc.g * glow, wc.b * glow, 1f);
            float prevX = cx - 15, prevY = cy + 60;
            for (int seg = 1; seg <= 20; seg++) {
                float t = seg / 20f;
                float nx2 = cx - 15 + 30 * t;
                float ny2 = cy + 60 - 120 * (float)Math.sin(Math.PI * t);
                game.shapes.line(prevX, prevY, nx2, ny2); prevX=nx2; prevY=ny2;
            }
            game.shapes.setColor(0.9f, 0.9f, 0.7f, 0.8f);
            game.shapes.line(cx - 15, cy + 60, cx - 15, cy - 60);
            game.shapes.setColor(wc.r, wc.g, wc.b, 1f);
            game.shapes.line(cx - 10, cy, cx + 50, cy);
            game.shapes.line(cx + 50, cy, cx + 38, cy + 8);
            game.shapes.line(cx + 50, cy, cx + 38, cy - 8);
            game.shapes.end();
            return;
        }

        if (w.shape == WeaponShape.STAFF) {
            game.shapes.begin(ShapeRenderer.ShapeType.Filled);
            game.shapes.setColor(wc.r * glow, wc.g * glow, wc.b * glow, 1f);
            game.shapes.rect(cx - 5, cy - 80, 10, 110);
            game.shapes.circle(cx, cy + 40, 22, 20);
            game.shapes.end();
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            game.shapes.setColor(1f, 1f, 1f, glow * 0.6f);
            game.shapes.circle(cx, cy + 40, 28, 20);
            game.shapes.end();
            return;
        }

        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(wc.r * glow, wc.g * glow, wc.b * glow, 1f);
        switch (w.shape) {
            case CROSS -> {
                game.shapes.rect(cx - 5, cy - 55, 10, 80);
                game.shapes.rect(cx - 28, cy + 15, 56, 9);
                game.shapes.rect(cx - 4, cy - 75, 8, 22);
                game.shapes.setColor(wc.r * 0.5f, wc.g * 0.5f, wc.b * 0.5f, 1f);
                game.shapes.rect(cx - 4, cy - 90, 8, 38);
            }
            case AXE -> {
                game.shapes.triangle(cx - 40, cy + 30, cx + 40, cy + 30, cx, cy - 30);
                game.shapes.triangle(cx - 40, cy + 50, cx + 40, cy + 50, cx - 40, cy + 30);
                game.shapes.setColor(wc.r * 0.5f, wc.g * 0.5f, wc.b * 0.5f, 1f);
                game.shapes.rect(cx - 5, cy - 85, 10, 60);
            }
            case DAGGER -> {
                game.shapes.rect(cx - 3, cy - 50, 6, 70);
                game.shapes.triangle(cx - 3, cy + 20, cx + 3, cy + 20, cx, cy + 50);
                game.shapes.rect(cx - 15, cy + 8, 30, 6);
                game.shapes.setColor(wc.r * 0.5f, wc.g * 0.5f, wc.b * 0.5f, 1f);
                game.shapes.rect(cx - 4, cy - 75, 8, 28);
            }
            default -> {
                game.shapes.rect(cx - 5, cy - 55, 10, 80);
            }
        }
        game.shapes.end();

        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(wc.r, wc.g, wc.b, glow * 0.4f);
        game.shapes.rect(cx - 42, cy - 90, 84, 160);
        game.shapes.end();
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
        font.setColor(statusMsg.contains("!") && !statusMsg.contains("Bought") ? 1f : 0.3f,
                      statusMsg.contains("Bought") ? 0.9f : 0.2f, 0.2f, alpha);
        layout.setText(font, statusMsg);
        font.draw(game.batch, statusMsg, W / 2f - layout.width / 2f, 90);
        game.batch.end();
    }

    private void drawFooter() {
        game.batch.begin();
        smallFont.setColor(0.4f, 0.4f, 0.5f, 1f);
        layout.setText(smallFont, "UP/DOWN navigate    ENTER = buy    ESC = close shop");
        smallFont.draw(game.batch, "UP/DOWN navigate    ENTER = buy    ESC = close shop",
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
