package com.dyingdark;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.dyingdark.screens.MenuScreen;

public class DyingDarkGame extends Game {

    public SpriteBatch batch;
    public ShapeRenderer shapes;

    @Override
    public void create() {
        batch  = new SpriteBatch();
        shapes = new ShapeRenderer();
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
    }
}
