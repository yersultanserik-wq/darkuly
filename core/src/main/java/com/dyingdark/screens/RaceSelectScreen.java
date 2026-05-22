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
import com.dyingdark.races.Race;

public class RaceSelectScreen implements Screen {

    private static final float W = 960, H = 640;
    private final DyingDarkGame game;
    private final BitmapFont titleFont, font, smallFont;
    private final GlyphLayout layout = new GlyphLayout();
    private int selected = 0;
    private float time = 0;

    private final Race[] races = Race.values();

    // Neon colors per race
    private static final Color[] RACE_COLORS = {
        new Color(0.2f, 0.6f, 1.0f, 1f),   // HUMAN  blue
        new Color(0.2f, 1.0f, 0.5f, 1f),   // ELF    green
        new Color(1.0f, 0.35f, 0.1f, 1f),  // ORC    orange
        new Color(0.7f, 0.2f, 1.0f, 1f),   // NECRO  purple
        new Color(1.0f, 0.85f, 0.2f, 1f),  // DWARF  gold
    };

    public RaceSelectScreen(DyingDarkGame game) {
        this.game      = game;
        this.titleFont = new BitmapFont();
        this.font      = new BitmapFont();
        this.smallFont = new BitmapFont();
        titleFont.getData().setScale(3.5f);
        font.getData().setScale(2.0f);
        smallFont.getData().setScale(1.3f);
    }

    @Override
    public void render(float delta) {
        time += delta;

        Gdx.gl.glClearColor(0.03f, 0.03f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput();
        drawBackground();
        drawTitle();
        drawRaceCards();
        drawStatBars();
        drawInstructions();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)  || Gdx.input.isKeyJustPressed(Input.Keys.A))
            selected = (selected + races.length - 1) % races.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D))
            selected = (selected + 1) % races.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game, races[selected]));
            dispose();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            dispose();
        }
    }

    private void drawBackground() {
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        Color col = RACE_COLORS[selected];
        game.shapes.setColor(col.r * 0.12f, col.g * 0.12f, col.b * 0.12f, 1f);
        for (int x = 0; x < W; x += 40) game.shapes.line(x, 0, x, H);
        for (int y = 0; y < H; y += 40) game.shapes.line(0, y, W, y);
        game.shapes.end();
    }

    private void drawTitle() {
        game.batch.begin();
        Color c = RACE_COLORS[selected];
        titleFont.setColor(c.r, c.g, c.b, 0.9f);
        layout.setText(titleFont, "ВЫБОР РАСЫ");
        titleFont.draw(game.batch, "ВЫБОР РАСЫ", W / 2f - layout.width / 2f, H - 30);
        game.batch.end();
    }

    private void drawRaceCards() {
        float cardW = 148, cardH = 200;
        float spacing = 20;
        float totalW = races.length * cardW + (races.length - 1) * spacing;
        float startX = W / 2f - totalW / 2f;
        float cardY  = H / 2f - 60;

        for (int i = 0; i < races.length; i++) {
            Race r = races[i];
            Color nc = RACE_COLORS[i];
            float cx = startX + i * (cardW + spacing);
            boolean sel = i == selected;
            float glow = sel ? (0.6f + 0.4f * (float)Math.sin(time * 4)) : 0.15f;

            // Card background
            game.shapes.begin(ShapeRenderer.ShapeType.Filled);
            game.shapes.setColor(nc.r * 0.08f, nc.g * 0.08f, nc.b * 0.08f, 1f);
            game.shapes.rect(cx, cardY, cardW, cardH);
            game.shapes.end();

            // Card border glow
            game.shapes.begin(ShapeRenderer.ShapeType.Line);
            game.shapes.setColor(nc.r * glow, nc.g * glow, nc.b * glow, 1f);
            game.shapes.rect(cx, cardY, cardW, cardH);
            if (sel) { // double border
                game.shapes.rect(cx - 2, cardY - 2, cardW + 4, cardH + 4);
            }
            game.shapes.end();

            // Race figure (stylized shape per race)
            drawRaceFigure(r, nc, cx + cardW / 2f, cardY + cardH - 60, sel);

            // Name
            game.batch.begin();
            font.setColor(nc.r, nc.g, nc.b, sel ? 1f : 0.55f);
            layout.setText(font, r.displayName);
            font.draw(game.batch, r.displayName, cx + cardW / 2f - layout.width / 2f, cardY + 50);

            // Description (wrap manually)
            smallFont.setColor(0.75f, 0.75f, 0.75f, sel ? 0.95f : 0.45f);
            String[] words = r.description.split(" ");
            StringBuilder line = new StringBuilder();
            float lineY = cardY + 32;
            for (String w : words) {
                String test = line + (line.length() > 0 ? " " : "") + w;
                layout.setText(smallFont, test);
                if (layout.width > cardW - 10) {
                    if (line.length() > 0) {
                        layout.setText(smallFont, line.toString());
                        smallFont.draw(game.batch, line.toString(), cx + cardW/2f - layout.width/2f, lineY);
                        lineY -= 16;
                    }
                    line = new StringBuilder(w);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (line.length() > 0) {
                layout.setText(smallFont, line.toString());
                smallFont.draw(game.batch, line.toString(), cx + cardW/2f - layout.width/2f, lineY);
            }
            game.batch.end();
        }
    }

    private void drawRaceFigure(Race r, Color c, float cx, float cy, boolean sel) {
        float alpha = sel ? 1f : 0.5f;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(c.r, c.g, c.b, alpha);

        switch (r) {
            case HUMAN -> {
                // Proportional humanoid
                game.shapes.circle(cx, cy + 30, 14, 16);     // head
                game.shapes.rect(cx - 12, cy - 20, 24, 46);  // body
                game.shapes.rect(cx - 20, cy + 8, 8, 30);    // L arm
                game.shapes.rect(cx + 12, cy + 8, 8, 30);    // R arm
                game.shapes.rect(cx - 14, cy - 40, 10, 22);  // L leg
                game.shapes.rect(cx + 4,  cy - 40, 10, 22);  // R leg
            }
            case ELF -> {
                // Slim with pointed ears
                game.shapes.circle(cx, cy + 30, 11, 16);
                // Pointed ears
                float[] ear1 = { cx - 11, cy + 30, cx - 20, cy + 45, cx - 11, cy + 40 };
                float[] ear2 = { cx + 11, cy + 30, cx + 20, cy + 45, cx + 11, cy + 40 };
                game.shapes.triangle(ear1[0],ear1[1],ear1[2],ear1[3],ear1[4],ear1[5]);
                game.shapes.triangle(ear2[0],ear2[1],ear2[2],ear2[3],ear2[4],ear2[5]);
                game.shapes.rect(cx - 9, cy - 18, 18, 44);
                game.shapes.rect(cx - 16, cy + 6, 6, 28);
                game.shapes.rect(cx + 10, cy + 6, 6, 28);
                game.shapes.rect(cx - 11, cy - 38, 8, 22);
                game.shapes.rect(cx + 3,  cy - 38, 8, 22);
            }
            case ORC -> {
                // Wide, bulky
                game.shapes.circle(cx, cy + 28, 18, 16);
                // Tusks
                game.shapes.rect(cx - 18, cy + 10, 5, 16);
                game.shapes.rect(cx + 13, cy + 10, 5, 16);
                game.shapes.rect(cx - 20, cy - 25, 40, 50);  // fat body
                game.shapes.rect(cx - 30, cy + 5, 12, 32);
                game.shapes.rect(cx + 18, cy + 5, 12, 32);
                game.shapes.rect(cx - 18, cy - 48, 14, 26);
                game.shapes.rect(cx + 4,  cy - 48, 14, 26);
            }
            case NECRO -> {
                // Robed figure with skull motif
                game.shapes.circle(cx, cy + 30, 12, 16);
                // Hood outline (darker)
                game.shapes.rect(cx - 14, cy + 18, 28, 20);
                // Robe (triangle-ish using rects)
                for (int i = 0; i < 6; i++) {
                    float rw = 16 + i * 3;
                    game.shapes.rect(cx - rw/2, cy - 20 + i * 6 - 6, rw, 6);
                }
                // Staff hint
                game.shapes.rect(cx + 16, cy - 30, 4, 70);
                game.shapes.circle(cx + 18, cy + 42, 7, 12);
            }
            case DWARF -> {
                // Short and stocky + big beard
                game.shapes.circle(cx, cy + 22, 16, 16);
                // Beard
                game.shapes.triangle(cx - 14, cy + 6, cx + 14, cy + 6, cx, cy - 16);
                game.shapes.rect(cx - 18, cy - 22, 36, 40); // wide body
                game.shapes.rect(cx - 26, cy + 0, 10, 22);  // arms
                game.shapes.rect(cx + 16, cy + 0, 10, 22);
                game.shapes.rect(cx - 16, cy - 42, 12, 22); // legs (short)
                game.shapes.rect(cx + 4,  cy - 42, 12, 22);
            }
        }
        game.shapes.end();

        // Eye glow
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(1f, 1f, 1f, alpha * 0.9f);
        float ey = (r == Race.ORC) ? cy + 29 : (r == Race.DWARF) ? cy + 23 : (r == Race.ELF) ? cy + 31 : cy + 30;
        game.shapes.circle(cx - 5, ey, 2.5f, 8);
        game.shapes.circle(cx + 5, ey, 2.5f, 8);
        game.shapes.end();
    }

    private void drawStatBars() {
        Race r = races[selected];
        Color nc = RACE_COLORS[selected];
        float barX = 40, barY = 145, barW = 200, barH = 12, gap = 22;

        // Background panel
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.06f, 0.06f, 0.1f, 1f);
        game.shapes.rect(barX - 10, barY - 15, barW + 20, 120);
        game.shapes.end();

        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(nc.r * 0.4f, nc.g * 0.4f, nc.b * 0.4f, 1f);
        game.shapes.rect(barX - 10, barY - 15, barW + 20, 120);
        game.shapes.end();

        drawStatBar("HP",  r.baseHp,    250, barX, barY + gap * 3, barW, barH, new Color(0.9f,0.2f,0.2f,1f));
        drawStatBar("ATK", r.baseAttack, 80, barX, barY + gap * 2, barW, barH, new Color(1f,0.7f,0.1f,1f));
        drawStatBar("DEF", r.baseDefense,25, barX, barY + gap * 1, barW, barH, new Color(0.3f,0.6f,1f,1f));
        drawStatBar("SPD", (int)r.baseSpeed,200, barX, barY, barW, barH, new Color(0.3f,1f,0.5f,1f));
    }

    private void drawStatBar(String label, int val, int max, float x, float y, float w, float h, Color c) {
        float fill = Math.min(1f, (float) val / max) * w;
        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        game.shapes.setColor(0.1f, 0.1f, 0.15f, 1f);
        game.shapes.rect(x + 50, y, w, h);
        game.shapes.setColor(c);
        game.shapes.rect(x + 50, y, fill, h);
        game.shapes.end();

        game.batch.begin();
        smallFont.setColor(0.8f, 0.8f, 0.8f, 1f);
        smallFont.draw(game.batch, label, x, y + h);
        smallFont.setColor(c);
        smallFont.draw(game.batch, String.valueOf(val), x + 50 + fill + 5, y + h);
        game.batch.end();
    }

    private void drawInstructions() {
        game.batch.begin();
        smallFont.setColor(0.4f, 0.4f, 0.5f, 1f);
        layout.setText(smallFont, "← → выбор расы    ENTER = подтвердить    ESC = назад");
        smallFont.draw(game.batch, "← → выбор расы    ENTER = подтвердить    ESC = назад",
                W / 2f - layout.width / 2f, 28);
        game.batch.end();
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { titleFont.dispose(); font.dispose(); smallFont.dispose(); }
}
