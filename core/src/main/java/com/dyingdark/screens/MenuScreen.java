package com.dyingdark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.dyingdark.DyingDarkGame;

public class MenuScreen implements Screen {

    private static final float VW = 640, VH = 426;

    private final DyingDarkGame game;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GlyphLayout layout;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private float time = 0;

    public MenuScreen(DyingDarkGame game) {
        this.game      = game;
        this.font      = new BitmapFont();
        this.smallFont = new BitmapFont();
        this.layout    = new GlyphLayout();
        font.getData().setScale(4f);
        smallFont.getData().setScale(1.8f);

        camera = new OrthographicCamera();
        viewport = new FitViewport(VW, VH, camera);
        camera.setToOrtho(false, VW, VH);
        camera.update();
    }

    @Override
    public void render(float delta) {
        time += delta;

        Gdx.gl.glClearColor(0.04f, 0.04f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Always reset projection matrix — batch may have stale camera from GameScreen
        game.batch.setProjectionMatrix(camera.combined);
        game.shapes.setProjectionMatrix(camera.combined);

        float W = VW, H = VH;

        // Draw decorative background grid
        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(0.1f, 0.1f, 0.2f, 1f);
        for (int x = 0; x < W; x += 32) game.shapes.line(x, 0, x, H);
        for (int y = 0; y < H; y += 32) game.shapes.line(0, y, W, y);
        game.shapes.end();

        // Pulsing "Play" button
        float pulse = 0.7f + 0.3f * (float)Math.sin(time * 3);
        float btnW = 220, btnH = 55;
        float btnX = W / 2f - btnW / 2f;
        float btnY = H / 2f - 80;

        game.shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Title glow
        game.shapes.setColor(0.5f, 0.05f, 0.05f, 0.3f);
        game.shapes.rect(W/2f - 260, H/2f + 40, 520, 90);
        // Button
        game.shapes.setColor(0.6f * pulse, 0.05f, 0.05f, 1f);
        game.shapes.rect(btnX, btnY, btnW, btnH);
        game.shapes.end();

        game.shapes.begin(ShapeRenderer.ShapeType.Line);
        game.shapes.setColor(Color.RED);
        game.shapes.rect(btnX, btnY, btnW, btnH);
        game.shapes.end();

        // Text
        game.batch.begin();
        font.setColor(0.9f, 0.1f, 0.1f, 1f);
        layout.setText(font, "DYING DARK");
        font.draw(game.batch, "DYING DARK", W/2f - layout.width/2f, H/2f + 120);

        smallFont.setColor(1f, 1f, 1f, pulse);
        layout.setText(smallFont, "PRESS  ENTER  TO  PLAY");
        smallFont.draw(game.batch, "PRESS  ENTER  TO  PLAY", W/2f - layout.width/2f, btnY + 35);

        smallFont.setColor(0.5f, 0.5f, 0.5f, 1f);
        layout.setText(smallFont, "WASD = Move   SPACE = Attack   Q = Scroll");
        smallFont.draw(game.batch, "WASD = Move   SPACE = Attack   Q = Scroll",
                W/2f - layout.width/2f, 60);
        game.batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Screen next = new RaceSelectScreen(game);
            dispose();
            game.setScreen(next);
        }
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { font.dispose(); smallFont.dispose(); }
}
